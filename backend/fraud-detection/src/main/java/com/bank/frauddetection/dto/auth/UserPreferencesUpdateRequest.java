package com.bank.frauddetection.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UserPreferencesUpdateRequest(
        boolean criticalAlertEmails,
        boolean dailySummaryReport,
        @Min(15) @Max(60) int sessionTimeoutMinutes
) {
}
