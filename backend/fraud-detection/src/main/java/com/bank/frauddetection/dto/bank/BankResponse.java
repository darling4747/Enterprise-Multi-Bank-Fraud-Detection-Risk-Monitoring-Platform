package com.bank.frauddetection.dto.bank;

import com.bank.frauddetection.enums.BankStatus;
import java.time.Instant;

public record BankResponse(
        Long id,
        String code,
        String name,
        String headOffice,
        String headOfficeCity,
        String headOfficeState,
        String headOfficeCountry,
        String swiftCode,
        String licenseNumber,
        String contactEmail,
        String contactPhone,
        BankStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
