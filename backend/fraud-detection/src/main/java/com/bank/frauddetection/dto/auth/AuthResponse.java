package com.bank.frauddetection.dto.auth;

import com.bank.frauddetection.enums.RoleType;
import java.time.Instant;
import java.util.Set;

public record AuthResponse(
        String token,
        String tokenType,
        String refreshToken,
        Long sessionId,
        Instant expiresAt,
        Long userId,
        String username,
        String fullName,
        String email,
        Set<RoleType> roles,
        Long bankId,
        boolean mustChangePassword,
        boolean mfaEnabled,
        boolean mfaRequired,
        int sessionTimeoutMinutes
) {
}
