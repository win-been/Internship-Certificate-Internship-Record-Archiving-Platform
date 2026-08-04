package com.internship.repository;
import com.internship.entity.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArchiveRepository extends JpaRepository<Archive, Long> {
    List<Archive> findByCompanyId(Long companyId);
    List<Archive> findByStudentId(Long studentId);
    List<Archive> findByType(String type);
}
