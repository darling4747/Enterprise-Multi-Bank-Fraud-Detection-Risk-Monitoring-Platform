package com.bank.frauddetection.service;

import com.bank.frauddetection.entity.Alert;
import com.bank.frauddetection.entity.FraudCase;
import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AlertStatus;
import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.NotificationType;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.TransactionStatus;
import com.bank.frauddetection.repository.AlertRepository;
import com.bank.frauddetection.repository.FraudCaseRepository;
import com.bank.frauddetection.repository.TransactionRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailySummaryReportService {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryReportService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;
    private final ZoneId reportZone;

    public DailySummaryReportService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            AlertRepository alertRepository,
            FraudCaseRepository fraudCaseRepository,
            EmailNotificationService emailNotificationService,
            NotificationService notificationService,
            @Value("${app.reports.time-zone:Asia/Kolkata}") String reportTimeZone
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.emailNotificationService = emailNotificationService;
        this.notificationService = notificationService;
        this.reportZone = ZoneId.of(reportTimeZone);
    }

    @Transactional(readOnly = true)
    public int sendDailySummaryToEnabledSuperAdmins() {
        LocalDate reportDate = LocalDate.now(reportZone);
        Instant start = reportDate.atStartOfDay(reportZone).toInstant();
        Instant end = reportDate.plusDays(1).atStartOfDay(reportZone).toInstant();

        List<User> recipients = userRepository.findAll().stream()
                .filter(User::isDailySummaryReport)
                .filter(this::isPlatform)
                .toList();
        if (recipients.isEmpty()) {
            log.info("DAILY_SUMMARY_EMAIL_SKIPPED date={} reason=no_enabled_superadmin_recipients", reportDate);
            return 0;
        }

        Summary summary = buildSummary(start, end);
        String subject = "[SecureBank] Daily fraud summary - " + reportDate;
        String body = buildEmailBody(reportDate, summary);

        recipients.forEach(recipient -> {
            emailNotificationService.send(recipient, subject, body);
            notificationService.notify(recipient, NotificationType.DAILY_REPORT_READY, "Daily fraud summary generated for " + reportDate);
        });
        log.info("DAILY_SUMMARY_EMAIL_DISPATCHED date={} recipients={} flaggedTransactions={} openAlerts={}",
                reportDate,
                recipients.size(),
                summary.flaggedTransactions(),
                summary.openAlerts()
        );
        return recipients.size();
    }

    private Summary buildSummary(Instant start, Instant end) {
        List<Transaction> todaysTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> !transaction.getCreatedAt().isBefore(start) && transaction.getCreatedAt().isBefore(end))
                .toList();
        List<FraudCase> todaysCases = fraudCaseRepository.findAll().stream()
                .filter(fraudCase -> !fraudCase.getCreatedAt().isBefore(start) && fraudCase.getCreatedAt().isBefore(end))
                .toList();
        List<Alert> todaysAlerts = alertRepository.findAll().stream()
                .filter(alert -> !alert.getCreatedAt().isBefore(start) && alert.getCreatedAt().isBefore(end))
                .toList();

        long approved = todaysTransactions.stream().filter(transaction -> transaction.getFraudDecision() == FraudDecision.APPROVE).count();
        long review = todaysTransactions.stream().filter(transaction -> transaction.getFraudDecision() == FraudDecision.REVIEW).count();
        long blocked = todaysTransactions.stream().filter(transaction -> transaction.getFraudDecision() == FraudDecision.BLOCK).count();
        long highOrCritical = todaysTransactions.stream()
                .filter(transaction -> transaction.getRiskLevel() == RiskLevel.HIGH || transaction.getRiskLevel() == RiskLevel.CRITICAL)
                .count();
        long openAlerts = alertRepository.countByStatus(AlertStatus.OPEN);
        BigDecimal totalAmount = todaysTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Transaction> topFlagged = todaysTransactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.REVIEW || transaction.getStatus() == TransactionStatus.BLOCKED)
                .sorted(Comparator.comparingInt(Transaction::getRiskScore).reversed())
                .limit(5)
                .toList();

        return new Summary(
                todaysTransactions.size(),
                approved,
                review,
                blocked,
                highOrCritical,
                todaysAlerts.size(),
                openAlerts,
                todaysCases.size(),
                totalAmount,
                topFlagged
        );
    }

    private String buildEmailBody(LocalDate reportDate, Summary summary) {
        String topFlagged = summary.topFlagged().isEmpty()
                ? "No flagged transactions today."
                : summary.topFlagged().stream()
                        .map(transaction -> "- %s | %s %s | risk=%s | decision=%s | status=%s".formatted(
                                transaction.getReference(),
                                transaction.getCurrency(),
                                transaction.getAmount(),
                                transaction.getRiskScore(),
                                transaction.getFraudDecision(),
                                transaction.getStatus()
                        ))
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("No flagged transactions today.");

        return """
                Daily Fraud Detection Summary
                Date: %s

                Transactions Processed: %s
                Total Amount Monitored: %s
                Approved: %s
                Review: %s
                Blocked: %s
                High/Critical Risk Transactions: %s
                New Fraud Alerts: %s
                Open Alerts: %s
                New Fraud Cases: %s

                Top Flagged Transactions:
                %s

                This report was generated automatically because Daily Summary Report is enabled in Settings.
                """.formatted(
                reportDate,
                summary.totalTransactions(),
                summary.totalAmount(),
                summary.approved(),
                summary.review(),
                summary.blocked(),
                summary.highOrCritical(),
                summary.newAlerts(),
                summary.openAlerts(),
                summary.newFraudCases(),
                topFlagged
        );
    }

    private boolean isPlatform(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.PLATFORM_ADMIN || role.getName() == RoleType.SUPER_ADMIN);
    }

    private record Summary(
            long totalTransactions,
            long approved,
            long review,
            long blocked,
            long highOrCritical,
            long newAlerts,
            long openAlerts,
            long newFraudCases,
            BigDecimal totalAmount,
            List<Transaction> topFlagged
    ) {
        long flaggedTransactions() {
            return review + blocked;
        }
    }
}
