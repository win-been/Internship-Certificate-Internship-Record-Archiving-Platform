package com.internship.service;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

@Service
public class BlockchainService {
    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern TX_HASH = Pattern.compile("0x[0-9a-fA-F]{64}");
    private static final Pattern CONTRACT_ADDRESS = Pattern.compile("0x[0-9a-fA-F]{40}");

    @Value("${webase.url}") private String baseUrl;
    @Value("${webase.api-path:/WeBASE-Front}") private String apiPath;
    @Value("${webase.certificate-contract-address}") private String certContractAddr;
    @Value("${webase.record-contract-address}") private String recordContractAddr;
    @Value("${webase.group-id:group0}") private String groupId;
    @Value("${webase.school-account:}") private String schoolAccount;
    @Value("${webase.enterprise-account:}") private String enterpriseAccount;
    @Value("${webase.school-private-key:}") private String schoolPrivateKey;
    @Value("${webase.enterprise-private-key:}") private String enterprisePrivateKey;

    private final RestTemplate restTemplate = buildRestTemplate();
    private volatile boolean chainAvailable = false;
    private volatile String lastError = null;

    private static RestTemplate buildRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    // WeBASE-Front expects groupId as an integer
    private Object groupIdValue() {
        try {
            return Integer.valueOf(groupId.trim());
        } catch (Exception e) {
            return groupId;
        }
    }

