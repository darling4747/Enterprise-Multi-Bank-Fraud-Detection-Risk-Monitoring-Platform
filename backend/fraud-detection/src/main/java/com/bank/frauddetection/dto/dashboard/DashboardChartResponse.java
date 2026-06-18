package com.bank.frauddetection.dto.dashboard;

import java.util.Map;

public record DashboardChartResponse(
        Map<String, Long> riskDistribution,
        Map<String, Long> transactionStatusDistribution,
        Map<String, Long> alertStatusDistribution
) {
}
