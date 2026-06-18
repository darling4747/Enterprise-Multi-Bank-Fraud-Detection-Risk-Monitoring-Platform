package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.security.SecurityAlertCreateRequest;
import com.bank.frauddetection.dto.security.SecurityAlertResponse;
import com.bank.frauddetection.entity.Bank;
import com.bank.frauddetection.entity.Branch;
import com.bank.frauddetection.entity.SecurityAlert;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.NotificationType;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.SecurityIncidentStatus;
import com.bank.frauddetection.enums.SecurityIncidentType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.BankRepository;
import com.bank.frauddetection.repository.BranchRepository;
import com.bank.frauddetection.repository.SecurityAlertRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityIncidentService {

    private final SecurityAlertRepository securityAlertRepository;
    private final UserRepository userRepository;
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public SecurityIncidentService(
            SecurityAlertRepository securityAlertRepository,
            UserRepository userRepository,
            BankRepository bankRepository,
            BranchRepository branchRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.securityAlertRepository = securityAlertRepository;
        this.userRepository = userRepository;
        this.bankRepository = bankRepository;
        this.branchRepository = branchRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SecurityAlertResponse> list(String actorUsername) {
        User actor = findUser(actorUsername);
        return securityAlertRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(alert -> canAccess(actor, alert))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SecurityAlertResponse create(SecurityAlertCreateRequest request, String actorUsername) {
        User actor = findUser(actorUsername);
        if (!isPlatform(actor) && !hasRole(actor, RoleType.BANK_ADMIN)) {
            throw new BusinessException("Insufficient privileges to create security incidents");
        }
        User targetUser = request.userId() == null ? null : userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        Bank bank = resolveBank(actor, request.bankId(), targetUser);
        Branch branch = resolveBranch(actor, bank, request.branchId(), targetUser);
        return createInternal(request.eventType(), request.severity(), request.description(), targetUser, bank, branch, actor);
    }

    @Transactional
    public void recordForUser(User targetUser, SecurityIncidentType type, RiskLevel severity, String description) {
        createInternal(type, severity, description, targetUser, targetUser.getBank(), targetUser.getBranch(), null);
    }

    @Transactional
    public SecurityAlertResponse updateStatus(Long id, SecurityIncidentStatus status, String actorUsername) {
        User actor = findUser(actorUsername);
        SecurityAlert alert = securityAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Security incident not found: " + id));
        if (!canAccess(actor, alert)) {
            throw new BusinessException("Cannot update another tenant's security incident");
        }
        alert.setStatus(status);
        if (status == SecurityIncidentStatus.RESOLVED || status == SecurityIncidentStatus.FALSE_POSITIVE) {
            alert.setResolvedAt(Instant.now());
        } else {
            alert.setResolvedAt(null);
        }
        alert = securityAlertRepository.save(alert);
        auditService.log(AuditEventType.SECURITY_INCIDENT_UPDATED, actor, alert.getUser(), bankId(alert), "Security incident status changed to " + status, AuditStatus.SUCCESS);
        return toResponse(alert);
    }

    private SecurityAlertResponse createInternal(
            SecurityIncidentType type,
            RiskLevel severity,
            String description,
            User targetUser,
            Bank bank,
            Branch branch,
            User actor
    ) {
        SecurityAlert alert = new SecurityAlert();
        alert.setEventType(type);
        alert.setSeverity(severity == null ? RiskLevel.MEDIUM : severity);
        alert.setDescription(description);
        alert.setUser(targetUser);
        alert.setBank(bank);
        alert.setBranch(branch);
        alert = securityAlertRepository.save(alert);
        auditService.log(AuditEventType.SECURITY_INCIDENT_CREATED, actor, targetUser, bankId(alert), "Security incident created: " + type, AuditStatus.SUCCESS);
        notificationService.notifyPlatformUsers(NotificationType.SECURITY_INCIDENT, "Security incident: " + type + " - " + description);
        return toResponse(alert);
    }

    private Bank resolveBank(User actor, Long bankId, User targetUser) {
        if (targetUser != null && targetUser.getBank() != null) {
            return targetUser.getBank();
        }
        if (isPlatform(actor)) {
            return bankId == null ? null : bankRepository.findById(bankId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + bankId));
        }
        if (actor.getBank() == null) {
            throw new BusinessException("Bank Admin is not assigned to a bank");
        }
        if (bankId != null && !Objects.equals(actor.getBank().getId(), bankId)) {
            throw new BusinessException("Cannot create incident for another bank");
        }
        return actor.getBank();
    }

    private Branch resolveBranch(User actor, Bank bank, Long branchId, User targetUser) {
        if (targetUser != null && targetUser.getBranch() != null) {
            return targetUser.getBranch();
        }
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        if (bank != null && !branch.getBank().getId().equals(bank.getId())) {
            throw new BusinessException("Branch does not belong to bank");
        }
        if (!isPlatform(actor) && actor.getBank() != null && !Objects.equals(actor.getBank().getId(), branch.getBank().getId())) {
            throw new BusinessException("Cannot create incident for another bank branch");
        }
        return branch;
    }

    private boolean canAccess(User actor, SecurityAlert alert) {
        if (isPlatform(actor)) {
            return true;
        }
        if (actor.getBank() == null || alert.getBank() == null || !Objects.equals(actor.getBank().getId(), alert.getBank().getId())) {
            return false;
        }
        return actor.getBranch() == null || alert.getBranch() != null && Objects.equals(actor.getBranch().getId(), alert.getBranch().getId());
    }

    private SecurityAlertResponse toResponse(SecurityAlert alert) {
        return new SecurityAlertResponse(
                alert.getId(),
                alert.getEventType(),
                alert.getSeverity(),
                alert.getDescription(),
                alert.getStatus(),
                alert.getUser() == null ? null : alert.getUser().getId(),
                alert.getUser() == null ? null : alert.getUser().getUsername(),
                alert.getBank() == null ? null : alert.getBank().getId(),
                alert.getBank() == null ? null : alert.getBank().getCode(),
                alert.getBranch() == null ? null : alert.getBranch().getId(),
                alert.getBranch() == null ? null : alert.getBranch().getCode(),
                alert.getCreatedAt(),
                alert.getResolvedAt()
        );
    }

    private Long bankId(SecurityAlert alert) {
        return alert.getBank() == null ? null : alert.getBank().getId();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private boolean isPlatform(User user) {
        return hasRole(user, RoleType.PLATFORM_ADMIN) || hasRole(user, RoleType.SUPER_ADMIN);
    }

    private boolean hasRole(User user, RoleType roleType) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleType);
    }
}
