package com.bank.frauddetection.dto.dashboard;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalTransactions,
        long pendingTransactions,
        long reviewTransactions,
        long blockedTransactions,
        long openAlerts,
        long activeFraudCases,
        long highRiskTransactions,
        long criticalRiskTransactions,
        BigDecimal last24hVolume
) {
}
