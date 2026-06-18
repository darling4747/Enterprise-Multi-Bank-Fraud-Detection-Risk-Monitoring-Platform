package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.transaction.ExternalTransactionIngestRequest;
import com.bank.frauddetection.dto.transaction.TransactionRequest;
import com.bank.frauddetection.dto.transaction.TransactionResponse;
import com.bank.frauddetection.dto.transaction.TransactionSummaryResponse;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.TransactionStatus;
import com.bank.frauddetection.service.TransactionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request, Principal principal) {
        return transactionService.create(request, principal.getName());
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse ingest(@Valid @RequestBody ExternalTransactionIngestRequest request, Principal principal) {
        return transactionService.ingest(request, principal.getName());
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id, Principal principal) {
        return transactionService.get(id, principal.getName());
    }

    @GetMapping
    public List<TransactionSummaryResponse> search(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            Principal principal
    ) {
        return transactionService.search(customerId, status, riskLevel, bankId, branchId, fromDate, toDate, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public TransactionResponse updateStatus(@PathVariable Long id, @RequestParam TransactionStatus status, Principal principal) {
        return transactionService.updateStatus(id, status, principal.getName());
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request, Principal principal) {
        return transactionService.update(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        transactionService.delete(id, principal.getName());
    }
}
