package com.bank.frauddetection.dto.auth;

import com.bank.frauddetection.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 140) String fullName,
        Set<RoleType> roles
) {
}
