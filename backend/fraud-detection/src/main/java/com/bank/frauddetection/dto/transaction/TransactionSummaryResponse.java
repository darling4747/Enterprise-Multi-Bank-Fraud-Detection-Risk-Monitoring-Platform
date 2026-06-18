package com.bank.frauddetection.dto.transaction;

import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionSummaryResponse(
        Long id,
        String reference,
        String customerId,
        Long bankId,
        Long branchId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        RiskLevel riskLevel,
        int riskScore,
        Instant createdAt
) {
}
