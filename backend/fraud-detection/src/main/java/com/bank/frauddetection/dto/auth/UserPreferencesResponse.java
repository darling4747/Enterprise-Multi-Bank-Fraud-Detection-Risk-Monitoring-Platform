package com.bank.frauddetection.dto.auth;

public record UserPreferencesResponse(
        boolean criticalAlertEmails,
        boolean dailySummaryReport,
        int sessionTimeoutMinutes,
        boolean mfaEnabled
) {
}
