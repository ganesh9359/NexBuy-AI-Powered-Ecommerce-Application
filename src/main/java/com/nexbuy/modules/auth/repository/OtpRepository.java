package com.nexbuy.modules.auth.repository;

import com.nexbuy.modules.auth.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, String purpose);
    void deleteByExpiresAtBefore(LocalDateTime time);
}
