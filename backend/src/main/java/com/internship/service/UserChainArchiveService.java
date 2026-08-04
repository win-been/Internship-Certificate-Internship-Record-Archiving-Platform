package com.internship.service;

import com.internship.entity.Archive;
import com.internship.entity.User;
import com.internship.repository.ArchiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserChainArchiveService {
    private static final Logger log = LoggerFactory.getLogger(UserChainArchiveService.class);
    public static final String USER_PROFILE = "USER_PROFILE";

    private final ArchiveRepository archiveRepo;
    private final BlockchainService blockchainService;

    public UserChainArchiveService(ArchiveRepository archiveRepo, BlockchainService blockchainService) {
        this.archiveRepo = archiveRepo;
        this.blockchainService = blockchainService;
    }

    public boolean hasUserProfileArchive(User user) {
        if (user == null || user.getId() == null) return false;
        return findUserProfileArchive(user).isPresent();
    }

    public Archive anchorUserProfile(User user) {
        return anchorUserProfile(user, true);
    }

    public Archive anchorUserProfileRequired(User user) {
        Archive archive = anchorUserProfile(user, true);
        requireOnChain(archive, USER_PROFILE);
        return archive;
    }

    public Archive anchorUserProfile(User user, boolean attemptChain) {
        requireSavedUser(user);
        String payload = profilePayload(user);
        String hash = BlockchainService.calculateHash(payload);
        Archive archive = findUserProfileArchive(user).orElseGet(Archive::new);
        if (hash.equals(archive.getHash()) && "ON_CHAIN".equals(archive.getChainStatus()) && hasText(archive.getTxHash())) {
            return archive;
        }
        fillUserArchive(archive, user, USER_PROFILE, "user:" + user.getId() + ":profile", hash);
        return saveWithChainState(archive, user, attemptChain);
    }

    public Archive anchorUserEvent(User user, String eventType) {
        return anchorUserEvent(user, eventType, true);
    }

    public Archive anchorUserEventRequired(User user, String eventType) {
        Archive archive = anchorUserEvent(user, eventType, true);
        requireOnChain(archive, eventType);
        return archive;
    }

    private Archive anchorUserEvent(User user, String eventType, boolean attemptChain) {
        requireSavedUser(user);
        String normalizedEvent = hasText(eventType) ? eventType : "USER_EVENT";
        Archive archive = new Archive();
        String payload = eventPayload(user, normalizedEvent);
        fillUserArchive(
                archive,
                user,
                normalizedEvent,
                "user:" + user.getId() + ":" + normalizedEvent + ":" + System.currentTimeMillis(),
                BlockchainService.calculateHash(payload)
        );
        return saveWithChainState(archive, user, attemptChain);
    }

    private void requireOnChain(Archive archive, String type) {
        if (archive == null || !"ON_CHAIN".equals(archive.getChainStatus()) || !hasText(archive.getTxHash())) {
            String error = archive != null ? archive.getChainError() : null;
            throw new RuntimeException("Required chain archive failed for " + valueOrEmpty(type)
                    + (hasText(error) ? ": " + error : ""));
        }
    }

    public Map<String, Object> syncAllUserProfiles(Collection<User> users) {
        boolean attemptChain = Boolean.TRUE.equals(blockchainService.checkHealth().get("available"));
        int total = 0;
        int onChain = 0;
        int fallback = 0;
        int eventOnChain = 0;
        int eventFallback = 0;
        int eventSkipped = 0;
        int failed = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        for (User user : users) {
            if (user == null || user.getId() == null) continue;
            total++;
            try {
                Archive archive = anchorUserProfile(user, attemptChain);
                String status = archive.getChainStatus();
                if ("ON_CHAIN".equals(status)) onChain++;
                else fallback++;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", user.getId());
                item.put("username", valueOrEmpty(user.getUsername()));
                item.put("archiveId", archive.getId());
                item.put("chainStatus", valueOrEmpty(status));
                item.put("txHash", valueOrEmpty(archive.getTxHash()));
                item.put("chainError", valueOrEmpty(archive.getChainError()));
                if (hasUserRegisterArchive(user)) {
                    eventSkipped++;
                    item.put("registerEvent", "EXISTS");
                } else {
                    Archive eventArchive = anchorUserEvent(user, registerEventType(user), attemptChain);
                    if ("ON_CHAIN".equals(eventArchive.getChainStatus())) eventOnChain++;
                    else eventFallback++;
                    item.put("registerEvent", eventArchive.getType());
                    item.put("registerEventArchiveId", eventArchive.getId());
                    item.put("registerEventChainStatus", valueOrEmpty(eventArchive.getChainStatus()));
                    item.put("registerEventTxHash", valueOrEmpty(eventArchive.getTxHash()));
                    item.put("registerEventChainError", valueOrEmpty(eventArchive.getChainError()));
                }
                items.add(item);
            } catch (Exception e) {
                failed++;
                items.add(Map.of(
                        "userId", user.getId(),
                        "username", valueOrEmpty(user.getUsername()),
                        "chainStatus", "FAILED",
                        "error", e.getMessage() != null ? e.getMessage() : "unknown"
                ));
                log.warn("Failed to sync user {} to chain: {}", user.getId(), e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("onChain", onChain);
        result.put("localFallback", fallback);
        result.put("registerEventOnChain", eventOnChain);
        result.put("registerEventLocalFallback", eventFallback);
        result.put("registerEventSkipped", eventSkipped);
        result.put("failed", failed);
        result.put("attemptedChain", attemptChain);
        result.put("items", items);
        return result;
    }

    private Optional<Archive> findUserProfileArchive(User user) {
        return archiveRepo.findByStudentId(user.getId()).stream()
                .filter(a -> USER_PROFILE.equals(a.getType()))
                .filter(a -> ("user:" + user.getId() + ":profile").equals(a.getName()))
                .findFirst();
    }

    private boolean hasUserRegisterArchive(User user) {
        return archiveRepo.findByStudentId(user.getId()).stream()
                .anyMatch(a -> "USER_REGISTER".equals(a.getType()) || "USER_SCHOOL_REGISTER".equals(a.getType()));
    }

    private String registerEventType(User user) {
        return user != null && "SCHOOL_ADMIN".equals(user.getRole()) ? "USER_SCHOOL_REGISTER" : "USER_REGISTER";
    }

    private void fillUserArchive(Archive archive, User user, String type, String name, String hash) {
        archive.setType(type);
        archive.setName(name);
        archive.setHash(hash);
        archive.setTime(nowText());
        archive.setStudentId(user.getId());
        archive.setCompanyId(isEnterprise(user) ? user.getId() : null);
        archive.setBlock(null);
        archive.setTxHash(null);
        archive.setChainStatus("LOCAL_FALLBACK");
        archive.setChainError(null);
    }

    private Archive saveWithChainState(Archive archive, User user, boolean attemptChain) {
        if (attemptChain) {
            try {
                String txHash = blockchainService.createCertificate(
                        user.getId(),
                        user.getId(),
                        archive.getHash(),
                        blockchainService.getSchoolAccount(),
                        blockchainService.getEnterpriseAccount()
                );
                if (BlockchainService.isTransactionHash(txHash)) {
                    archive.setTxHash(txHash);
                    archive.setChainStatus("ON_CHAIN");
                    archive.setChainError(null);
                    archive.setBlock(System.currentTimeMillis() % 20000000L + 18400000L);
                } else {
                    archive.setChainError("WeBASE did not return a valid transactionHash: " + txHash);
                }
            } catch (Exception e) {
                archive.setChainError(e.getMessage());
                log.warn("User archive fallback for user {} type {}: {}", user.getId(), archive.getType(), e.getMessage());
            }
        } else {
            archive.setChainError("WeBASE health check unavailable; chain write skipped");
        }
        return archiveRepo.save(archive);
    }

    private String profilePayload(User user) {
        return String.join("|",
                "USER_PROFILE",
                "id=" + user.getId(),
                "username=" + valueOrEmpty(user.getUsername()),
                "role=" + valueOrEmpty(user.getRole()),
                "realName=" + valueOrEmpty(user.getRealName()),
                "email=" + valueOrEmpty(user.getEmail()),
                "schoolId=" + valueOrEmpty(user.getSchoolId()),
                "organizationName=" + valueOrEmpty(user.getOrganizationName()),
                "major=" + valueOrEmpty(user.getMajor()),
                "organizationCode=" + valueOrEmpty(user.getOrganizationCode()),
                "phone=" + valueOrEmpty(user.getPhone()),
                "idCardHash=" + sensitiveHash(user.getIdCard()),
                "walletAddress=" + valueOrEmpty(user.getWalletAddress()),
                "enabled=" + valueOrEmpty(user.getEnabled()),
                "approved=" + valueOrEmpty(user.getApproved()),
                "status=" + valueOrEmpty(user.getStatus()),
                "identityStatus=" + valueOrEmpty(user.getIdentityStatus()),
                "approvedBy=" + valueOrEmpty(user.getApprovedBy()),
                "approvedAt=" + valueOrEmpty(user.getApprovedAt()),
                "rejectReasonHash=" + sensitiveHash(user.getRejectReason())
        );
    }

    private String eventPayload(User user, String eventType) {
        return profilePayload(user) + "|event=" + eventType + "|eventTime=" + nowText();
    }

    private void requireSavedUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("user must be saved before archive");
        }
    }

    private boolean isEnterprise(User user) {
        return user.getRole() != null && user.getRole().startsWith("ENTERPRISE");
    }

    private String sensitiveHash(String value) {
        return hasText(value) ? BlockchainService.calculateHash(value) : "";
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nowText() {
        return LocalDateTime.now().toString().replace("T", " ").substring(0, 19);
    }
}
