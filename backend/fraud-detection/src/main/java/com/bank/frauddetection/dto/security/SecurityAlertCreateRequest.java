package com.bank.frauddetection.dto.security;

import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.SecurityIncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SecurityAlertCreateRequest(
        @NotNull SecurityIncidentType eventType,
        @NotNull RiskLevel severity,
        @NotBlank String description,
        Long userId,
        Long bankId,
        Long branchId
) {
}
