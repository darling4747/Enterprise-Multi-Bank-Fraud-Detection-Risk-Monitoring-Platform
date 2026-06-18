package com.bank.frauddetection.dto.beneficiary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BeneficiaryCreateRequest(
        @NotBlank String accountNumber,
        @NotBlank String beneficiaryAccount,
        @Min(0) @Max(100) Integer trustScore
) {
}
