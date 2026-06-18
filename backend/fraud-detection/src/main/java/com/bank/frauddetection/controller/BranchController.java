package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.branch.BranchCreateRequest;
import com.bank.frauddetection.dto.branch.BranchResponse;
import com.bank.frauddetection.service.BranchService;
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
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public List<BranchResponse> listBranches(@RequestParam(required = false) Long bankId, Principal principal) {
        return branchService.listBranches(principal.getName(), bankId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse createBranch(@Valid @RequestBody BranchCreateRequest request, Principal principal) {
        return branchService.createBranch(request, principal.getName());
    }

    @PatchMapping("/{id}/disable")
    public BranchResponse disableBranch(@PathVariable Long id, Principal principal) {
        return branchService.disableBranch(id, principal.getName());
    }
}
