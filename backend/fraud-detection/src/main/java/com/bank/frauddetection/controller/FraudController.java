package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.fraud.FraudAnalysisResponse;
import com.bank.frauddetection.dto.fraud.FraudCaseUpdateRequest;
import com.bank.frauddetection.dto.fraud.FraudCaseResponse;
import com.bank.frauddetection.dto.fraud.FraudScoreResponse;
import com.bank.frauddetection.enums.FraudCaseStatus;
import com.bank.frauddetection.service.FraudDetectionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    public FraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/analyze/{transactionId}")
    public FraudAnalysisResponse analyze(@PathVariable Long transactionId, Principal principal) {
        return fraudDetectionService.analyze(transactionId, principal.getName());
    }

    @GetMapping("/score/{transactionId}")
    public FraudScoreResponse score(@PathVariable Long transactionId, Principal principal) {
        return fraudDetectionService.score(transactionId, principal.getName());
    }

    @GetMapping("/cases")
    public List<FraudCaseResponse> cases(Principal principal) {
        return fraudDetectionService.listCases(principal.getName());
    }

    @GetMapping("/cases/{id}")
    public FraudCaseResponse caseDetails(@PathVariable Long id, Principal principal) {
        return fraudDetectionService.getCase(id, principal.getName());
    }

    @PatchMapping("/cases/{id}/status")
    public FraudCaseResponse updateCaseStatus(@PathVariable Long id, @RequestParam FraudCaseStatus status, Principal principal) {
        return fraudDetectionService.updateCaseStatus(id, status, principal.getName());
    }

    @PatchMapping("/cases/{id}/investigation")
    public FraudCaseResponse submitInvestigation(
            @PathVariable Long id,
            @Valid @RequestBody FraudCaseUpdateRequest request,
            Principal principal
    ) {
        return fraudDetectionService.updateCaseInvestigation(id, request, principal.getName());
    }
}
