package com.internship.controller;

import com.internship.entity.Assessment;
import com.internship.entity.InternshipInfo;
import com.internship.entity.User;
import com.internship.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/enterprise")
@CrossOrigin(origins = "*")
public class EnterpriseController {
    private final UserRepository userRepo;
    private final InternshipRepository internshipRepo;
    private final JobRepository jobRepo;
    private final AssessmentRepository assessmentRepo;
    private final ArchiveRepository archiveRepo;
    private final DisputeRepository disputeRepo;

    public EnterpriseController(UserRepository userRepo, InternshipRepository internshipRepo,
                                JobRepository jobRepo, AssessmentRepository assessmentRepo,
                                ArchiveRepository archiveRepo, DisputeRepository disputeRepo) {
        this.userRepo = userRepo;
        this.internshipRepo = internshipRepo;
        this.jobRepo = jobRepo;
        this.assessmentRepo = assessmentRepo;
        this.archiveRepo = archiveRepo;
        this.disputeRepo = disputeRepo;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication authentication) {
        User enterprise = currentEnterprise(authentication);
        Long companyId = enterprise.getId();
        String thisMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        long activeInterns = internshipRepo.findByEnterpriseId(companyId).stream()
                .filter(i -> "ACTIVE".equals(i.getStatus()) || Boolean.TRUE.equals(i.getAgreementSigned()))
                .count();
        long openJobs = jobRepo.findByCompanyId(companyId).stream()
                .filter(j -> !"CLOSED".equals(j.getStatus()))
                .count();
        List<Assessment> assessments = assessmentRepo.findByCompanyId(companyId);
        long monthAssessments = assessments.stream()
                .filter(a -> a.getMonth() != null && a.getMonth().startsWith(thisMonth))
                .count();
        long archives = archiveRepo.findByCompanyId(companyId).size();
        long pendingAssessments = assessments.stream()
                .filter(a -> !"COMPLETED".equals(a.getStatus()) && !"Completed".equals(a.getStatus()) && !"已完成".equals(a.getStatus()))
                .count();
        long pendingDisputes = disputeRepo.findByCompanyId(companyId).stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stats", List.of(
                statItem("在岗实习生", activeInterns, 0),
                statItem("发布岗位", openJobs, 0),
                statItem("本月考核", monthAssessments, 0),
                statItem("存证单据", archives, 0),
                statItem("待处理考核", pendingAssessments, 0),
                statItem("待处理纠纷", pendingDisputes, 0)
        ));
        return ResponseEntity.ok(data);
    }

    @GetMapping("/badges")
    public ResponseEntity<Map<String, Object>> badges(Authentication authentication) {
        Long companyId = currentEnterprise(authentication).getId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingAssessment", assessmentRepo.findByCompanyId(companyId).stream()
                .filter(a -> !"COMPLETED".equals(a.getStatus()) && !"Completed".equals(a.getStatus()) && !"已完成".equals(a.getStatus()))
                .count());
        data.put("pendingDisputes", disputeRepo.findByCompanyId(companyId).stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .count());
        return ResponseEntity.ok(data);
    }

    private User currentEnterprise(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Please login again");
        }
        User user = userRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        if (user.getRole() == null || !user.getRole().startsWith("ENTERPRISE")) {
            throw new RuntimeException("Only enterprise users can view dashboard");
        }
        return user;
    }

    private Map<String, Object> statItem(String label, long value, int trend) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("trend", trend);
        return item;
    }
}
