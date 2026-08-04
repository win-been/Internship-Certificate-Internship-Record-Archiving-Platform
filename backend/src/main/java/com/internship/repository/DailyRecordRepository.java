package com.internship.repository;

import com.internship.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    List<DailyRecord> findByStudentId(Long studentId);
    List<DailyRecord> findByInternshipId(Long internshipId);
    List<DailyRecord> findByInternshipIdAndRecordDateBetween(Long internshipId, java.time.LocalDate start, java.time.LocalDate end);
}
