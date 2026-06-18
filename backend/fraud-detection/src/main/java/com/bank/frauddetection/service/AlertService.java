package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.alert.AlertResponse;
import com.bank.frauddetection.entity.Alert;
import com.bank.frauddetection.entity.FraudCase;
import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AlertStatus;
import com.bank.frauddetection.enums.NotificationType;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.AlertRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    public AlertService(
            AlertRepository alertRepository,
            UserRepository userRepository,
            EmailNotificationService emailNotificationService,
            NotificationService notificationService
    ) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Alert createAlert(FraudCase fraudCase) {
        Alert alert = new Alert();
        alert.setFraudCase(fraudCase);
        alert.setTransaction(fraudCase.getTransaction());
        alert.setSeverity(fraudCase.getRiskLevel());
        alert.setTitle("Suspicious transaction " + fraudCase.getTransaction().getReference());
        alert.setMessage(fraudCase.getReason());
        Alert saved = alertRepository.save(alert);
        log.info(
                "FRAUD_ALERT_CREATED alertId={} caseId={} transactionId={} reference={} severity={} status={}",
                saved.getId(),
                fraudCase.getId(),
                fraudCase.getTransaction().getId(),
                fraudCase.getTransaction().getReference(),
                saved.getSeverity(),
                saved.getStatus()
        );
        notificationService.notifyPlatformUsers(NotificationType.NEW_FRAUD_ALERT, saved.getSeverity() + " fraud alert created for " + saved.getTransaction().getReference());
        sendCriticalAlertEmails(saved);
        return saved;
    }

    private void sendCriticalAlertEmails(Alert alert) {
        if (alert.getSeverity() != RiskLevel.HIGH && alert.getSeverity() != RiskLevel.CRITICAL) {
            return;
        }
        List<User> recipients = userRepository.findAll().stream()
                .filter(User::isCriticalAlertEmails)
                .filter(this::isPlatform)
                .toList();
        if (recipients.isEmpty()) {
            log.info("CRITICAL_ALERT_EMAIL_SKIPPED alertId={} reason=no_enabled_superadmin_recipients", alert.getId());
            return;
        }

        Transaction transaction = alert.getTransaction();
        String subject = "[SecureBank] " + alert.getSeverity() + " fraud alert: " + transaction.getReference();
        String body = """
                A high-risk fraud alert was created.

                Alert ID: %s
                Transaction: %s
                Decision: %s
                Risk Level: %s
                Risk Score: %s
                Amount: %s %s
                Channel: %s
                Bank ID: %s
                Branch ID: %s

                Triggered Rules:
                %s

                Open the fraud dashboard to review and assign investigation.
                """.formatted(
                alert.getId(),
                transaction.getReference(),
                transaction.getFraudDecision(),
                transaction.getRiskLevel(),
                transaction.getRiskScore(),
                transaction.getCurrency(),
                transaction.getAmount(),
                transaction.getChannel(),
                transaction.getBank() == null ? "-" : transaction.getBank().getId(),
                transaction.getBranch() == null ? "-" : transaction.getBranch().getId(),
                alert.getMessage()
        );

        recipients.forEach(recipient -> emailNotificationService.send(recipient, subject, body));
        log.info("CRITICAL_ALERT_EMAIL_DISPATCHED alertId={} recipients={}", alert.getId(), recipients.size());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(AlertStatus status) {
        List<Alert> alerts = status == null
                ? alertRepository.findTop10ByOrderByCreatedAtDesc()
                : alertRepository.findByStatusOrderByCreatedAtDesc(status);
        return alerts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(AlertStatus status, String actorUsername) {
        TenantScope scope = resolveScope(findUser(actorUsername));
        List<Alert> alerts = alertRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(alert -> status == null || alert.getStatus() == status)
                .filter(alert -> scope.bankId() == null || alert.getTransaction().getBank() != null && Objects.equals(alert.getTransaction().getBank().getId(), scope.bankId()))
                .filter(alert -> scope.branchId() == null || alert.getTransaction().getBranch() != null && Objects.equals(alert.getTransaction().getBranch().getId(), scope.branchId()))
                .toList();
        return alerts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertResponse updateStatus(Long id, AlertStatus status, String assignedTo) {
        return updateStatus(id, status, assignedTo, null);
    }

    @Transactional
    public AlertResponse updateStatus(Long id, AlertStatus status, String assignedTo, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
        ensureCanAccess(actor, alert);
        alert.setStatus(status);
        alert.setAssignedTo(assignedTo);
        if (status == AlertStatus.RESOLVED || status == AlertStatus.FALSE_POSITIVE) {
            alert.setResolvedAt(Instant.now());
        }
        Alert saved = alertRepository.save(alert);
        log.info("FRAUD_ALERT_STATUS_UPDATED alertId={} status={} assignedTo={}", saved.getId(), saved.getStatus(), saved.getAssignedTo());
        return toResponse(saved);
    }

    @Transactional
    public void syncCaseStatus(Long fraudCaseId, AlertStatus status, String assignedTo) {
        Instant resolvedAt = status == AlertStatus.RESOLVED || status == AlertStatus.FALSE_POSITIVE ? Instant.now() : null;
        for (Alert alert : alertRepository.findByFraudCaseId(fraudCaseId)) {
            alert.setStatus(status);
            alert.setAssignedTo(assignedTo);
            alert.setResolvedAt(resolvedAt);
            Alert saved = alertRepository.save(alert);
            log.info("FRAUD_ALERT_SYNCED_WITH_CASE alertId={} caseId={} status={} assignedTo={}", saved.getId(), fraudCaseId, status, assignedTo);
        }
    }

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getFraudCase().getId(),
                alert.getTransaction().getId(),
                alert.getTransaction().getReference(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getAssignedTo(),
                alert.getCreatedAt(),
                alert.getResolvedAt()
        );
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private TenantScope resolveScope(User actor) {
        if (isPlatform(actor)) {
            return new TenantScope(null, null);
        }
        if (actor.getBank() == null) {
            throw new BusinessException("User is not assigned to a bank");
        }
        return new TenantScope(
                actor.getBank().getId(),
                actor.getBranch() == null ? null : actor.getBranch().getId()
        );
    }

    private void ensureCanAccess(User actor, Alert alert) {
        if (actor == null || isPlatform(actor)) {
            return;
        }
        if (actor.getBank() == null || alert.getTransaction().getBank() == null
                || !actor.getBank().getId().equals(alert.getTransaction().getBank().getId())) {
            throw new BusinessException("Cannot access another bank's alert");
        }
        if (actor.getBranch() != null && (alert.getTransaction().getBranch() == null
                || !actor.getBranch().getId().equals(alert.getTransaction().getBranch().getId()))) {
            throw new BusinessException("Cannot access another branch's alert");
        }
    }

    private boolean isPlatform(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.PLATFORM_ADMIN || role.getName() == RoleType.SUPER_ADMIN);
    }

    private record TenantScope(Long bankId, Long branchId) {
    }
}
