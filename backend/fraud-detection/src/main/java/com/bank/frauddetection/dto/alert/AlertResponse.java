package com.bank.frauddetection.dto.alert;

import com.bank.frauddetection.enums.AlertStatus;
import com.bank.frauddetection.enums.RiskLevel;
import java.time.Instant;

public record AlertResponse(
        Long id,
        Long fraudCaseId,
        Long transactionId,
        String transactionReference,
        String title,
        String message,
        RiskLevel severity,
        AlertStatus status,
        String assignedTo,
        Instant createdAt,
        Instant resolvedAt
) {
}
