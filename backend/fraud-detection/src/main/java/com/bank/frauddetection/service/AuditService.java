package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.audit.AuditLogResponse;
import com.bank.frauddetection.entity.AuditLog;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.AuditLogRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditEventType eventType, User performedBy, User targetUser, Long bankId, String description, AuditStatus status) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType(eventType);
        auditLog.setPerformedByUserId(performedBy == null ? null : performedBy.getId());
        auditLog.setTargetUserId(targetUser == null ? null : targetUser.getId());
        auditLog.setBankId(bankId);
        auditLog.setDescription(description);
        auditLog.setStatus(status);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listLatest() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listLatestForBank(Long bankId) {
        return auditLogRepository.findTop50ByBankIdOrderByTimestampDesc(bankId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listLatest(String actorUsername, Long requestedBankId) {
        User actor = findUser(actorUsername);
        if (isPlatform(actor)) {
            return requestedBankId == null ? listLatest() : listLatestForBank(requestedBankId);
        }
        if (actor.getBank() == null) {
            throw new BusinessException("User is not assigned to a bank");
        }
        if (requestedBankId != null && !actor.getBank().getId().equals(requestedBankId)) {
            throw new BusinessException("Cannot access another bank's audit logs");
        }
        return listLatestForBank(actor.getBank().getId());
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEventType(),
                auditLog.getPerformedByUserId(),
                auditLog.getTargetUserId(),
                auditLog.getBankId(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getDescription(),
                auditLog.getTimestamp(),
                auditLog.getStatus()
        );
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
}