    // Cert contract ABI (minimal - just the functions we use)
    private static final String CERT_ABI = "["
        + "{\"constant\":true,\"inputs\":[],\"name\":\"getCertificateCount\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"internshipId\",\"type\":\"uint256\"},{\"name\":\"studentId\",\"type\":\"uint256\"},{\"name\":\"contentHash\",\"type\":\"bytes32\"},{\"name\":\"schoolAddress\",\"type\":\"address\"},{\"name\":\"enterpriseAddress\",\"type\":\"address\"}],\"name\":\"createCertificate\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[{\"name\":\"certificateId\",\"type\":\"uint256\"},{\"name\":\"contentHash\",\"type\":\"bytes32\"}],\"name\":\"verifyCertificate\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"view\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"certificateId\",\"type\":\"uint256\"}],\"name\":\"approveBySchool\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"certificateId\",\"type\":\"uint256\"}],\"name\":\"approveByEnterprise\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    private static final String RECORD_ABI = "["
        + "{\"constant\":false,\"inputs\":[{\"name\":\"internshipId\",\"type\":\"uint256\"},{\"name\":\"studentId\",\"type\":\"uint256\"},{\"name\":\"recordDateHash\",\"type\":\"bytes32\"},{\"name\":\"contentHash\",\"type\":\"bytes32\"},{\"name\":\"studentAddress\",\"type\":\"address\"},{\"name\":\"mentorAddress\",\"type\":\"address\"}],\"name\":\"createRecord\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[{\"name\":\"internshipId\",\"type\":\"uint256\"},{\"name\":\"recordIndex\",\"type\":\"uint256\"},{\"name\":\"contentHash\",\"type\":\"bytes32\"}],\"name\":\"verifyRecord\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"view\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"internshipId\",\"type\":\"uint256\"},{\"name\":\"recordIndex\",\"type\":\"uint256\"}],\"name\":\"approveRecord\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"internshipId\",\"type\":\"uint256\"},{\"name\":\"recordIndex\",\"type\":\"uint256\"}],\"name\":\"rejectRecord\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    // ===== Certificate Contract =====

    public String createCertificate(long internshipId, long studentId, String contentHash,
                                     String schoolAddr, String enterpriseAddr) throws Exception {
        List<Object> params = Arrays.asList(
            String.valueOf(internshipId), String.valueOf(studentId),
            ensureBytes32(contentHash), ensureAddress(schoolAddr), ensureAddress(enterpriseAddr)
        );
        return sendTransaction(certContractAddr, "createCertificate", params);
    }

    public String approveCertificateBySchool(long certId) throws Exception {
        return sendTransaction(certContractAddr, "approveBySchool", Arrays.asList(String.valueOf(certId)));
    }

    public String approveCertificateByEnterprise(long certId) throws Exception {
        return sendTransaction(certContractAddr, "approveByEnterprise", Arrays.asList(String.valueOf(certId)));
    }

    public boolean verifyCertificate(long certId, String contentHash) throws Exception {
        String result = callContract(certContractAddr, "verifyCertificate",
            Arrays.asList(String.valueOf(certId), ensureBytes32(contentHash)));
        return "true".equalsIgnoreCase(result) || "1".equals(result);
    }

    // ===== Record Contract =====

    public String createRecord(long internshipId, long studentId, String recordDateHash,
                                String contentHash, String studentAddr, String mentorAddr) throws Exception {
        List<Object> params = Arrays.asList(
            String.valueOf(internshipId), String.valueOf(studentId),
            ensureBytes32(recordDateHash), ensureBytes32(contentHash),
            ensureAddress(studentAddr), ensureAddress(mentorAddr)
        );
        return sendTransaction(recordContractAddr, "createRecord", params);
    }

    public String approveRecord(long internshipId, long recordIndex) throws Exception {
        return sendTransaction(recordContractAddr, "approveRecord",
            Arrays.asList(String.valueOf(internshipId), String.valueOf(recordIndex)));
    }

    public String rejectRecord(long internshipId, long recordIndex) throws Exception {
        return sendTransaction(recordContractAddr, "rejectRecord",
            Arrays.asList(String.valueOf(internshipId), String.valueOf(recordIndex)));
    }

    public boolean verifyRecord(long internshipId, long recordIndex, String contentHash) throws Exception {
        String result = callContract(recordContractAddr, "verifyRecord",
            Arrays.asList(String.valueOf(internshipId), String.valueOf(recordIndex), ensureBytes32(contentHash)));
        return "true".equalsIgnoreCase(result) || "1".equals(result);
    }

    // ===== Core API Calls =====

    private String callContract(String contractAddr, String funcName, List<Object> params) throws Exception {
        String user = schoolAccount != null && !schoolAccount.isEmpty() ? schoolAccount : enterpriseAccount;
        return executeRequest(contractAddr, funcName, params, user, false);
    }

    private String sendTransaction(String contractAddr, String funcName, List<Object> params) throws Exception {
        String user = contractAddr.equals(certContractAddr) ? schoolAccount : enterpriseAccount;
        return executeRequest(contractAddr, funcName, params, user, true);
    }

    private String executeRequest(String contractAddr, String funcName,
                                   List<Object> params, String user, boolean requireTxHash) {
        String url = baseUrl + apiPath + "/trans/handle";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("groupId", groupIdValue());
        body.put("user", user != null && !user.isEmpty() ? user : schoolAccount);
        body.put("contractName", contractAddr.equals(certContractAddr) ? "InternshipCertificate" : "InternshipRecord");
        body.put("contractAddress", contractAddr);
        body.put("funcName", funcName);
        body.put("funcParam", params);
        body.put("useCns", false);
        // Parse ABI string to List for proper JSON array serialization
        try {
            String abiStr = contractAddr.equals(certContractAddr) ? CERT_ABI : RECORD_ABI;
            body.put("contractAbi", JSON.readValue(abiStr, java.util.List.class));
        } catch (Exception abiEx) {
            log.warn("Failed to parse ABI: {}", abiEx.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("WeBASE: {} -> {}", funcName, url);
            ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);
            if (response.getBody() == null) throw new RuntimeException("empty response");

            Object result = response.getBody();
            assertSuccessfulResponse(result);

            Object data = responsePayload(result);
            if (requireTxHash) {
                String txHash = extractTxHash(data);
                if (txHash == null) {
                    throw new RuntimeException("WeBASE did not return transactionHash: " + summarize(result));
                }
                chainAvailable = true;
                lastError = null;
                log.info("TxHash: {}", txHash);
                return txHash;
            }

            chainAvailable = true;
            lastError = null;
            Object output = extractCallOutput(data);
            return output != null ? String.valueOf(output) : String.valueOf(data);
        } catch (RuntimeException re) {
            lastError = re.getMessage();
            chainAvailable = false;
            throw re;
        } catch (Exception e) {
            lastError = e.getMessage();
            chainAvailable = false;
            log.error("WeBASE request failed: {}", e.getMessage());
            throw new RuntimeException("Blockchain unavailable: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> deployContracts() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("webaseUrl", baseUrl + apiPath);
        result.put("groupId", groupId);
        try {
            Map<String, Object> certificate = deployContract("InternshipCertificate", "InternshipCertificate.sol", schoolAccount);
            certContractAddr = String.valueOf(certificate.get("contractAddress"));
            Map<String, Object> record = deployContract("InternshipRecord", "InternshipRecord.sol", enterpriseAccount);
            recordContractAddr = String.valueOf(record.get("contractAddress"));
            result.put("available", true);
            result.put("certificate", certificate);
            result.put("record", record);
            result.put("certContract", certContractAddr);
            result.put("recordContract", recordContractAddr);
            chainAvailable = true;
            lastError = null;
        } catch (Exception e) {
            chainAvailable = false;
            lastError = e.getMessage();
            result.put("available", false);
            result.put("error", e.getMessage());
            log.warn("Contract deployment failed: {}", e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deployContract(String contractName, String sourceFile, String user) throws Exception {
        String source = readContractSource(sourceFile);
        String compileUrl = baseUrl + apiPath + "/contract/contractCompile";
        Map<String, Object> compileBody = new LinkedHashMap<>();
        compileBody.put("contractName", contractName);
        compileBody.put("solidityBase64", Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8)));

        Object compileResponse = postJson(compileUrl, compileBody);
        assertSuccessfulResponse(compileResponse);
        Object compilePayload = responsePayload(compileResponse);
        if (!(compilePayload instanceof Map<?, ?> compileMapRaw)) {
            throw new RuntimeException("WeBASE compile returned unexpected response: " + summarize(compileResponse));
        }
        Map<String, Object> compileMap = (Map<String, Object>) compileMapRaw;
        String bytecodeBin = firstText(compileMap, "bytecodeBin", "bin");
        Object abiInfo = compileMap.get("abiInfo");
        if (abiInfo == null) {
            String contractAbi = firstText(compileMap, "contractAbi", "abi");
            if (contractAbi != null) abiInfo = JSON.readValue(contractAbi, java.util.List.class);
        }
        if (bytecodeBin == null || bytecodeBin.isBlank() || abiInfo == null) {
            throw new RuntimeException("WeBASE compile response missed ABI/BIN: " + summarize(compileResponse));
        }

        String deployUrl = baseUrl + apiPath + "/contract/deploy";
        Map<String, Object> deployBody = new LinkedHashMap<>();
        deployBody.put("groupId", groupIdValue());
        deployBody.put("user", user != null && !user.isEmpty() ? user : schoolAccount);
        deployBody.put("contractName", contractName);
        deployBody.put("abiInfo", abiInfo);
        deployBody.put("bytecodeBin", bytecodeBin);
        deployBody.put("funcParam", Collections.emptyList());

        Object deployResponse = postJson(deployUrl, deployBody);
        assertSuccessfulResponse(deployResponse);
        Object deployPayload = responsePayload(deployResponse);
        String contractAddress = extractContractAddress(deployPayload);
        if (contractAddress == null) {
            throw new RuntimeException("WeBASE deploy did not return contractAddress: " + summarize(deployResponse));
        }

        Map<String, Object> deployed = new LinkedHashMap<>();
        deployed.put("contractName", contractName);
        deployed.put("contractAddress", contractAddress);
        deployed.put("txHash", extractTxHash(deployPayload));
        deployed.put("response", deployPayload);
        return deployed;
    }

    private Object postJson(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Object.class).getBody();
    }

    private String readContractSource(String fileName) throws Exception {
        List<Path> candidates = List.of(
                Path.of(fileName),
                Path.of("..", fileName),
                Path.of("backend", fileName),
                Path.of("..", "..", fileName)
        );
        for (Path path : candidates) {
            if (Files.exists(path)) return Files.readString(path, StandardCharsets.UTF_8);
        }
        throw new RuntimeException("Contract source not found: " + fileName);
    }

    private void assertSuccessfulResponse(Object response) {
        if (!(response instanceof Map<?, ?> map)) return;
        Object codeObj = map.get("code");
        if (codeObj == null) return;
        boolean ok = (codeObj instanceof Number number && number.intValue() == 0)
                || "0".equals(String.valueOf(codeObj))
                || "0x0".equalsIgnoreCase(String.valueOf(codeObj));
        if (!ok) {
            String errorMsg = String.valueOf(firstNonNull(map.get("errorMessage"), map.get("message"), map.get("msg"), "unknown"));
            throw new RuntimeException("WeBASE code=" + codeObj + ": " + errorMsg);
        }
    }

    private Object responsePayload(Object response) {
        if (response instanceof Map<?, ?> map && map.containsKey("data")) {
            return map.get("data");
        }
        return response;
    }

    private Object extractCallOutput(Object data) {
        if (data instanceof Map<?, ?> map) {
            if (map.containsKey("output")) return map.get("output");
            if (map.containsKey("result")) return map.get("result");
            if (map.containsKey("data")) return extractCallOutput(map.get("data"));
        }
        if (data instanceof List<?> list && !list.isEmpty()) return list.get(0);
        return data;
    }

    private String extractTxHash(Object value) {
        Object direct = findByKey(value, Set.of("transactionHash", "transaction_hash", "txHash", "tx_hash", "transHash"));
        String directText = direct != null ? String.valueOf(direct) : null;
        if (isTransactionHash(directText)) return directText;
        Matcher matcher = TX_HASH.matcher(String.valueOf(value));
        return matcher.find() ? matcher.group() : null;
    }

    private String extractContractAddress(Object value) {
        Object direct = findByKey(value, Set.of("contractAddress", "contract_address", "address"));
        String directText = direct != null ? String.valueOf(direct) : null;
        if (isContractAddress(directText)) return directText;
        Matcher matcher = CONTRACT_ADDRESS.matcher(String.valueOf(value));
        return matcher.find() ? matcher.group() : null;
    }

    private Object findByKey(Object value, Set<String> keys) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && keys.contains(String.valueOf(entry.getKey()))) return entry.getValue();
            }
            for (Object nested : map.values()) {
                Object found = findByKey(nested, keys);
                if (found != null) return found;
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                Object found = findByKey(item, keys);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return String.valueOf(value);
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String summarize(Object value) {
        String text;
        try {
            text = JSON.writeValueAsString(value);
        } catch (Exception e) {
            text = String.valueOf(value);
        }
        return text.length() > 700 ? text.substring(0, 700) + "..." : text;
    }

    // ===== Utility =====

    public static String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("0x");
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    public static boolean isTransactionHash(String value) {
        return value != null && TX_HASH.matcher(value).matches();
    }

    public static boolean isContractAddress(String value) {
        return value != null && CONTRACT_ADDRESS.matcher(value).matches();
    }

    private String ensureBytes32(String input) {
        if (input == null || input.isEmpty()) return "0x" + "0".repeat(64);
        String hex = input.startsWith("0x") ? input.substring(2) : input;
        if (!hex.matches("[0-9a-fA-F]+")) return calculateHash(input);
        if (hex.length() < 64) hex = String.format("%64s", hex).replace(' ', '0');
        if (hex.length() > 64) hex = hex.substring(0, 64);
        return "0x" + hex;
    }

    private String ensureAddress(String input) {
        if (input == null || input.isEmpty()) return "0x" + "0".repeat(40);
        String hex = input.startsWith("0x") ? input.substring(2) : input;
        if (hex.length() < 40) hex = String.format("%40s", hex).replace(' ', '0');
        if (hex.length() > 40) hex = hex.substring(0, 40);
        return "0x" + hex.toLowerCase();
    }

    // ===== Getters & Health =====
    public String getCertificateContractAddress() { return certContractAddr; }
    public String getRecordContractAddress() { return recordContractAddr; }
    public String getSchoolAccount() { return schoolAccount; }
    public String getEnterpriseAccount() { return enterpriseAccount; }
    public boolean isChainAvailable() { return chainAvailable; }
    public String getLastError() { return lastError; }

    public Map<String, Object> checkHealth() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("webaseUrl", baseUrl + apiPath);
        status.put("certContract", certContractAddr);
        status.put("recordContract", recordContractAddr);
        status.put("groupId", groupId);
        status.put("available", chainAvailable);
        status.put("lastError", lastError);
        try {
            String user = schoolAccount != null && !schoolAccount.isEmpty() ? schoolAccount : enterpriseAccount;
            String url = baseUrl + apiPath + "/trans/handle";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("groupId", groupIdValue());
            body.put("user", user);
            body.put("contractName", "InternshipCertificate");
            body.put("contractAddress", certContractAddr);
            body.put("funcName", "getCertificateCount");
            body.put("funcParam", Collections.emptyList());
            body.put("useCns", false);
            body.put("contractAbi", JSON.readValue(CERT_ABI, java.util.List.class));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Object> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Object.class);
            status.put("chainTest", resp.getBody());
            assertSuccessfulResponse(resp.getBody());
            chainAvailable = true;
            lastError = null;
        } catch (Exception e) {
            lastError = e.getMessage();
            status.put("chainTestError", lastError);
            chainAvailable = false;
        }
        status.put("available", chainAvailable);
        status.put("lastError", lastError);
        return status;
    }
}
