package com.bank.frauddetection.dto.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchCreateRequest(
        Long bankId,
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 40) String ifscCode,
        @Size(max = 160) String city,
        @Size(max = 160) String state,
        @Size(max = 240) String address,
        @Size(max = 160) String managerName
) {
}
