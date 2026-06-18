package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.dashboard.DashboardChartResponse;
import com.bank.frauddetection.dto.dashboard.DashboardStatsResponse;
import com.bank.frauddetection.service.DashboardService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse stats(Principal principal) {
        return dashboardService.stats(principal.getName());
    }

    @GetMapping("/charts")
    public DashboardChartResponse charts(Principal principal) {
        return dashboardService.charts(principal.getName());
    }
}
