package com.internship.controller;

import com.internship.dto.DailyRecordDTO;
import com.internship.entity.DailyRecord;
import com.internship.entity.InternshipInfo;
import com.internship.entity.User;
import com.internship.repository.InternshipRepository;
import com.internship.repository.UserRepository;
import com.internship.service.DailyRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/daily-records")
@CrossOrigin(origins = "*")
public class DailyRecordController {
    private final DailyRecordService ds;
    private final UserRepository userRepo;
    private final InternshipRepository internshipRepo;

    public DailyRecordController(DailyRecordService s, UserRepository userRepo, InternshipRepository internshipRepo) {
        this.ds = s;
        this.userRepo = userRepo;
        this.internshipRepo = internshipRepo;
    }

    @PostMapping
    public ResponseEntity<DailyRecord> create(Authentication authentication, @RequestBody DailyRecordDTO d) {
        requireStudentActor(authentication, d.getStudentId());
        return ResponseEntity.ok(ds.createRecord(d));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DailyRecord> getById(Authentication authentication, @PathVariable Long id) {
        DailyRecord record = ds.getRecordById(id);
        if (!recordVisibleTo(authentication, record)) throw new RuntimeException("No permission to view this daily record");
        return ResponseEntity.ok(record);
    }

    @GetMapping("/internship/{iid}")
    public ResponseEntity<List<DailyRecord>> getByInternship(Authentication authentication, @PathVariable Long iid) {
        return ResponseEntity.ok(ds.getRecordsByInternship(iid).stream()
                .filter(record -> recordVisibleTo(authentication, record))
                .toList());
    }

    @GetMapping("/student/{sid}")
    public ResponseEntity<List<DailyRecord>> getByStudent(Authentication authentication, @PathVariable Long sid) {
        return ResponseEntity.ok(ds.getRecordsByStudent(sid).stream()
                .filter(record -> recordVisibleTo(authentication, record))
                .toList());
    }

    @GetMapping("/internship/{iid}/date-range")
    public ResponseEntity<List<DailyRecord>> getByDateRange(Authentication authentication, @PathVariable Long iid,
                                                            @RequestParam LocalDate sd, @RequestParam LocalDate ed) {
        return ResponseEntity.ok(ds.getRecordsByDateRange(iid, sd, ed).stream()
                .filter(record -> recordVisibleTo(authentication, record))
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DailyRecord> update(Authentication authentication, @PathVariable Long id, @RequestBody DailyRecordDTO d) {
        DailyRecord record = ds.getRecordById(id);
        requireStudentActor(authentication, record.getStudentId());
        return ResponseEntity.ok(ds.updateRecord(id, d));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<DailyRecord> review(Authentication authentication, @PathVariable Long id,
                                              @RequestParam Long mid, @RequestParam String c, @RequestParam boolean app) {
        DailyRecord record = ds.getRecordById(id);
        User reviewer = requireEnterpriseReviewer(authentication, record);
        Long reviewerId = "PLATFORM_ADMIN".equals(reviewer.getRole()) ? mid : reviewer.getId();
        return ResponseEntity.ok(ds.reviewRecord(id, reviewerId, c, app));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        DailyRecord record = ds.getRecordById(id);
        requireStudentActor(authentication, record.getStudentId());
        ds.deleteRecord(id);
        return ResponseEntity.ok().build();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Please login again");
        return userRepo.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private void requireStudentActor(Authentication authentication, Long studentId) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return;
        if ("STUDENT".equals(viewer.getRole()) && Objects.equals(viewer.getId(), studentId)) return;
        throw new RuntimeException("No permission to manage this student's daily record");
    }

    private User requireEnterpriseReviewer(Authentication authentication, DailyRecord record) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return viewer;
        InternshipInfo internship = internshipFor(record);
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")
                && internship != null && Objects.equals(internship.getEnterpriseId(), viewer.getId())) return viewer;
        throw new RuntimeException("No permission to review this daily record");
    }

    private boolean recordVisibleTo(Authentication authentication, DailyRecord record) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(record.getStudentId(), viewer.getId());
        InternshipInfo internship = internshipFor(record);
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return studentBelongsToSchool(record.getStudentId(), viewerSchoolId(viewer));
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) {
            return internship != null && Objects.equals(internship.getEnterpriseId(), viewer.getId());
        }
        return false;
    }

    private InternshipInfo internshipFor(DailyRecord record) {
        return record.getInternshipId() != null ? internshipRepo.findById(record.getInternshipId()).orElse(null) : null;
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
