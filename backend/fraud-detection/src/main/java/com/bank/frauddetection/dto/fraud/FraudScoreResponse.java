package com.bank.frauddetection.dto.fraud;

import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.RiskLevel;

public record FraudScoreResponse(
        Long transactionId,
        String transactionReference,
        int riskScore,
        RiskLevel riskLevel,
        FraudDecision decision,
        String summary
) {
}
