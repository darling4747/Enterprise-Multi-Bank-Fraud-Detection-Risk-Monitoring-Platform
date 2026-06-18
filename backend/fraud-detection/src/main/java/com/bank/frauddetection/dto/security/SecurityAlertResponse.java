package com.bank.frauddetection.dto.security;

import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.SecurityIncidentStatus;
import com.bank.frauddetection.enums.SecurityIncidentType;
import java.time.Instant;

public record SecurityAlertResponse(
        Long id,
        SecurityIncidentType eventType,
        RiskLevel severity,
        String description,
        SecurityIncidentStatus status,
        Long userId,
        String username,
        Long bankId,
        String bankCode,
        Long branchId,
        String branchCode,
        Instant createdAt,
        Instant resolvedAt
) {
}
