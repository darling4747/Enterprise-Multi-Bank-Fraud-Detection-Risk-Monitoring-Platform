package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    Optional<UserSession> findByIdAndRevokedAtIsNull(Long id);

    List<UserSession> findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(Long userId);
}
