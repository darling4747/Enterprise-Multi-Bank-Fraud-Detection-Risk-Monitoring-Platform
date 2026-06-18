package com.bank.frauddetection.dto.bank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record BankCreateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 160) String headOffice,
        @Size(max = 120) String headOfficeCity,
        @Size(max = 120) String headOfficeState,
        @Size(max = 120) String headOfficeCountry,
        @Size(max = 40) String swiftCode,
        @Size(max = 80) String licenseNumber,
        @Email @Size(max = 160) String contactEmail,
        @Size(max = 40) String contactPhone
) {
}
