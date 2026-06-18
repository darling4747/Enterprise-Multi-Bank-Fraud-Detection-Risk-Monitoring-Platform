package com.bank.frauddetection.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 140) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        String profilePhotoDataUrl
) {
}
