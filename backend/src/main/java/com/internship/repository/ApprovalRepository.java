package com.internship.repository;

import com.internship.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    Optional<Approval> findFirstByUserIdAndTypeOrderByIdDesc(Long userId, String type);
    Optional<Approval> findFirstByUserIdAndTypeAndStatusOrderByIdDesc(Long userId, String type, String status);
}
