package com.bank.frauddetection.dto.account;

import com.bank.frauddetection.enums.AccountStatus;
import com.bank.frauddetection.enums.AccountType;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String customerId,
        String customerName,
        Long bankId,
        String bankCode,
        Long branchId,
        String branchCode,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
