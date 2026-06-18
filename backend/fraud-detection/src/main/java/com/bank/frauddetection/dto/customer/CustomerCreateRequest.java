package com.bank.frauddetection.dto.customer;

import com.bank.frauddetection.enums.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerCreateRequest(
        @NotBlank String customerId,
        Long bankId,
        Long branchId,
        @NotNull CustomerType customerType,
        @NotBlank String fullName,
        String email,
        String phone
) {
}
