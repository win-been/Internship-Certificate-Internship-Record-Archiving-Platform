package com.internship.controller;

import com.internship.dto.InternshipInfoDTO;
import com.internship.entity.InternshipInfo;
import com.internship.entity.User;
import com.internship.repository.UserRepository;
import com.internship.service.InternshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/internships")
@CrossOrigin(origins = "*")
public class InternshipController {
    private final InternshipService is;
    private final UserRepository userRepo;

    public InternshipController(InternshipService s, UserRepository userRepo) {
        this.is = s;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<InternshipInfo> create(Authentication authentication, @RequestBody InternshipInfoDTO d) {
        requireEnterpriseActor(authentication, d.getEnterpriseId());
        return ResponseEntity.ok(is.registerInternship(d));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternshipInfo> getById(Authentication authentication, @PathVariable Long id) {
        InternshipInfo internship = is.getInternshipById(id);
        if (!internshipVisibleTo(authentication, internship)) throw new RuntimeException("No permission to view this internship");
        return ResponseEntity.ok(internship);
    }

    @GetMapping("/student/{sid}")
    public ResponseEntity<List<InternshipInfo>> getByStudent(Authentication authentication, @PathVariable Long sid) {
        return ResponseEntity.ok(is.getInternshipsByStudent(sid).stream()
                .filter(internship -> internshipVisibleTo(authentication, internship))
                .toList());
    }

    @GetMapping("/enterprise/{eid}")
    public ResponseEntity<List<InternshipInfo>> getByEnterprise(Authentication authentication, @PathVariable Long eid) {
        return ResponseEntity.ok(is.getInternshipsByEnterprise(eid).stream()
                .filter(internship -> internshipVisibleTo(authentication, internship))
                .toList());
    }

    @GetMapping("/school/{sid}")
    public ResponseEntity<List<InternshipInfo>> getBySchool(Authentication authentication, @PathVariable Long sid) {
        User viewer = currentUser(authentication);
        if (!"PLATFORM_ADMIN".equals(viewer.getRole()) && !("SCHOOL_ADMIN".equals(viewer.getRole()) && Objects.equals(viewerSchoolId(viewer), sid))) {
            throw new RuntimeException("No permission to view this school's internships");
        }
        return ResponseEntity.ok(is.getInternshipsBySchool(sid));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InternshipInfo> update(Authentication authentication, @PathVariable Long id, @RequestBody InternshipInfoDTO d) {
        InternshipInfo internship = is.getInternshipById(id);
        requireEnterpriseActor(authentication, internship.getEnterpriseId());
        return ResponseEntity.ok(is.updateInternship(id, d));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<InternshipInfo> updateStatus(Authentication authentication, @PathVariable Long id, @RequestParam String status) {
        InternshipInfo internship = is.getInternshipById(id);
        requireEnterpriseActor(authentication, internship.getEnterpriseId());
        return ResponseEntity.ok(is.updateInternshipStatus(id, status));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Please login again");
        return userRepo.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private void requireEnterpriseActor(Authentication authentication, Long enterpriseId) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return;
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE") && Objects.equals(viewer.getId(), enterpriseId)) return;
        throw new RuntimeException("No permission to manage this internship");
    }

    private boolean internshipVisibleTo(Authentication authentication, InternshipInfo internship) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(internship.getStudentId(), viewer.getId());
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return studentBelongsToSchool(internship.getStudentId(), viewerSchoolId(viewer));
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(internship.getEnterpriseId(), viewer.getId());
        return false;
    }

    private boolean studentBelongsToSchool(Long studentId, Long schoolId) {
        return studentId != null && schoolId != null && userRepo.findById(studentId)
                .map(student -> Objects.equals(student.getSchoolId(), schoolId))
                .orElse(false);
    }

    private Long viewerSchoolId(User user) {
        if (user == null) return null;
        return user.getSchoolId() != null ? user.getSchoolId() : user.getId();
    }
}
