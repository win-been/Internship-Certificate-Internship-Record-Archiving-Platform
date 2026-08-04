package com.internship.repository;
import com.internship.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobId(Long jobId);
    List<Application> findByStatus(String status);
    List<Application> findByCompanyId(Long companyId);
    List<Application> findByStudentId(Long studentId);
    List<Application> findByStudentIdAndCompanyId(Long studentId, Long companyId);
    List<Application> findByCompanyIdAndJobId(Long companyId, Long jobId);
    List<Application> findByStudentIdAndCompanyIdAndStatus(Long studentId, Long companyId, String status);
}
