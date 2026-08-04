package com.internship.repository;
import com.internship.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByStatus(String status);
    List<Dispute> findByStudent(String student);
    List<Dispute> findByStudentId(Long studentId);
    List<Dispute> findByCompanyId(Long companyId);
    List<Dispute> findByAssessmentId(Long assessmentId);
}

