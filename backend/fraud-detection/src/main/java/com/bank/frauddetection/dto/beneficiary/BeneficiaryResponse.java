package com.bank.frauddetection.dto.beneficiary;

import java.time.Instant;

public record BeneficiaryResponse(
        Long id,
        Long accountId,
        String accountNumber,
        String beneficiaryAccount,
        int trustScore,
        int usageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
