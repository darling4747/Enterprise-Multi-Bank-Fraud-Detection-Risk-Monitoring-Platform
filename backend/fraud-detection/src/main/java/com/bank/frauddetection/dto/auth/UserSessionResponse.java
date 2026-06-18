package com.bank.frauddetection.dto.auth;

import java.time.Instant;

public record UserSessionResponse(
        Long id,
        String deviceId,
        String browser,
        String operatingSystem,
        String ipAddress,
        String userAgent,
        Instant loginTime,
        Instant lastSeenAt,
        Instant expiresAt,
        boolean current
) {
}
