package com.bank.frauddetection.dto.customer;

import com.bank.frauddetection.enums.CustomerStatus;
import com.bank.frauddetection.enums.CustomerType;
import java.time.Instant;

public record CustomerResponse(
        Long id,
        String customerId,
        Long bankId,
        String bankCode,
        Long branchId,
        String branchCode,
        CustomerType customerType,
        String fullName,
        String email,
        String phone,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
