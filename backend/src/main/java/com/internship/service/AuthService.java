package com.internship.service;

import com.internship.dto.*;
import com.internship.entity.Approval;
import com.internship.entity.User;
import com.internship.repository.ApprovalRepository;
import com.internship.repository.UserRepository;
import com.internship.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private static final String IDENTITY_APPROVAL = "IDENTITY_VERIFICATION";

    private final UserRepository userRepo;
    private final ApprovalRepository approvalRepo;
    private final BlockchainService blockchainService;
    private final UserChainArchiveService userChainArchiveService;
    private final PasswordEncoder pe;
    private final JwtUtil jwt;
    private final RestTemplate rt = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    @Value("${webase.url:}")
    private String wu;
    @Value("${webase.api-path:/WeBASE-Front}")
    private String ap;

    public AuthService(UserRepository userRepo, ApprovalRepository approvalRepo, BlockchainService blockchainService,
                       UserChainArchiveService userChainArchiveService, PasswordEncoder pe, JwtUtil jwt) {
        this.userRepo = userRepo;
        this.approvalRepo = approvalRepo;
        this.blockchainService = blockchainService;
        this.userChainArchiveService = userChainArchiveService;
        this.pe = pe;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest req) {
        User u = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        if (!pe.matches(req.getPassword(), u.getPassword()))
            throw new RuntimeException("Password is incorrect");
        if (u.getEnabled() != null && !u.getEnabled())
            throw new RuntimeException("User is disabled");
        boolean enterpriseQualificationEditable = u.getRole() != null
                && u.getRole().startsWith("ENTERPRISE")
                && ("PENDING".equals(u.getStatus()) || "REJECTED".equals(u.getStatus()));
        if ("REJECTED".equals(u.getStatus()) && !enterpriseQualificationEditable)
            throw new RuntimeException("Account was rejected" +
                    (u.getRejectReason() != null && !u.getRejectReason().isEmpty() ? ": " + u.getRejectReason() : ""));
        if (!Boolean.TRUE.equals(u.getApproved()) && !enterpriseQualificationEditable)
            throw new RuntimeException("Account is not approved yet");

        String t = jwt.generateToken(u.getId(), u.getUsername(), u.getRole());
        if (!userChainArchiveService.hasUserProfileArchive(u)) {
            java.util.concurrent.CompletableFuture.runAsync(() -> userChainArchiveService.anchorUserProfile(u));
        }
        return LoginResponse.of(t, u.getId(), u.getUsername(), u.getRealName(), u.getRole(),
                u.getSchoolId(), u.getOrganizationName(), u.getMajor(), u.getIdentityStatus());
    }

    @Transactional
    public User register(RegisterRequest req) {
        String username = normalizeBlank(req.getUsername());
        String email = normalizeBlank(req.getEmail());
        String role = normalizeBlank(req.getRole());
        if (role != null) role = role.toUpperCase(Locale.ROOT);
        String idCard = normalizeBlank(req.getIdCard());
        String organizationCode = normalizeBlank(req.getOrganizationCode());
        String realName = normalizeBlank(req.getRealName());
        String organizationName = normalizeBlank(req.getOrganizationName());
        String schoolName = normalizeBlank(req.getSchoolName());
        String phone = normalizeBlank(req.getPhone());
        validateRegisterInput(username, req.getPassword(), role);
        Long schoolId = "STUDENT".equals(role) ? resolveRegisterSchoolId(req.getSchoolId(), schoolName) : null;
        User u = findRosterUser(role, idCard, realName, organizationCode, organizationName, schoolId);
        boolean requiresSchoolApproval = "STUDENT".equals(role)
                && !Boolean.TRUE.equals(u.getApproved())
                && (u.getId() == null || "PENDING".equals(u.getStatus()) || isClaimableRosterUser(u));

        if (u.getId() == null ? userRepo.existsByUsername(username) : userRepo.existsByUsernameAndIdNot(username, u.getId()))
            throw new RuntimeException("Username already exists");
        if (email != null && (u.getId() == null ? userRepo.existsByEmail(email) : userRepo.existsByEmailAndIdNot(email, u.getId())))
            throw new RuntimeException("Email already exists");
        if (phone != null && (u.getId() == null ? userRepo.existsByPhone(phone) : userRepo.existsByPhoneAndIdNot(phone, u.getId())))
            throw new RuntimeException("手机号已被注册");

        String wa = "";
        String pk = "";
        try {
            if (wu != null && !wu.isEmpty()) {
                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);
                ResponseEntity<Map> r = rt.exchange(wu + ap + "/privateKey?type=0&userName=" + username,
                        HttpMethod.GET, new HttpEntity<>(h), Map.class);
                if (r.getBody() != null) {
                    wa = String.valueOf(r.getBody().getOrDefault("address", ""));
                    pk = String.valueOf(r.getBody().getOrDefault("privateKey", ""));
                }
            }
        } catch (Exception e) {
            wa = "0x" + Long.toHexString(System.currentTimeMillis());
        }

        u.setUsername(username);
        u.setPassword(pe.encode(req.getPassword()));
        u.setEmail(email);
        if (realName != null) u.setRealName(realName);
        u.setRole(role);
        if (organizationName != null) u.setOrganizationName(organizationName);
        if (organizationCode != null) u.setOrganizationCode(organizationCode);
        if (phone != null) u.setPhone(phone);
        if ("STUDENT".equals(role)) {
            u.setIdCard(idCard);
            u.setSchoolId(schoolId);
            if (schoolName != null) {
                u.setOrganizationName(schoolName);
            }
        } else if ("SCHOOL_ADMIN".equals(role)) {
            u.setSchoolId(null);
            u.setOrganizationName(organizationName);
            if (realName == null) {
                u.setRealName(organizationName);
            }
        } else if (idCard != null) {
            u.setIdCard(idCard);
        }
        u.setWalletAddress(wa);
        u.setPrivateKey(pk);
        u.setEnabled(true);
        if (requiresSchoolApproval) {
            u.setApproved(false);
            u.setStatus("PENDING");
            u.setApprovedAt(null);
            u.setIdentityStatus("PENDING");
        } else {
            u.setApproved(true);
            u.setStatus("APPROVED");
            u.setApprovedAt(LocalDateTime.now());
            if ("STUDENT".equals(role) && isBlank(u.getIdentityStatus())) {
                u.setIdentityStatus("APPROVED");
            }
        }
        u.setRejectReason(null);
        User saved = userRepo.save(u);
        if ("SCHOOL_ADMIN".equals(role) && saved.getSchoolId() == null) {
            saved.setSchoolId(saved.getId());
            saved = userRepo.save(saved);
        }
        if (requiresSchoolApproval) {
            saveStudentRegistrationApproval(saved);
        }

        userChainArchiveService.anchorUserProfileRequired(saved);
        userChainArchiveService.anchorUserEventRequired(saved,
                "SCHOOL_ADMIN".equals(role) ? "USER_SCHOOL_REGISTER"
                        : requiresSchoolApproval ? "USER_STUDENT_REGISTER_PENDING" : "USER_REGISTER");
        return saved;
    }

    private void validateRegisterInput(String username, String password, String role) {
        if (username == null) throw new RuntimeException("Please enter username");
        if (password == null || password.trim().isEmpty()) throw new RuntimeException("Please enter password");
        if (role == null) throw new RuntimeException("Please select role");
        if (!"STUDENT".equals(role) && !"ENTERPRISE_HR".equals(role) && !"SCHOOL_ADMIN".equals(role)) {
            throw new RuntimeException("This role does not support self registration");
        }
    }

    private String normalizeBlank(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User findRosterUser(String role, String idCard, String realName, String organizationCode, String organizationName, Long schoolId) {
        if (role == null) throw new RuntimeException("Please select role");
        if ("STUDENT".equals(role)) {
            if (idCard == null) throw new RuntimeException("Student registration requires ID card");
            if (schoolId == null) throw new RuntimeException("请选择所属学校");
            List<User> byIdCard = userRepo.findByRoleAndIdCard("STUDENT", idCard);
            Optional<User> rosterByIdCard = byIdCard.stream()
                    .filter(u -> belongsToSchool(u, schoolId))
                    .filter(this::isClaimableRosterUser)
                    .findFirst();
            if (rosterByIdCard.isPresent()) return rosterByIdCard.get();
            if (byIdCard.stream().anyMatch(u -> !belongsToSchool(u, schoolId)))
                throw new RuntimeException("该身份证号不属于所选学校学生名单");
            if (!byIdCard.isEmpty()) throw new RuntimeException("该学生账号已注册，请直接登录或联系管理员重置密码");
            if (realName != null) {
                List<User> byName = userRepo.findByRoleAndRealName("STUDENT", realName).stream()
                        .filter(u -> belongsToSchool(u, schoolId))
                        .filter(u -> isBlank(u.getIdCard()))
                        .filter(this::isClaimableRosterUser)
                        .toList();
                if (byName.size() == 1) return byName.get(0);
                return newPendingStudentUser();
            }
            return newPendingStudentUser();
        }
        if ("SCHOOL_ADMIN".equals(role)) {
            if (organizationName == null) throw new RuntimeException("School administrator registration requires school name");
            if (organizationCode != null) {
                List<User> byCode = userRepo.findByRoleAndOrganizationCode("SCHOOL_ADMIN", organizationCode);
                if (!byCode.isEmpty()) throw new RuntimeException("该学校管理员账号已注册，请直接登录或联系平台管理员");
            }
            List<User> byName = userRepo.findByOrganizationNameAndRoleIn(organizationName, List.of("SCHOOL_ADMIN"));
            if (!byName.isEmpty()) throw new RuntimeException("该学校管理员账号已注册，请直接登录或联系平台管理员");
            return new User();
        }
        if ("ENTERPRISE_HR".equals(role)) {
            if (organizationCode == null) throw new RuntimeException("Enterprise registration requires organization code");
            List<User> byCode = userRepo.findByRoleAndOrganizationCode("ENTERPRISE_HR", organizationCode);
            Optional<User> rosterByCode = byCode.stream().filter(this::isClaimableRosterUser).findFirst();
            if (rosterByCode.isPresent()) return rosterByCode.get();
            if (!byCode.isEmpty()) throw new RuntimeException("该企业账号已注册，请直接登录或联系管理员重置密码");
            if (organizationName != null) {
                List<User> byName = userRepo.findByOrganizationNameAndRoleIn(organizationName, List.of("ENTERPRISE_HR")).stream()
                        .filter(u -> isBlank(u.getOrganizationCode()) || organizationCode.equals(u.getOrganizationCode()))
                        .toList();
                Optional<User> rosterByName = byName.stream()
                        .filter(this::isClaimableRosterUser)
                        .findFirst()
                        ;
                if (rosterByName.isPresent()) return rosterByName.get();
                if (!byName.isEmpty()) throw new RuntimeException("该企业账号已注册，请直接登录或联系管理员重置密码");
                throw new RuntimeException("企业不在名单中，请先由学校端录入企业名称和信用代码");
            }
            throw new RuntimeException("企业不在名单中，请先由学校端录入企业名称和信用代码");
        }
        throw new RuntimeException("This role does not support self registration");
    }

    private User newPendingStudentUser() {
        User user = new User();
        user.setStatus("PENDING");
        user.setApproved(false);
        return user;
    }

    private void saveStudentRegistrationApproval(User user) {
        Approval approval = approvalRepo
                .findFirstByUserIdAndTypeAndStatusOrderByIdDesc(user.getId(), IDENTITY_APPROVAL, "PENDING")
                .orElseGet(Approval::new);
        String schoolName = resolveSchoolName(user.getSchoolId(), user.getOrganizationName());
        approval.setUserId(user.getId());
        approval.setType(IDENTITY_APPROVAL);
        approval.setName(firstNonBlank(user.getRealName(), user.getUsername()));
        approval.setCode(user.getIdCard());
        approval.setSchool(schoolName);
        approval.setMajor(user.getMajor());
        approval.setContact(schoolName);
        approval.setPhone(user.getPhone());
        approval.setStatus("PENDING");
        approval.setDate(LocalDate.now().toString());
        approvalRepo.save(approval);
    }

    private Long resolveRegisterSchoolId(Long schoolId, String schoolName) {
        if (schoolId != null) return schoolId;
        if (schoolName == null) return null;
        return userRepo.findByRole("SCHOOL_ADMIN").stream()
                .filter(school -> sameText(school.getOrganizationName(), schoolName)
                        || sameText(school.getRealName(), schoolName)
                        || sameText(school.getUsername(), schoolName))
                .map(User::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("所选学校不存在，请联系学校管理员"));
    }

    private boolean belongsToSchool(User user, Long schoolId) {
        return schoolId != null && user != null && Objects.equals(user.getSchoolId(), schoolId);
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private boolean isClaimableRosterUser(User user) {
        String status = user.getStatus();
        boolean rosterStatus = status == null || "ROSTER".equals(status);
        return rosterStatus && !Boolean.TRUE.equals(user.getApproved());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    public User getUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    public List<Map<String, Object>> getSchools() {
        return userRepo.findByRole("SCHOOL_ADMIN").stream()
                .map(school -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", school.getId());
                    item.put("name", firstNonBlank(school.getOrganizationName(), school.getRealName(), school.getUsername()));
                    return item;
                })
                .toList();
    }

    public User updateProfile(String username, Map<String, Object> body) {
        User u = getUserByUsername(username);
        String realName = normalizeBlank(textValue(body, "realName"));
        String email = normalizeBlank(textValue(body, "email"));
        String phone = normalizeBlank(textValue(body, "phone"));
        String organizationName = normalizeBlank(textValue(body, "organizationName"));

        if (realName == null) throw new RuntimeException("Please enter real name");
        if (email != null && userRepo.existsByEmailAndIdNot(email, u.getId()))
            throw new RuntimeException("Email already exists");
        if (phone != null && userRepo.existsByPhoneAndIdNot(phone, u.getId()))
            throw new RuntimeException("手机号已被注册");

        u.setRealName(realName);
        u.setEmail(email);
        u.setPhone(phone);
        u.setOrganizationName(organizationName);
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_PROFILE_UPDATE");
        return saved;
    }

    public User changePassword(String username, Map<String, Object> body) {
        User u = getUserByUsername(username);
        String oldPassword = normalizeBlank(textValue(body, "oldPassword"));
        String newPassword = normalizeBlank(textValue(body, "newPassword"));
        if (oldPassword == null) throw new RuntimeException("Please enter current password");
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20)
            throw new RuntimeException("Password must be 6-20 characters");
        if (!pe.matches(oldPassword, u.getPassword()))
            throw new RuntimeException("Current password is incorrect");

        u.setPassword(pe.encode(newPassword));
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_PASSWORD_CHANGE");
        return saved;
    }

    public List<User> getPendingApprovals() {
        return userRepo.findByStatusAndApproved("PENDING", false);
    }

    public User approveUser(Long userId, Long approverId) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        u.setApproved(true);
        u.setStatus("APPROVED");
        u.setApprovedBy(approverId);
        u.setApprovedAt(LocalDateTime.now());
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_APPROVE");
        return saved;
    }

    public User rejectUser(Long userId, Long approverId, String reason) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        u.setApproved(false);
        u.setStatus("REJECTED");
        u.setApprovedBy(approverId);
        u.setApprovedAt(LocalDateTime.now());
        u.setRejectReason(reason);
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_REJECT");
        return saved;
    }

    public List<Map<String, Object>> getAllUsers() {
        List<User> users = userRepo.findAll();
        boolean changed = false;
        for (User user : users) {
            if (isBrokenText(user.getRealName())) {
                user.setRealName(defaultDisplayNameFor(user));
                User saved = userRepo.save(user);
                userChainArchiveService.anchorUserProfile(saved);
                userChainArchiveService.anchorUserEvent(saved, "USER_NAME_BACKFILL");
                changed = true;
            }
            if (isBlank(user.getPhone())) {
                user.setPhone(defaultPhoneFor(user));
                User saved = userRepo.save(user);
                userChainArchiveService.anchorUserProfile(saved);
                userChainArchiveService.anchorUserEvent(saved, "USER_PHONE_BACKFILL");
                changed = true;
            }
        }
        List<User> latest = changed ? userRepo.findAll() : users;
        return latest.stream().map(this::mapUserForAdmin).toList();
    }

    private Map<String, Object> mapUserForAdmin(User user) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", user.getId());
        item.put("username", user.getUsername());
        item.put("email", user.getEmail());
        item.put("realName", user.getRealName());
        item.put("schoolId", user.getSchoolId());
        item.put("role", user.getRole());
        item.put("organizationName", user.getOrganizationName());
        item.put("major", user.getMajor());
        item.put("organizationCode", user.getOrganizationCode());
        item.put("phone", user.getPhone());
        item.put("idCard", user.getIdCard());
        item.put("walletAddress", user.getWalletAddress());
        item.put("enabled", user.getEnabled());
        item.put("status", user.getStatus());
        item.put("identityStatus", user.getIdentityStatus());
        item.put("approved", user.getApproved());
        item.put("approvedBy", user.getApprovedBy());
        item.put("approvedAt", user.getApprovedAt());
        item.put("rejectReason", user.getRejectReason());
        item.put("schoolName", resolveSchoolName(user.getSchoolId()));
        item.put("affiliationName", affiliationNameFor(user));
        return item;
    }

    private String affiliationNameFor(User user) {
        if ("STUDENT".equals(user.getRole())) {
            return firstNonBlank(resolveSchoolName(user.getSchoolId()), user.getOrganizationName(), "未绑定学校");
        }
        if ("SCHOOL_ADMIN".equals(user.getRole())) {
            return firstNonBlank(user.getOrganizationName(), user.getRealName(), resolveSchoolName(user.getSchoolId()), "未填写学校");
        }
        if (user.getRole() != null && user.getRole().startsWith("ENTERPRISE")) {
            return firstNonBlank(user.getOrganizationName(), user.getRealName(), "未填写企业");
        }
        if ("PLATFORM_ADMIN".equals(user.getRole())) {
            return "平台管理";
        }
        return firstNonBlank(user.getOrganizationName(), resolveSchoolName(user.getSchoolId()), "-");
    }

    private String resolveSchoolName(Long schoolId) {
        if (schoolId == null) return null;
        return userRepo.findById(schoolId)
                .filter(school -> "SCHOOL_ADMIN".equals(school.getRole()))
                .map(school -> firstNonBlank(school.getOrganizationName(), school.getRealName(), school.getUsername()))
                .orElse(null);
    }

    private String resolveSchoolName(Long schoolId, String fallback) {
        String schoolName = resolveSchoolName(schoolId);
        return !isBlank(schoolName) ? schoolName : fallback;
    }

    public User resetPassword(Long userId, String newPassword) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        u.setPassword(pe.encode(newPassword));
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_PASSWORD_RESET");
        return saved;
    }

    public User setUserEnabled(Long userId, boolean enabled) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        u.setEnabled(enabled);
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, enabled ? "USER_ENABLE" : "USER_DISABLE");
        return saved;
    }

    public Map<String, Object> syncAllUsersToChain() {
        return userChainArchiveService.syncAllUserProfiles(userRepo.findAll());
    }

    private String textValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return null;
        return body.get(key).toString();
    }

    private String defaultPhoneFor(User user) {
        long seed = user != null && user.getId() != null ? user.getId() : System.currentTimeMillis() % 100000;
        return "13" + String.format("%09d", seed % 1000000000L);
    }

    private boolean isBrokenText(String value) {
        if (value == null) return false;
        String text = value.trim();
        return text.matches("\\?{2,}") || text.contains("�") || text.contains("å") || text.contains("æ") || text.contains("ç");
    }

    private String defaultDisplayNameFor(User user) {
        if (user == null) return "用户";
        if ("SCHOOL_ADMIN".equals(user.getRole())) return firstNonBlank(user.getOrganizationName(), "学校管理员");
        if (user.getRole() != null && user.getRole().startsWith("ENTERPRISE")) {
            return firstNonBlank(user.getOrganizationName(), user.getUsername(), "企业用户");
        }
        if ("PLATFORM_ADMIN".equals(user.getRole())) return firstNonBlank(user.getUsername(), "平台管理员");
        return firstNonBlank(user.getUsername(), "用户");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value) && !isBrokenText(value)) return value.trim();
        }
        return "用户";
    }
}
