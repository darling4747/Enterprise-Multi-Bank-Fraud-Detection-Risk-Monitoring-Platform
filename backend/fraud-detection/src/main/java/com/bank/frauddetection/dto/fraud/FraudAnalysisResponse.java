package com.bank.frauddetection.dto.fraud;

import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.RiskLevel;
import java.util.List;

public record FraudAnalysisResponse(
        Long transactionId,
        String transactionReference,
        int riskScore,
        RiskLevel riskLevel,
        FraudDecision decision,
        List<String> triggeredRules,
        double mlProbability
) {
}
