package com.bank.frauddetection.dto.fraud;

import com.bank.frauddetection.enums.CasePriority;
import com.bank.frauddetection.enums.FraudCaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FraudCaseUpdateRequest(
        @NotNull FraudCaseStatus status,
        @Size(max = 4000) String investigationNotes,
        Long assignedToUserId,
        CasePriority priority
) {
}
