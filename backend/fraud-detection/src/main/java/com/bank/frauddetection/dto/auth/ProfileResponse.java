package com.bank.frauddetection.dto.auth;

public record ProfileResponse(
        String fullName,
        String email,
        String profilePhotoDataUrl
) {
}
