package com.bank.frauddetection.dto.user;

import java.time.Instant;

public record TemporaryCredentialResponse(
        UserResponse user,
        String temporaryPassword,
        Instant expiresAt
) {
}
