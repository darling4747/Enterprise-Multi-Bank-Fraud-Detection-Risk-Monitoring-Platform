package com.bank.frauddetection.dto.auth;

public record MfaSetupResponse(
        String secret,
        String otpAuthUri,
        String qrCodeUrl
) {
}
