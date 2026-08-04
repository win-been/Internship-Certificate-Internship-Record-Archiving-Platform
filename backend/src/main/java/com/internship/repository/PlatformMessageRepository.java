package com.internship.repository;

import com.internship.entity.PlatformMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformMessageRepository extends JpaRepository<PlatformMessage, Long> {
}
