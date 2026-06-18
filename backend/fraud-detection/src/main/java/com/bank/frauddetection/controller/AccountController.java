package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.account.AccountCreateRequest;
import com.bank.frauddetection.dto.account.AccountResponse;
import com.bank.frauddetection.enums.AccountStatus;
import com.bank.frauddetection.service.AccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> list(Principal principal) {
        return accountService.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest request, Principal principal) {
        return accountService.create(request, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(@PathVariable Long id, @RequestParam AccountStatus status, Principal principal) {
        return accountService.updateStatus(id, status, principal.getName());
    }

    @PostMapping("/{id}/approve-branch-operation")
    public AccountResponse approveBranchOperation(@PathVariable Long id, Principal principal) {
        return accountService.approveBranchOperation(id, principal.getName());
    }
}
