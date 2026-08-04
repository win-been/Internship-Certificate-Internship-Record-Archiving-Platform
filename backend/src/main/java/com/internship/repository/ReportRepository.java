package com.internship.repository;

import com.internship.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStudentId(Long studentId);
    List<Report> findByInternshipId(Long internshipId);
    List<Report> findByEnterpriseId(Long enterpriseId);
}
