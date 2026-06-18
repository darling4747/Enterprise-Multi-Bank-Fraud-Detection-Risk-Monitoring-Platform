package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.audit.AuditLogResponse;
import com.bank.frauddetection.service.AuditService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLogResponse> list(@RequestParam(required = false) Long bankId, Principal principal) {
        return auditService.listLatest(principal.getName(), bankId);
    }
}
