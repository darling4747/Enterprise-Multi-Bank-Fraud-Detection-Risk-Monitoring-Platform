package com.bank.frauddetection.util;

import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.RiskLevel;

public final class RiskCalculator {

    private RiskCalculator() {
    }

    public static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    public static RiskLevel levelFor(int score) {
        int normalized = clamp(score);
        if (normalized >= 71) {
            return RiskLevel.HIGH;
        }
        if (normalized >= 31) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public static FraudDecision decisionFor(int score) {
        int normalized = clamp(score);
        if (normalized >= 71) {
            return FraudDecision.BLOCK;
        }
        if (normalized >= 31) {
            return FraudDecision.REVIEW;
        }
        return FraudDecision.APPROVE;
    }
}
