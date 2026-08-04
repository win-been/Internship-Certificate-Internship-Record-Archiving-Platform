package com.internship.controller;

import com.internship.dto.*;
import com.internship.entity.User;
import com.internship.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService as;

    public AuthController(AuthService as) {
        this.as = as;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(as.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(as.register(req));
    }

    @GetMapping("/schools")
    public ResponseEntity<List<Map<String, Object>>> schools() {
        return ResponseEntity.ok(as.getSchools());
    }

    @GetMapping("/user/{uid}")
    public ResponseEntity<User> getUser(Authentication authentication, @PathVariable Long uid) {
        requireSelfOrPlatformAdmin(authentication, uid);
        return ResponseEntity.ok(as.getUserById(uid));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(as.getUserByUsername(currentUsername(authentication)));
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(Authentication authentication, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(as.updateProfile(currentUsername(authentication), body));
    }

    @PutMapping("/me/password")
    public ResponseEntity<User> changeCurrentPassword(Authentication authentication, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(as.changePassword(currentUsername(authentication), body));
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<List<User>> getPendingApprovals(Authentication authentication) {
        requirePlatformAdmin(authentication);
        return ResponseEntity.ok(as.getPendingApprovals());
    }

    @PutMapping("/approve/{userId}")
    public ResponseEntity<User> approveUser(Authentication authentication, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        User approver = requirePlatformAdmin(authentication);
        Long approverId = approver.getId();
        return ResponseEntity.ok(as.approveUser(userId, approverId));
    }

    @PutMapping("/reject/{userId}")
    public ResponseEntity<User> rejectUser(Authentication authentication, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        User approver = requirePlatformAdmin(authentication);
        Long approverId = approver.getId();
        String reason = body.get("reason") != null ? body.get("reason").toString() : "";
        return ResponseEntity.ok(as.rejectUser(userId, approverId, reason));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(Authentication authentication) {
        requirePlatformAdmin(authentication);
        return ResponseEntity.ok(as.getAllUsers());
    }

    @PutMapping("/users/{userId}/reset-pwd")
    public ResponseEntity<User> resetPassword(Authentication authentication, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        requirePlatformAdmin(authentication);
        String newPassword = body.get("password") != null ? body.get("password").toString() : "123456";
        return ResponseEntity.ok(as.resetPassword(userId, newPassword));
    }

    @PutMapping("/users/{userId}/enabled")
    public ResponseEntity<User> setUserEnabled(Authentication authentication, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        requirePlatformAdmin(authentication);
        boolean enabled = body.get("enabled") == null || Boolean.parseBoolean(body.get("enabled").toString());
        return ResponseEntity.ok(as.setUserEnabled(userId, enabled));
    }

    @PostMapping("/users/sync-chain")
    public ResponseEntity<Map<String, Object>> syncAllUsersToChain(Authentication authentication) {
        requirePlatformAdmin(authentication);
        return ResponseEntity.ok(as.syncAllUsersToChain());
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Please login again");
        }
        return authentication.getName();
    }

    private User currentUser(Authentication authentication) {
        return as.getUserByUsername(currentUsername(authentication));
    }

    private User requirePlatformAdmin(Authentication authentication) {
        User user = currentUser(authentication);
        if (!"PLATFORM_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only platform admin can perform this operation");
        }
        return user;
    }

    private void requireSelfOrPlatformAdmin(Authentication authentication, Long userId) {
        User user = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(user.getRole()) || user.getId().equals(userId)) return;
        throw new RuntimeException("No permission to view this user");
    }
}
