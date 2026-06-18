package com.bank.frauddetection.dto.account;

import com.bank.frauddetection.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountCreateRequest(
        @NotBlank String customerId,
        @NotBlank String accountNumber,
        @NotNull AccountType accountType,
        @NotNull @DecimalMin("0.00") BigDecimal balance,
        String currency,
        Long branchId
) {
}
