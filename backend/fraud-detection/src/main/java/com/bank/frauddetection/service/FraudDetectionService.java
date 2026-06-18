package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.fraud.FraudAnalysisResponse;
import com.bank.frauddetection.dto.fraud.FraudCaseUpdateRequest;
import com.bank.frauddetection.dto.fraud.FraudCaseResponse;
import com.bank.frauddetection.dto.fraud.FraudScoreResponse;
import com.bank.frauddetection.entity.FraudCase;
import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.AlertStatus;
import com.bank.frauddetection.enums.CasePriority;
import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.FraudCaseStatus;
import com.bank.frauddetection.enums.NotificationType;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.TransactionStatus;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.kafka.event.FraudDetectedEvent;
import com.bank.frauddetection.kafka.producer.TransactionProducer;
import com.bank.frauddetection.repository.FraudCaseRepository;
import com.bank.frauddetection.repository.TransactionRepository;
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
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final TransactionRepository transactionRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final RuleEngineService ruleEngineService;
    private final MlServiceClient mlServiceClient;
    private final AlertService alertService;
    private final TransactionProducer transactionProducer;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FraudDetectionService(
            TransactionRepository transactionRepository,
            FraudCaseRepository fraudCaseRepository,
            RuleEngineService ruleEngineService,
            MlServiceClient mlServiceClient,
            AlertService alertService,
            TransactionProducer transactionProducer,
            AuditService auditService,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.transactionRepository = transactionRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.ruleEngineService = ruleEngineService;
        this.mlServiceClient = mlServiceClient;
        this.alertService = alertService;
        this.transactionProducer = transactionProducer;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FraudAnalysisResponse analyze(Long transactionId) {
        return analyze(transactionId, null);
    }

    @Transactional
    public FraudAnalysisResponse analyze(Long transactionId, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        ensureCanAccess(actor, transaction);
        return analyze(transaction);
    }

    @Transactional
    public FraudAnalysisResponse analyze(Transaction transaction) {
        log.info("FRAUD_ANALYSIS_STARTED reference={} transactionId={}", transaction.getReference(), transaction.getId());
        auditService.log(
                AuditEventType.FRAUD_PREDICTION_REQUESTED,
                null,
                null,
                bankId(transaction),
                "Fraud prediction requested for transaction " + transaction.getReference(),
                AuditStatus.SUCCESS
        );
        log.info("AUDIT_LOG_CREATED event={} reference={}", AuditEventType.FRAUD_PREDICTION_REQUESTED, transaction.getReference());
        double mlProbability = mlServiceClient.predictFraudProbability(transaction);
        log.info("APPLYING_BUSINESS_RULES reference={} mlScore={}", transaction.getReference(), Math.round(mlProbability * 100));
        FraudAnalysisResponse analysis = ruleEngineService.analyze(transaction, mlProbability);
        log.info(
                "FINAL_RISK_SCORE reference={} riskScore={} riskLevel={} decision={} rules={}",
                transaction.getReference(),
                analysis.riskScore(),
                analysis.riskLevel(),
                analysis.decision(),
                String.join(", ", analysis.triggeredRules())
        );
        transaction.setRiskScore(analysis.riskScore());
        transaction.setRiskLevel(analysis.riskLevel());
        transaction.setFraudDecision(analysis.decision());
        transaction.setRiskSummary(String.join(", ", analysis.triggeredRules()));
        transaction.setProcessedAt(Instant.now());

        if (analysis.decision() == FraudDecision.BLOCK) {
            transaction.setStatus(TransactionStatus.BLOCKED);
        } else if (analysis.decision() == FraudDecision.REVIEW) {
            transaction.setStatus(TransactionStatus.REVIEW);
        } else {
            transaction.setStatus(TransactionStatus.APPROVED);
        }
        transactionRepository.save(transaction);
        log.info("TRANSACTION_DECISION_SAVED reference={} status={} decision={}", transaction.getReference(), transaction.getStatus(), transaction.getFraudDecision());

        if (analysis.decision() != FraudDecision.APPROVE) {
            boolean isNewCase = fraudCaseRepository.findByTransactionId(transaction.getId()).isEmpty();
            FraudCase fraudCase = fraudCaseRepository.findByTransactionId(transaction.getId()).orElseGet(FraudCase::new);
            fraudCase.setTransaction(transaction);
            fraudCase.setRiskScore(analysis.riskScore());
            fraudCase.setRiskLevel(analysis.riskLevel());
            fraudCase.setDecision(analysis.decision());
            fraudCase.setPriority(priorityFor(analysis.riskLevel()));
            fraudCase.setReason(String.join(", ", analysis.triggeredRules()));
            fraudCase = fraudCaseRepository.save(fraudCase);
            log.info("FRAUD_CASE_{} caseId={} reference={} riskScore={} decision={}", isNewCase ? "OPENED" : "UPDATED", fraudCase.getId(), transaction.getReference(), analysis.riskScore(), analysis.decision());
            auditService.log(
                    isNewCase ? AuditEventType.FRAUD_CASE_OPENED : AuditEventType.FRAUD_CASE_UPDATED,
                    null,
                    null,
                    bankId(transaction),
                    "Fraud case " + (isNewCase ? "opened" : "updated") + " for transaction " + transaction.getReference(),
                    AuditStatus.SUCCESS
            );
            log.info("AUDIT_LOG_CREATED event={} reference={}", isNewCase ? AuditEventType.FRAUD_CASE_OPENED : AuditEventType.FRAUD_CASE_UPDATED, transaction.getReference());
            alertService.createAlert(fraudCase);
            transactionProducer.publishFraudDetected(new FraudDetectedEvent(
                    fraudCase.getId(),
                    transaction.getId(),
                    transaction.getReference(),
                    analysis.riskScore(),
                    analysis.riskLevel().name(),
                    analysis.decision().name(),
                    Instant.now()
            ));
        }

        return analysis;
    }

    @Transactional(readOnly = true)
    public FraudScoreResponse score(Long transactionId) {
        return score(transactionId, null);
    }

    @Transactional(readOnly = true)
    public FraudScoreResponse score(Long transactionId, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        ensureCanAccess(actor, transaction);
        return new FraudScoreResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getRiskScore(),
                transaction.getRiskLevel(),
                transaction.getFraudDecision(),
                transaction.getRiskSummary()
        );
    }

    @Transactional(readOnly = true)
    public List<FraudCaseResponse> listCases() {
        return fraudCaseRepository.findTop10ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FraudCaseResponse> listCases(String actorUsername) {
        TenantScope scope = resolveScope(findUser(actorUsername));
        return fraudCaseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(fraudCase -> scope.bankId() == null || fraudCase.getTransaction().getBank() != null && Objects.equals(fraudCase.getTransaction().getBank().getId(), scope.bankId()))
                .filter(fraudCase -> scope.branchId() == null || fraudCase.getTransaction().getBranch() != null && Objects.equals(fraudCase.getTransaction().getBranch().getId(), scope.branchId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FraudCaseResponse getCase(Long id, String actorUsername) {
        User actor = findUser(actorUsername);
        FraudCase fraudCase = fraudCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud case not found: " + id));
        ensureCanAccess(actor, fraudCase.getTransaction());
        return toResponse(fraudCase);
    }

    @Transactional
    public FraudCaseResponse updateCaseStatus(Long id, FraudCaseStatus status) {
        return updateCaseStatus(id, status, null);
    }

    @Transactional
    public FraudCaseResponse updateCaseStatus(Long id, FraudCaseStatus status, String actorUsername) {
        return updateCaseInvestigation(id, new FraudCaseUpdateRequest(status, null, null, null), actorUsername);
    }

    @Transactional
    public FraudCaseResponse updateCaseInvestigation(Long id, FraudCaseUpdateRequest request, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        FraudCase fraudCase = fraudCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud case not found: " + id));
        ensureCanAccess(actor, fraudCase.getTransaction());
        FraudCaseStatus status = request.status();
        fraudCase.setStatus(status);
        fraudCase.setInvestigationNotes(request.investigationNotes());
        if (request.priority() != null) {
            fraudCase.setPriority(request.priority());
        }
        if (request.assignedToUserId() != null) {
            User assignee = userRepository.findById(request.assignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found: " + request.assignedToUserId()));
            ensureCanAccess(assignee, fraudCase.getTransaction());
            fraudCase.setAssignedTo(assignee);
            fraudCase.setAssignedBy(actor);
            fraudCase.setAssignedAt(Instant.now());
            notificationService.notify(assignee, NotificationType.CASE_ASSIGNED, "Fraud case CASE-" + fraudCase.getId() + " assigned to you.");
            auditService.log(AuditEventType.CASE_ASSIGNED, actor, assignee, bankId(fraudCase.getTransaction()), "Fraud case assigned", AuditStatus.SUCCESS);
        }
        fraudCase.setReviewedBy(actor == null ? null : actor.getUsername());
        fraudCase.setReviewedAt(Instant.now());
        if (status == FraudCaseStatus.CLOSED || status == FraudCaseStatus.FALSE_POSITIVE || status == FraudCaseStatus.CONFIRMED_FRAUD) {
            fraudCase.setClosedAt(Instant.now());
        } else {
            fraudCase.setClosedAt(null);
        }
        FraudCase saved = fraudCaseRepository.save(fraudCase);
        alertService.syncCaseStatus(saved.getId(), alertStatusFor(status), actor == null ? null : actor.getUsername());
        log.info("FRAUD_CASE_INVESTIGATION_SUBMITTED caseId={} status={} reviewedBy={}", saved.getId(), status, actor == null ? null : actor.getUsername());
        auditService.log(
                AuditEventType.TRANSACTION_REVIEWED,
                actor,
                null,
                bankId(saved.getTransaction()),
                "Fraud case " + saved.getId() + " investigation submitted with status " + status,
                AuditStatus.SUCCESS
        );
        log.info("AUDIT_LOG_CREATED event={} caseId={}", AuditEventType.TRANSACTION_REVIEWED, saved.getId());
        return toResponse(saved);
    }

    private FraudCaseResponse toResponse(FraudCase fraudCase) {
        return new FraudCaseResponse(
                fraudCase.getId(),
                fraudCase.getTransaction().getId(),
                fraudCase.getTransaction().getReference(),
                fraudCase.getRiskScore(),
                fraudCase.getRiskLevel(),
                fraudCase.getDecision(),
                fraudCase.getStatus(),
                fraudCase.getPriority(),
                fraudCase.getReason(),
                fraudCase.getInvestigationNotes(),
                fraudCase.getReviewedBy(),
                fraudCase.getReviewedAt(),
                fraudCase.getAssignedTo() == null ? null : fraudCase.getAssignedTo().getId(),
                fraudCase.getAssignedTo() == null ? null : fraudCase.getAssignedTo().getUsername(),
                fraudCase.getAssignedBy() == null ? null : fraudCase.getAssignedBy().getId(),
                fraudCase.getAssignedBy() == null ? null : fraudCase.getAssignedBy().getUsername(),
                fraudCase.getAssignedAt(),
                fraudCase.getCreatedAt(),
                fraudCase.getClosedAt()
        );
    }

    private AlertStatus alertStatusFor(FraudCaseStatus status) {
        return switch (status) {
            case OPEN -> AlertStatus.OPEN;
            case UNDER_REVIEW, ESCALATED -> AlertStatus.IN_REVIEW;
            case CONFIRMED_FRAUD, CLOSED -> AlertStatus.RESOLVED;
            case FALSE_POSITIVE -> AlertStatus.FALSE_POSITIVE;
        };
    }

    private CasePriority priorityFor(com.bank.frauddetection.enums.RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> CasePriority.LOW;
            case MEDIUM -> CasePriority.MEDIUM;
            case HIGH -> CasePriority.HIGH;
            case CRITICAL -> CasePriority.CRITICAL;
        };
    }

    private Long bankId(Transaction transaction) {
        return transaction.getBank() == null ? null : transaction.getBank().getId();
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

    private void ensureCanAccess(User actor, Transaction transaction) {
        if (actor == null || isPlatform(actor)) {
            return;
        }
        if (actor.getBank() == null || transaction.getBank() == null || !actor.getBank().getId().equals(transaction.getBank().getId())) {
            throw new BusinessException("Cannot access another bank's fraud data");
        }
        if (actor.getBranch() != null && (transaction.getBranch() == null || !actor.getBranch().getId().equals(transaction.getBranch().getId()))) {
            throw new BusinessException("Cannot access another branch's fraud data");
        }
    }

    private boolean isPlatform(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.PLATFORM_ADMIN || role.getName() == RoleType.SUPER_ADMIN);
    }

    private record TenantScope(Long bankId, Long branchId) {
    }
}
