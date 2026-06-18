package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.beneficiary.BeneficiaryCreateRequest;
import com.bank.frauddetection.dto.beneficiary.BeneficiaryResponse;
import com.bank.frauddetection.service.BeneficiaryService;
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
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public List<BeneficiaryResponse> list(Principal principal) {
        return beneficiaryService.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse create(@Valid @RequestBody BeneficiaryCreateRequest request, Principal principal) {
        return beneficiaryService.create(request, principal.getName());
    }

    @PatchMapping("/{id}/mark-used")
    public BeneficiaryResponse markUsed(@PathVariable Long id, Principal principal) {
        return beneficiaryService.markUsed(id, principal.getName());
    }
}
