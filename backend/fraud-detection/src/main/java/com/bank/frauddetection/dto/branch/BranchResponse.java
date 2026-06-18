package com.bank.frauddetection.dto.branch;

import com.bank.frauddetection.enums.BranchStatus;
import java.time.Instant;

public record BranchResponse(
        Long id,
        Long bankId,
        String bankCode,
        String code,
        String name,
        String ifscCode,
        String city,
        String state,
        String address,
        String managerName,
        BranchStatus status,
        Instant createdAt
) {
}
