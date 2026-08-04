package com.internship.repository;

import com.internship.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByStudentId(Long studentId);
    List<Certificate> findByInternshipId(Long internshipId);
    List<Certificate> findByStatus(String status);
    java.util.Optional<Certificate> findByCertificateNumber(String certificateNumber);
}
