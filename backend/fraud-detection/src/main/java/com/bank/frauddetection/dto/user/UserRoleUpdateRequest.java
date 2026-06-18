package com.bank.frauddetection.dto.user;

import com.bank.frauddetection.enums.RoleType;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserRoleUpdateRequest(
        @NotEmpty Set<RoleType> roles
) {
}
