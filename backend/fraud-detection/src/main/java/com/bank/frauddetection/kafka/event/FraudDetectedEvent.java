package com.bank.frauddetection.kafka.event;

import java.time.Instant;

public record FraudDetectedEvent(
        Long fraudCaseId,
        Long transactionId,
        String transactionReference,
        int riskScore,
        String riskLevel,
        String decision,
        Instant detectedAt
) {
}
