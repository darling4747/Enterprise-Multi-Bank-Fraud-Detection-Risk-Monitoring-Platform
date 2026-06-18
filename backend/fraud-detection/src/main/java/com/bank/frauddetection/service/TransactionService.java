package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.transaction.ExternalTransactionIngestRequest;
import com.bank.frauddetection.dto.transaction.TransactionRequest;
import com.bank.frauddetection.dto.transaction.TransactionResponse;
import com.bank.frauddetection.dto.transaction.TransactionSummaryResponse;
import com.bank.frauddetection.entity.Account;
import com.bank.frauddetection.entity.Bank;
import com.bank.frauddetection.entity.Branch;
import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AccountStatus;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.TransactionChannel;
import com.bank.frauddetection.enums.TransactionStatus;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.kafka.event.TransactionCreatedEvent;
import com.bank.frauddetection.kafka.producer.TransactionProducer;
import com.bank.frauddetection.repository.AccountRepository;
import com.bank.frauddetection.repository.BankRepository;
import com.bank.frauddetection.repository.BranchRepository;
import com.bank.frauddetection.repository.TransactionRepository;
import com.bank.frauddetection.repository.AlertRepository;
import com.bank.frauddetection.repository.FraudCaseRepository;
import com.bank.frauddetection.repository.UserRepository;
import com.bank.frauddetection.util.ValidationUtil;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final AlertRepository alertRepository;
    private final AccountRepository accountRepository;
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final FraudDetectionService fraudDetectionService;
    private final TransactionProducer transactionProducer;
    private final AuditService auditService;
    private final BeneficiaryService beneficiaryService;

    public TransactionService(
            TransactionRepository transactionRepository,
            FraudCaseRepository fraudCaseRepository,
            AlertRepository alertRepository,
            AccountRepository accountRepository,
            BankRepository bankRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            FraudDetectionService fraudDetectionService,
            TransactionProducer transactionProducer,
            AuditService auditService,
            BeneficiaryService beneficiaryService
    ) {
        this.transactionRepository = transactionRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.alertRepository = alertRepository;
        this.accountRepository = accountRepository;
        this.bankRepository = bankRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.transactionProducer = transactionProducer;
        this.auditService = auditService;
        this.beneficiaryService = beneficiaryService;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        return create(request, null);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, String actorUsername) {
        ValidationUtil.ensurePositiveAmount(request.amount());
        User actor = actorUsername == null ? null : findUser(actorUsername);
        log.info(
                "TRANSACTION_RECEIVED customerId={} type={} amount={} accountType={} customerType={} beneficiaryTrusted={} knownDevice={} knownLocation={} hour={}",
                request.customerId(),
                request.type(),
                request.amount(),
                request.accountType(),
                request.customerType(),
                request.beneficiaryTrusted(),
                request.knownDevice(),
                request.knownLocation(),
                request.transactionHour()
        );
        Transaction transaction = new Transaction();
        transaction.setCustomerId(request.customerId());
        transaction.setSourceAccount(request.sourceAccount());
        transaction.setDestinationAccount(request.destinationAccount());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency().toUpperCase());
        transaction.setChannel(normalizeChannel(request.channel()).name());
        transaction.setMerchantCategory(normalize(request.merchantCategory()));
        transaction.setCountry(normalize(request.country()));
        transaction.setIpAddress(request.ipAddress());
        transaction.setDeviceId(request.deviceId());
        transaction.setAccountType(request.accountType());
        transaction.setCustomerType(request.customerType());
        boolean trustedByHistory = beneficiaryService.recordUsageAndCheckTrusted(request.sourceAccount(), request.destinationAccount());
        transaction.setBeneficiaryTrusted(request.beneficiaryTrusted() || trustedByHistory);
        transaction.setKnownDevice(request.knownDevice());
        transaction.setKnownLocation(request.knownLocation());
        transaction.setTransactionHour(request.transactionHour());
        transaction.setDailyTransactionPattern(request.dailyTransactionPattern());
        transaction.setTransactionType(normalize(request.type()));
        transaction.setStep(request.step());
        transaction.setOldbalanceOrg(request.oldbalanceOrg());
        transaction.setNewbalanceOrig(request.newbalanceOrig());
        transaction.setOldbalanceDest(request.oldbalanceDest());
        transaction.setNewbalanceDest(request.newbalanceDest());
        enrichFromAccounts(transaction, request, actor);
        assignTenant(transaction, request.bankId(), request.branchId(), actor);
        transaction = transactionRepository.save(transaction);
        log.info("TRANSACTION_CREATED id={} reference={} bankId={} branchId={}", transaction.getId(), transaction.getReference(), request.bankId(), request.branchId());
        auditService.log(AuditEventType.TRANSACTION_CREATED, actor, null, bankId(transaction), "Transaction created: " + transaction.getReference(), AuditStatus.SUCCESS);
        log.info("AUDIT_LOG_CREATED event={} reference={}", AuditEventType.TRANSACTION_CREATED, transaction.getReference());
        transactionProducer.publishTransactionCreated(new TransactionCreatedEvent(
                transaction.getId(),
                transaction.getReference(),
                transaction.getCustomerId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCreatedAt()
        ));
        fraudDetectionService.analyze(transaction);
        Transaction saved = transactionRepository.findById(transaction.getId()).orElseThrow();
        applyApprovedMoneyMovement(saved);
        log.info(
                "TRANSACTION_SAVED id={} reference={} status={} finalRiskScore={} decision={}",
                saved.getId(),
                saved.getReference(),
                saved.getStatus(),
                saved.getRiskScore(),
                saved.getFraudDecision()
        );
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse ingest(ExternalTransactionIngestRequest request, String actorUsername) {
        if (request.senderAccountNumber().trim().equalsIgnoreCase(request.receiverAccountNumber().trim())) {
            throw new BusinessException("Sender and receiver accounts must be different");
        }
        log.info(
                "EXTERNAL_TRANSACTION_INGEST_RECEIVED bankId={} branchId={} sender={} receiver={} type={} channel={} amount={}",
                request.bankId(),
                request.branchId(),
                request.senderAccountNumber(),
                request.receiverAccountNumber(),
                request.transactionType(),
                request.channel(),
                request.amount()
        );
        return create(toTransactionRequest(request), actorUsername);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id) {
        return get(id, null);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        return transactionRepository.findById(id)
                .map(transaction -> {
                    ensureCanAccess(actor, transaction);
                    return transaction;
                })
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryResponse> search(
            String customerId,
            TransactionStatus status,
            RiskLevel riskLevel,
            Long bankId,
            Long branchId,
            Instant fromDate,
            Instant toDate
    ) {
        return search(customerId, status, riskLevel, bankId, branchId, fromDate, toDate, null);
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryResponse> search(
            String customerId,
            TransactionStatus status,
            RiskLevel riskLevel,
            Long bankId,
            Long branchId,
            Instant fromDate,
            Instant toDate,
            String actorUsername
    ) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        TenantScope scope = resolveScope(actor, bankId, branchId);
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(transaction -> customerId == null || Objects.equals(transaction.getCustomerId(), customerId))
                .filter(transaction -> status == null || transaction.getStatus() == status)
                .filter(transaction -> riskLevel == null || transaction.getRiskLevel() == riskLevel)
                .filter(transaction -> scope.bankId() == null || transaction.getBank() != null && Objects.equals(transaction.getBank().getId(), scope.bankId()))
                .filter(transaction -> scope.branchId() == null || transaction.getBranch() != null && Objects.equals(transaction.getBranch().getId(), scope.branchId()))
                .filter(transaction -> fromDate == null || !transaction.getCreatedAt().isBefore(fromDate))
                .filter(transaction -> toDate == null || !transaction.getCreatedAt().isAfter(toDate))
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public TransactionResponse updateStatus(Long id, TransactionStatus status) {
        return updateStatus(id, status, null);
    }

    @Transactional
    public TransactionResponse updateStatus(Long id, TransactionStatus status, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        ensureCanAccess(actor, transaction);
        transaction.setStatus(status);
        transaction = transactionRepository.save(transaction);
        auditService.log(
                status == TransactionStatus.REVIEW ? AuditEventType.TRANSACTION_REVIEWED : AuditEventType.TRANSACTION_UPDATED,
                actor,
                null,
                bankId(transaction),
                "Transaction status changed to " + status + ": " + transaction.getReference(),
                AuditStatus.SUCCESS
        );
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request, String actorUsername) {
        ValidationUtil.ensurePositiveAmount(request.amount());
        User actor = actorUsername == null ? null : findUser(actorUsername);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        ensureCanAccess(actor, transaction);
        transaction.setCustomerId(request.customerId());
        transaction.setSourceAccount(request.sourceAccount());
        transaction.setDestinationAccount(request.destinationAccount());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency().toUpperCase());
        transaction.setChannel(normalizeChannel(request.channel()).name());
        transaction.setMerchantCategory(normalize(request.merchantCategory()));
        transaction.setCountry(normalize(request.country()));
        transaction.setIpAddress(request.ipAddress());
        transaction.setDeviceId(request.deviceId());
        transaction.setAccountType(request.accountType());
        transaction.setCustomerType(request.customerType());
        boolean trustedByHistory = beneficiaryService.recordUsageAndCheckTrusted(request.sourceAccount(), request.destinationAccount());
        transaction.setBeneficiaryTrusted(request.beneficiaryTrusted() || trustedByHistory);
        transaction.setKnownDevice(request.knownDevice());
        transaction.setKnownLocation(request.knownLocation());
        transaction.setTransactionHour(request.transactionHour());
        transaction.setDailyTransactionPattern(request.dailyTransactionPattern());
        transaction.setTransactionType(normalize(request.type()));
        transaction.setStep(request.step());
        transaction.setOldbalanceOrg(request.oldbalanceOrg());
        transaction.setNewbalanceOrig(request.newbalanceOrig());
        transaction.setOldbalanceDest(request.oldbalanceDest());
        transaction.setNewbalanceDest(request.newbalanceDest());
        enrichFromAccounts(transaction, request, actor);
        assignTenant(transaction, request.bankId(), request.branchId(), actor);
        transaction = transactionRepository.save(transaction);
        log.info("TRANSACTION_UPDATED id={} reference={} amount={} type={}", transaction.getId(), transaction.getReference(), transaction.getAmount(), transaction.getTransactionType());
        auditService.log(AuditEventType.TRANSACTION_UPDATED, actor, null, bankId(transaction), "Transaction updated: " + transaction.getReference(), AuditStatus.SUCCESS);
        fraudDetectionService.analyze(transaction);
        return toResponse(transactionRepository.findById(transaction.getId()).orElseThrow());
    }

    @Transactional
    public void delete(Long id) {
        delete(id, null);
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        ensureCanAccess(actor, transaction);
        alertRepository.deleteByTransactionId(id);
        fraudCaseRepository.findByTransactionId(id).ifPresent(fraudCaseRepository::delete);
        transactionRepository.delete(transaction);
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getCustomerId(),
                transaction.getSourceAccount(),
                transaction.getDestinationAccount(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getChannel(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getIpAddress(),
                transaction.getDeviceId(),
                transaction.getAccountType(),
                transaction.getCustomerType(),
                transaction.isBeneficiaryTrusted(),
                transaction.isKnownDevice(),
                transaction.isKnownLocation(),
                transaction.getTransactionHour(),
                transaction.getDailyTransactionPattern(),
                transaction.getTransactionType(),
                transaction.getStep(),
                transaction.getOldbalanceOrg(),
                transaction.getNewbalanceOrig(),
                transaction.getOldbalanceDest(),
                transaction.getNewbalanceDest(),
                transaction.getBank() == null ? null : transaction.getBank().getId(),
                transaction.getBank() == null ? null : transaction.getBank().getCode(),
                transaction.getBranch() == null ? null : transaction.getBranch().getId(),
                transaction.getBranch() == null ? null : transaction.getBranch().getCode(),
                transaction.getStatus(),
                transaction.getRiskLevel(),
                transaction.getFraudDecision(),
                transaction.getRiskScore(),
                transaction.getRiskSummary(),
                transaction.getCreatedAt(),
                transaction.getProcessedAt()
        );
    }

    public TransactionSummaryResponse toSummary(Transaction transaction) {
        return new TransactionSummaryResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getCustomerId(),
                transaction.getBank() == null ? null : transaction.getBank().getId(),
                transaction.getBranch() == null ? null : transaction.getBranch().getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getRiskLevel(),
                transaction.getRiskScore(),
                transaction.getCreatedAt()
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private TransactionChannel normalizeChannel(String channel) {
        String normalized = normalize(channel);
        if ("BRANCH".equals(normalized)) {
            normalized = "BRANCH_BANKING";
        }
        try {
            return TransactionChannel.valueOf(normalized);
        } catch (RuntimeException ex) {
            throw new BusinessException("Unsupported transaction channel: " + channel);
        }
    }

    private String normalizeAccount(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private TransactionRequest toTransactionRequest(ExternalTransactionIngestRequest request) {
        return new TransactionRequest(
                defaultText(request.customerId(), request.senderAccountNumber()),
                request.senderAccountNumber().trim(),
                request.receiverAccountNumber().trim(),
                request.amount(),
                defaultText(request.currency(), "INR"),
                request.channel().trim(),
                request.transactionType(),
                request.location(),
                request.ipAddress(),
                request.deviceId(),
                request.accountType(),
                request.customerType(),
                Boolean.TRUE.equals(request.beneficiaryTrusted()),
                Boolean.TRUE.equals(request.knownDevice()),
                Boolean.TRUE.equals(request.knownLocation()),
                request.transactionHour() == null ? LocalTime.now().getHour() : request.transactionHour(),
                request.dailyTransactionPattern(),
                request.transactionType(),
                request.step(),
                request.oldbalanceOrg(),
                request.newbalanceOrig(),
                request.oldbalanceDest(),
                request.newbalanceDest(),
                request.bankId(),
                request.branchId()
        );
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback.trim() : value.trim();
    }

    private void assignTenant(Transaction transaction, Long requestBankId, Long requestBranchId, User actor) {
        if (transaction.getBank() != null) {
            if (requestBankId != null && !transaction.getBank().getId().equals(requestBankId)) {
                throw new BusinessException("Transaction account does not belong to selected bank");
            }
            if (requestBranchId != null && (transaction.getBranch() == null || !transaction.getBranch().getId().equals(requestBranchId))) {
                throw new BusinessException("Transaction account does not belong to selected branch");
            }
            if (actor != null && !isPlatform(actor)) {
                ensureCanAccess(actor, transaction);
            }
            return;
        }
        Bank bank = resolveBank(requestBankId, actor);
        Branch branch = resolveBranch(requestBranchId, bank, actor);
        if (bank == null && branch != null) {
            bank = branch.getBank();
        }
        transaction.setBank(bank);
        transaction.setBranch(branch);
    }

    private void enrichFromAccounts(Transaction transaction, TransactionRequest request, User actor) {
        Account source = accountRepository.findByAccountNumber(normalizeAccount(request.sourceAccount())).orElse(null);
        Account destination = accountRepository.findByAccountNumber(normalizeAccount(request.destinationAccount())).orElse(null);
        if (source == null && destination == null) {
            return;
        }
        if (source == null || destination == null) {
            throw new BusinessException("Both sender and receiver accounts must exist when account validation is enabled");
        }
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Sender account is not active");
        }
        if (destination.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessException("Receiver account is closed");
        }
        if (actor != null && !isPlatform(actor)) {
            ensureCanAccessAccount(actor, source);
        }
        if (!source.getBank().getId().equals(destination.getBank().getId())) {
            throw new BusinessException("Cross-bank transfers must arrive through the external bank integration gateway");
        }
        transaction.setBank(source.getBank());
        transaction.setBranch(source.getBranch());
        transaction.setCustomerId(source.getCustomer().getCustomerId());
        if (request.accountType() == null) {
            transaction.setAccountType(source.getAccountType());
        }
        if (request.customerType() == null) {
            transaction.setCustomerType(source.getCustomer().getCustomerType());
        }
        if (request.oldbalanceOrg() == null) {
            transaction.setOldbalanceOrg(source.getBalance());
        }
        if (request.oldbalanceDest() == null) {
            transaction.setOldbalanceDest(destination.getBalance());
        }
    }

    private void applyApprovedMoneyMovement(Transaction transaction) {
        if (transaction.getFraudDecision() != FraudDecision.APPROVE) {
            return;
        }
        Account source = accountRepository.findByAccountNumber(normalizeAccount(transaction.getSourceAccount())).orElse(null);
        Account destination = accountRepository.findByAccountNumber(normalizeAccount(transaction.getDestinationAccount())).orElse(null);
        if (source == null || destination == null) {
            return;
        }
        if (source.getBalance().compareTo(transaction.getAmount()) < 0) {
            log.info("ACCOUNT_BALANCE_NOT_MOVED reference={} reason=insufficient_source_balance", transaction.getReference());
            return;
        }
        source.setBalance(source.getBalance().subtract(transaction.getAmount()));
        destination.setBalance(destination.getBalance().add(transaction.getAmount()));
        accountRepository.save(source);
        accountRepository.save(destination);
        log.info("ACCOUNT_BALANCES_MOVED reference={} source={} destination={} amount={}", transaction.getReference(), source.getAccountNumber(), destination.getAccountNumber(), transaction.getAmount());
    }

    private void ensureCanAccessAccount(User actor, Account account) {
        if (actor.getBank() == null || account.getBank() == null || !actor.getBank().getId().equals(account.getBank().getId())) {
            throw new BusinessException("Cannot access another bank's account");
        }
        if (actor.getBranch() != null && (account.getBranch() == null || !actor.getBranch().getId().equals(account.getBranch().getId()))) {
            throw new BusinessException("Cannot access another branch's account");
        }
    }

    private Bank resolveBank(Long requestBankId, User actor) {
        if (requestBankId != null) {
            Bank bank = bankRepository.findById(requestBankId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + requestBankId));
            if (actor != null && !isPlatform(actor) && actor.getBank() != null && !actor.getBank().getId().equals(bank.getId())) {
                throw new BusinessException("Cannot create transaction for another bank");
            }
            return bank;
        }
        return actor == null ? null : actor.getBank();
    }

    private Branch resolveBranch(Long requestBranchId, Bank bank, User actor) {
        if (requestBranchId == null) {
            return actor == null ? null : actor.getBranch();
        }
        Branch branch = branchRepository.findById(requestBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + requestBranchId));
        if (bank != null && !branch.getBank().getId().equals(bank.getId())) {
            throw new BusinessException("Branch does not belong to the selected bank");
        }
        if (actor != null && !isPlatform(actor) && actor.getBank() != null && !actor.getBank().getId().equals(branch.getBank().getId())) {
            throw new BusinessException("Cannot create transaction for another bank branch");
        }
        if (actor != null && !isPlatform(actor) && actor.getBranch() != null && !actor.getBranch().getId().equals(branch.getId())) {
            throw new BusinessException("Cannot create transaction for another branch");
        }
        return branch;
    }

    private TenantScope resolveScope(User actor, Long requestedBankId, Long requestedBranchId) {
        if (actor == null || isPlatform(actor)) {
            return new TenantScope(requestedBankId, requestedBranchId);
        }
        if (actor.getBank() == null) {
            throw new BusinessException("User is not assigned to a bank");
        }
        if (requestedBankId != null && !actor.getBank().getId().equals(requestedBankId)) {
            throw new BusinessException("Cannot access another bank's transactions");
        }
        Long bankId = actor.getBank().getId();
        Long branchId = requestedBranchId;
        if (actor.getBranch() != null) {
            if (requestedBranchId != null && !actor.getBranch().getId().equals(requestedBranchId)) {
                throw new BusinessException("Cannot access another branch's transactions");
            }
            branchId = actor.getBranch().getId();
        } else if (requestedBranchId != null) {
            Branch branch = branchRepository.findById(requestedBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + requestedBranchId));
            if (!branch.getBank().getId().equals(bankId)) {
                throw new BusinessException("Cannot access another bank's branch");
            }
        }
        return new TenantScope(bankId, branchId);
    }

    private void ensureCanAccess(User actor, Transaction transaction) {
        if (actor == null || isPlatform(actor)) {
            return;
        }
        if (actor.getBank() == null || transaction.getBank() == null || !actor.getBank().getId().equals(transaction.getBank().getId())) {
            throw new BusinessException("Cannot access another bank's transaction");
        }
        if (actor.getBranch() != null && (transaction.getBranch() == null || !actor.getBranch().getId().equals(transaction.getBranch().getId()))) {
            throw new BusinessException("Cannot access another branch's transaction");
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private boolean isPlatform(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.PLATFORM_ADMIN || role.getName() == RoleType.SUPER_ADMIN);
    }

    private Long bankId(Transaction transaction) {
        return transaction.getBank() == null ? null : transaction.getBank().getId();
    }

    private record TenantScope(Long bankId, Long branchId) {
    }
}
