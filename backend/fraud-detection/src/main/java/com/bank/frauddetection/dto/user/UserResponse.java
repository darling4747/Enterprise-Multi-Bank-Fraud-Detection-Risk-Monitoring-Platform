package com.bank.frauddetection.dto.user;

import com.bank.frauddetection.enums.PasswordStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.UserStatus;
import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Long bankId,
        String bankCode,
        Long branchId,
        String branchCode,
        String employeeId,
        boolean enabled,
        UserStatus status,
        Set<RoleType> roles,
        Long createdByUserId,
        Instant createdAt,
        PasswordStatus passwordStatus,
        boolean mustChangePassword,
        Instant temporaryPasswordExpiresAt,
        String visibleTemporaryPassword,
        Instant lastLoginAt,
        int failedLoginAttempts,
        Instant accountLockedUntil
) {
}
