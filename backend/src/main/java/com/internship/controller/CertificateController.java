package com.internship.controller;

import com.internship.dto.CertificateDTO;
import com.internship.entity.Certificate;
import com.internship.entity.InternshipInfo;
import com.internship.entity.User;
import com.internship.repository.InternshipRepository;
import com.internship.repository.UserRepository;
import com.internship.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {
    private final CertificateService cs;
    private final UserRepository userRepo;
    private final InternshipRepository internshipRepo;

    public CertificateController(CertificateService s, UserRepository userRepo, InternshipRepository internshipRepo) {
        this.cs = s;
        this.userRepo = userRepo;
        this.internshipRepo = internshipRepo;
    }

    @PostMapping
    public ResponseEntity<Certificate> create(Authentication authentication, @RequestBody CertificateDTO d) {
        InternshipInfo internship = internshipRepo.findById(d.getInternshipId())
                .orElseThrow(() -> new RuntimeException("Internship record does not exist"));
        if (!certificateWritableBySchool(authentication, internship) && !certificateWritableByEnterprise(authentication, internship)) {
            throw new RuntimeException("No permission to create this certificate");
        }
        return ResponseEntity.ok(cs.createCertificate(d));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getById(Authentication authentication, @PathVariable Long id) {
        Certificate certificate = cs.getCertificateById(id);
        if (!certificateVisibleTo(authentication, certificate)) throw new RuntimeException("No permission to view this certificate");
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/number/{n}")
    public ResponseEntity<Certificate> getByNum(Authentication authentication, @PathVariable String n) {
        Certificate certificate = cs.getCertificateByNumber(n);
        if (!certificateVisibleTo(authentication, certificate)) throw new RuntimeException("No permission to view this certificate");
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/student/{sid}")
    public ResponseEntity<List<Certificate>> getByStudent(Authentication authentication, @PathVariable Long sid) {
        return ResponseEntity.ok(cs.getCertificatesByStudent(sid).stream()
                .filter(certificate -> certificateVisibleTo(authentication, certificate))
                .toList());
    }

    @GetMapping("/internship/{iid}")
    public ResponseEntity<List<Certificate>> getByInternship(Authentication authentication, @PathVariable Long iid) {
        return ResponseEntity.ok(cs.getCertificatesByInternship(iid).stream()
                .filter(certificate -> certificateVisibleTo(authentication, certificate))
                .toList());
    }

    @PostMapping("/{id}/approve-school")
    public ResponseEntity<Certificate> approveSchool(Authentication authentication, @PathVariable Long id,
                                                     @RequestParam(required = false) Long approverId) {
        Certificate certificate = cs.getCertificateById(id);
        InternshipInfo internship = internshipFor(certificate);
        if (!certificateWritableBySchool(authentication, internship)) throw new RuntimeException("No permission to approve this certificate as school");
        User approver = currentUser(authentication);
        return ResponseEntity.ok(cs.approveBySchool(id, approver.getId()));
    }

    @PostMapping("/{id}/approve-enterprise")
    public ResponseEntity<Certificate> approveEnterprise(Authentication authentication, @PathVariable Long id,
                                                         @RequestParam(required = false) Long approverId) {
        Certificate certificate = cs.getCertificateById(id);
        InternshipInfo internship = internshipFor(certificate);
        if (!certificateWritableByEnterprise(authentication, internship)) throw new RuntimeException("No permission to approve this certificate as enterprise");
        User approver = currentUser(authentication);
        return ResponseEntity.ok(cs.approveByEnterprise(id, approver.getId()));
    }

    @PostMapping("/{id}/upload-blockchain")
    public ResponseEntity<Certificate> upload(Authentication authentication, @PathVariable Long id) {
        Certificate certificate = cs.getCertificateById(id);
        if (!certificateVisibleTo(authentication, certificate)) throw new RuntimeException("No permission to upload this certificate");
        return ResponseEntity.ok(cs.uploadToBlockchain(id));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String cn, @RequestParam String ch) {
        boolean ok = cs.verifyCertificate(cn, ch);
        Map<String, Object> r = new HashMap<>();
        r.put("verified", ok);
        return ResponseEntity.ok(r);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Please login again");
        return userRepo.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private InternshipInfo internshipFor(Certificate certificate) {
        return certificate.getInternshipId() != null
                ? internshipRepo.findById(certificate.getInternshipId()).orElse(null)
                : null;
    }

    private boolean certificateVisibleTo(Authentication authentication, Certificate certificate) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        InternshipInfo internship = internshipFor(certificate);
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(certificate.getStudentId(), viewer.getId());
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return studentBelongsToSchool(certificate.getStudentId(), viewerSchoolId(viewer));
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) {
            return internship != null && Objects.equals(internship.getEnterpriseId(), viewer.getId());
        }
        return false;
    }

    private boolean certificateWritableBySchool(Authentication authentication, InternshipInfo internship) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        return "SCHOOL_ADMIN".equals(viewer.getRole()) && internship != null
                && studentBelongsToSchool(internship.getStudentId(), viewerSchoolId(viewer));
    }

    private boolean certificateWritableByEnterprise(Authentication authentication, InternshipInfo internship) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        return viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")
                && internship != null && Objects.equals(internship.getEnterpriseId(), viewer.getId());
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

