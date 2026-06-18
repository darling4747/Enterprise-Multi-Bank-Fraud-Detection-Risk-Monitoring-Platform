package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.dashboard.DashboardChartResponse;
import com.bank.frauddetection.dto.dashboard.DashboardStatsResponse;
import com.bank.frauddetection.entity.Alert;
import com.bank.frauddetection.entity.FraudCase;
import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AlertStatus;
import com.bank.frauddetection.enums.FraudCaseStatus;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.TransactionStatus;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.AlertRepository;
import com.bank.frauddetection.repository.FraudCaseRepository;
import com.bank.frauddetection.repository.TransactionRepository;
import com.bank.frauddetection.repository.UserRepository;
import com.bank.frauddetection.util.DateUtil;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public DashboardService(
            TransactionRepository transactionRepository,
            FraudCaseRepository fraudCaseRepository,
            AlertRepository alertRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        return stats(null);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats(String actorUsername) {
        TenantScope scope = actorUsername == null ? new TenantScope(null, null) : resolveScope(findUser(actorUsername));
        List<Transaction> transactions = scopedTransactions(scope, null);
        List<Transaction> recentTransactions = scopedTransactions(scope, DateUtil.hoursAgo(24));
        List<Alert> alerts = alertRepository.search(null, scope.bankId(), scope.branchId());
        List<FraudCase> fraudCases = fraudCaseRepository.search(scope.bankId(), scope.branchId());
        return new DashboardStatsResponse(
                transactions.size(),
                countStatus(transactions, TransactionStatus.PENDING),
                countStatus(transactions, TransactionStatus.REVIEW),
                countStatus(transactions, TransactionStatus.BLOCKED),
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.OPEN).count(),
                fraudCases.stream().filter(fraudCase -> fraudCase.getStatus() == FraudCaseStatus.OPEN || fraudCase.getStatus() == FraudCaseStatus.UNDER_REVIEW || fraudCase.getStatus() == FraudCaseStatus.ESCALATED).count(),
                countRisk(transactions, RiskLevel.HIGH),
                countRisk(transactions, RiskLevel.CRITICAL),
                recentTransactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    @Transactional(readOnly = true)
    public DashboardChartResponse charts() {
        return charts(null);
    }

    @Transactional(readOnly = true)
    public DashboardChartResponse charts(String actorUsername) {
        TenantScope scope = actorUsername == null ? new TenantScope(null, null) : resolveScope(findUser(actorUsername));
        List<Transaction> transactions = scopedTransactions(scope, null);
        List<Alert> alertRows = alertRepository.search(null, scope.bankId(), scope.branchId());
        Map<String, Long> risks = Arrays.stream(RiskLevel.values())
                .collect(Collectors.toMap(Enum::name, risk -> countRisk(transactions, risk)));
        Map<String, Long> statuses = Arrays.stream(TransactionStatus.values())
                .collect(Collectors.toMap(Enum::name, status -> countStatus(transactions, status)));
        Map<String, Long> alerts = Arrays.stream(AlertStatus.values())
                .collect(Collectors.toMap(Enum::name, status -> alertRows.stream().filter(alert -> alert.getStatus() == status).count()));
        return new DashboardChartResponse(risks, statuses, alerts);
    }

    private long countStatus(List<Transaction> transactions, TransactionStatus status) {
        return transactions.stream().filter(transaction -> transaction.getStatus() == status).count();
    }

    private List<Transaction> scopedTransactions(TenantScope scope, java.time.Instant fromDate) {
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(transaction -> scope.bankId() == null || transaction.getBank() != null && Objects.equals(transaction.getBank().getId(), scope.bankId()))
                .filter(transaction -> scope.branchId() == null || transaction.getBranch() != null && Objects.equals(transaction.getBranch().getId(), scope.branchId()))
                .filter(transaction -> fromDate == null || !transaction.getCreatedAt().isBefore(fromDate))
                .toList();
    }

    private long countRisk(List<Transaction> transactions, RiskLevel riskLevel) {
        return transactions.stream().filter(transaction -> transaction.getRiskLevel() == riskLevel).count();
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

    private boolean isPlatform(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.PLATFORM_ADMIN || role.getName() == RoleType.SUPER_ADMIN);
    }

    private record TenantScope(Long bankId, Long branchId) {
    }
}
