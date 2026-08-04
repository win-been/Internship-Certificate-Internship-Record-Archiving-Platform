package com.internship.repository;
import com.internship.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByStudent(String student);
    List<Assessment> findByMonth(String month);
    List<Assessment> findByCompanyId(Long companyId);
    List<Assessment> findByStudentId(Long studentId);
    List<Assessment> findByInternshipId(Long internshipId);
    Optional<Assessment> findByStudentAndMonth(String student, String month);
    Optional<Assessment> findFirstByInternshipIdAndMonth(Long internshipId, String month);
}
