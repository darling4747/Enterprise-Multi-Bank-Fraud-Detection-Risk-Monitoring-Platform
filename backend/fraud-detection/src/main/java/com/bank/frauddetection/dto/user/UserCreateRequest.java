package com.bank.frauddetection.dto.user;

import com.bank.frauddetection.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Email String email,
        @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 140) String fullName,
        Long bankId,
        Long branchId,
        @Size(max = 80) String employeeId,
        @NotEmpty Set<RoleType> roles,
        boolean enabled
) {
}
