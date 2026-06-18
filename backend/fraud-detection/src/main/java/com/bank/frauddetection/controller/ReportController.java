package com.bank.frauddetection.controller;

import com.bank.frauddetection.service.DailySummaryReportService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final DailySummaryReportService dailySummaryReportService;

    public ReportController(DailySummaryReportService dailySummaryReportService) {
        this.dailySummaryReportService = dailySummaryReportService;
    }

    @PostMapping("/daily-summary/send-now")
    public Map<String, Object> sendDailySummaryNow() {
        int recipientCount = dailySummaryReportService.sendDailySummaryToEnabledSuperAdmins();
        return Map.of(
                "status", "SENT",
                "recipientCount", recipientCount
        );
    }
}
