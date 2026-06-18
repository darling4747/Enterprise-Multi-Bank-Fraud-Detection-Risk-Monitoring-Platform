package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.alert.AlertResponse;
import com.bank.frauddetection.enums.AlertStatus;
import com.bank.frauddetection.service.AlertService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status, Principal principal) {
        return alertService.list(status, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public AlertResponse updateStatus(
            @PathVariable Long id,
            @RequestParam AlertStatus status,
            @RequestParam(required = false) String assignedTo,
            Principal principal
    ) {
        return alertService.updateStatus(id, status, assignedTo, principal.getName());
    }
}
