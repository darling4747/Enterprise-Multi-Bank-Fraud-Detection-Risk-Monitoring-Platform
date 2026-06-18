package com.bank.frauddetection.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailySummaryReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryReportScheduler.class);

    private final DailySummaryReportService dailySummaryReportService;

    public DailySummaryReportScheduler(DailySummaryReportService dailySummaryReportService) {
        this.dailySummaryReportService = dailySummaryReportService;
    }

    @Scheduled(cron = "${app.reports.daily-summary-cron:0 0 8 * * *}", zone = "${app.reports.time-zone:Asia/Kolkata}")
    public void sendDailySummaryReport() {
        log.info("DAILY_SUMMARY_SCHEDULE_TRIGGERED");
        dailySummaryReportService.sendDailySummaryToEnabledSuperAdmins();
    }
}
