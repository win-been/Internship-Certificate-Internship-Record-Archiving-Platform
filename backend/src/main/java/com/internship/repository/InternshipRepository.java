package com.internship.repository;

import com.internship.entity.InternshipInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipRepository extends JpaRepository<InternshipInfo, Long> {
    List<InternshipInfo> findByStudentId(Long studentId);
    List<InternshipInfo> findByEnterpriseId(Long enterpriseId);
    List<InternshipInfo> findByStudentIdAndEnterpriseId(Long studentId, Long enterpriseId);
    List<InternshipInfo> findBySchoolId(Long schoolId);
    List<InternshipInfo> findByStatus(String status);
    List<InternshipInfo> findByStudentIdAndStatus(Long studentId, String status);
}
