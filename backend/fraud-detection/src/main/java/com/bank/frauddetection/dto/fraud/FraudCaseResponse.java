package com.bank.frauddetection.dto.fraud;

import com.bank.frauddetection.enums.CasePriority;
import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.FraudCaseStatus;
import com.bank.frauddetection.enums.RiskLevel;
import java.time.Instant;

public record FraudCaseResponse(
        Long id,
        Long transactionId,
        String transactionReference,
        int riskScore,
        RiskLevel riskLevel,
        FraudDecision decision,
        FraudCaseStatus status,
        CasePriority priority,
        String reason,
        String investigationNotes,
        String reviewedBy,
        Instant reviewedAt,
        Long assignedToUserId,
        String assignedToUsername,
        Long assignedByUserId,
        String assignedByUsername,
        Instant assignedAt,
        Instant createdAt,
        Instant closedAt
) {
}
