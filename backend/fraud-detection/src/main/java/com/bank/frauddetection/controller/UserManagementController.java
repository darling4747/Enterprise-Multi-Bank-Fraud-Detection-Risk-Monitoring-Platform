package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.user.UserCreateRequest;
import com.bank.frauddetection.dto.user.TemporaryCredentialResponse;
import com.bank.frauddetection.dto.user.UserResponse;
import com.bank.frauddetection.dto.user.UserRoleUpdateRequest;
import com.bank.frauddetection.service.UserManagementService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserResponse> listUsers(Principal principal) {
        return userManagementService.listUsers(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request, Principal principal) {
        return userManagementService.createUser(request, principal.getName());
    }

    @PostMapping("/bank-admins")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporaryCredentialResponse createBankAdmin(@Valid @RequestBody UserCreateRequest request, Principal principal) {
        return userManagementService.createBankAdmin(request, principal.getName());
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporaryCredentialResponse createEmployee(@Valid @RequestBody UserCreateRequest request, Principal principal) {
        return userManagementService.createEmployee(request, principal.getName());
    }

    @PostMapping("/{id}/reset-password")
    public TemporaryCredentialResponse resetPassword(@PathVariable Long id, Principal principal) {
        return userManagementService.resetPasswordByIssuer(id, principal.getName());
    }

    @PostMapping("/{id}/unlock")
    public UserResponse unlockUser(@PathVariable Long id, Principal principal) {
        return userManagementService.unlockUser(id, principal.getName());
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UserRoleUpdateRequest request, Principal principal) {
        return userManagementService.updateRoles(id, request, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public UserResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled, Principal principal) {
        return userManagementService.setEnabled(id, enabled, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, Principal principal) {
        userManagementService.deleteUser(id, principal.getName());
    }
}
