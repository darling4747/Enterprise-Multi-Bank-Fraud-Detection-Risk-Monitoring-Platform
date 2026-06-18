package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.bank.BankCreateRequest;
import com.bank.frauddetection.dto.bank.BankResponse;
import com.bank.frauddetection.service.BankService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    public List<BankResponse> listBanks() {
        return bankService.listBanks();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankResponse createBank(@Valid @RequestBody BankCreateRequest request, Principal principal) {
        return bankService.createBank(request, principal.getName());
    }

    @PatchMapping("/{id}/disable")
    public BankResponse disableBank(@PathVariable Long id, Principal principal) {
        return bankService.disableBank(id, principal.getName());
    }
}
