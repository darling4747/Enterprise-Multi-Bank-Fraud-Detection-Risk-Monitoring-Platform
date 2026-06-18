package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.security.SecurityAlertCreateRequest;
import com.bank.frauddetection.dto.security.SecurityAlertResponse;
import com.bank.frauddetection.enums.SecurityIncidentStatus;
import com.bank.frauddetection.service.SecurityIncidentService;
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
@RequestMapping("/api/security-incidents")
public class SecurityIncidentController {

    private final SecurityIncidentService securityIncidentService;

    public SecurityIncidentController(SecurityIncidentService securityIncidentService) {
        this.securityIncidentService = securityIncidentService;
    }

    @GetMapping
    public List<SecurityAlertResponse> list(Principal principal) {
        return securityIncidentService.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecurityAlertResponse create(@Valid @RequestBody SecurityAlertCreateRequest request, Principal principal) {
        return securityIncidentService.create(request, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public SecurityAlertResponse updateStatus(@PathVariable Long id, @RequestParam SecurityIncidentStatus status, Principal principal) {
        return securityIncidentService.updateStatus(id, status, principal.getName());
    }
}
