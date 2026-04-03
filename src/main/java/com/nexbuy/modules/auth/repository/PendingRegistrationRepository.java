package com.nexbuy.modules.auth.repository;

import com.nexbuy.modules.auth.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findByEmail(String email);
    Optional<PendingRegistration> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
}