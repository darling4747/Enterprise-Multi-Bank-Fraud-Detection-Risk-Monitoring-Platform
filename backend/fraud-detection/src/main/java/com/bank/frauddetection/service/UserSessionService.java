package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.auth.UserSessionResponse;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.entity.UserSession;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTtlSeconds;

    public UserSessionService(
            UserSessionRepository userSessionRepository,
            @Value("${app.security.refresh-token-ttl-seconds:604800}") long refreshTtlSeconds
    ) {
        this.userSessionRepository = userSessionRepository;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    @Transactional
    public CreatedSession createSession(User user, String deviceId, String userAgent, String ipAddress) {
        String refreshToken = generateRefreshToken();
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hash(refreshToken));
        session.setDeviceId(clean(deviceId, 120));
        session.setUserAgent(clean(userAgent, 600));
        session.setIpAddress(clean(ipAddress, 80));
        session.setBrowser(browser(userAgent));
        session.setOperatingSystem(operatingSystem(userAgent));
        session.setLastSeenAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        session = userSessionRepository.save(session);
        return new CreatedSession(session, refreshToken);
    }

    @Transactional
    public UserSession useRefreshToken(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash(refreshToken), Instant.now())
                .orElseThrow(() -> new BusinessException("Refresh token is invalid or expired"));
        session.setLastSeenAt(Instant.now());
        return userSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listSessions(User user, Long currentSessionId) {
        return userSessionRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(user.getId(), Instant.now())
                .stream()
                .map(session -> toResponse(session, currentSessionId))
                .toList();
    }

    @Transactional
    public void revokeOtherSessions(User user, Long currentSessionId) {
        userSessionRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(user.getId(), Instant.now())
                .stream()
                .filter(session -> currentSessionId == null || !session.getId().equals(currentSessionId))
                .forEach(session -> session.setRevokedAt(Instant.now()));
    }

    private UserSessionResponse toResponse(UserSession session, Long currentSessionId) {
        return new UserSessionResponse(
                session.getId(),
                session.getDeviceId(),
                session.getBrowser(),
                session.getOperatingSystem(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getCreatedAt(),
                session.getLastSeenAt(),
                session.getExpiresAt(),
                currentSessionId != null && currentSessionId.equals(session.getId())
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    private String browser(String userAgent) {
        String value = userAgent == null ? "" : userAgent;
        if (value.contains("Edg/")) {
            return "Microsoft Edge";
        }
        if (value.contains("Chrome/")) {
            return "Chrome";
        }
        if (value.contains("Firefox/")) {
            return "Firefox";
        }
        if (value.contains("Safari/")) {
            return "Safari";
        }
        return "Unknown Browser";
    }

    private String operatingSystem(String userAgent) {
        String value = userAgent == null ? "" : userAgent;
        if (value.contains("Windows")) {
            return "Windows";
        }
        if (value.contains("Mac OS")) {
            return "macOS";
        }
        if (value.contains("Android")) {
            return "Android";
        }
        if (value.contains("iPhone") || value.contains("iPad")) {
            return "iOS";
        }
        if (value.contains("Linux")) {
            return "Linux";
        }
        return "Unknown OS";
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    public record CreatedSession(UserSession session, String refreshToken) {
    }
}
