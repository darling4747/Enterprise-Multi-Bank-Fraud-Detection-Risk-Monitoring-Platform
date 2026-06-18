package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.beneficiary.BeneficiaryCreateRequest;
import com.bank.frauddetection.dto.beneficiary.BeneficiaryResponse;
import com.bank.frauddetection.entity.Account;
import com.bank.frauddetection.entity.Beneficiary;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.AccountRepository;
import com.bank.frauddetection.repository.BeneficiaryRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BeneficiaryService {

    private static final int TRUSTED_SCORE = 50;
    private static final int TRUSTED_USAGE_COUNT = 5;

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> list(String actorUsername) {
        User actor = findUser(actorUsername);
        return beneficiaryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(beneficiary -> canAccess(actor, beneficiary.getAccount()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BeneficiaryResponse create(BeneficiaryCreateRequest request, String actorUsername) {
        User actor = findUser(actorUsername);
        Account account = accountRepository.findByAccountNumber(normalizeAccount(request.accountNumber()))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.accountNumber()));
        if (!canAccess(actor, account)) {
            throw new BusinessException("Cannot create beneficiary for another tenant's account");
        }
        String beneficiaryAccount = normalizeAccount(request.beneficiaryAccount());
        beneficiaryRepository.findByAccountAccountNumberAndBeneficiaryAccount(account.getAccountNumber(), beneficiaryAccount)
                .ifPresent(existing -> {
                    throw new BusinessException("Beneficiary already exists for this account");
                });
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setAccount(account);
        beneficiary.setBeneficiaryAccount(beneficiaryAccount);
        beneficiary.setTrustScore(request.trustScore() == null ? 0 : request.trustScore());
        beneficiary = beneficiaryRepository.save(beneficiary);
        auditService.log(AuditEventType.BENEFICIARY_ADDED, actor, null, account.getBank().getId(), "Beneficiary added: " + beneficiaryAccount, AuditStatus.SUCCESS);
        return toResponse(beneficiary);
    }

    @Transactional
    public boolean recordUsageAndCheckTrusted(String sourceAccount, String destinationAccount) {
        if (sourceAccount == null || destinationAccount == null) {
            return false;
        }
        return beneficiaryRepository.findByAccountAccountNumberAndBeneficiaryAccount(normalizeAccount(sourceAccount), normalizeAccount(destinationAccount))
                .map(beneficiary -> {
                    beneficiary.setUsageCount(beneficiary.getUsageCount() + 1);
                    beneficiary.setTrustScore(Math.min(100, beneficiary.getTrustScore() + 5));
                    beneficiaryRepository.save(beneficiary);
                    auditService.log(AuditEventType.BENEFICIARY_USED, null, null, beneficiary.getAccount().getBank().getId(), "Beneficiary used: " + beneficiary.getBeneficiaryAccount(), AuditStatus.SUCCESS);
                    return isTrusted(beneficiary);
                })
                .orElse(false);
    }

    @Transactional
    public BeneficiaryResponse markUsed(Long id, String actorUsername) {
        User actor = findUser(actorUsername);
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + id));
        if (!canAccess(actor, beneficiary.getAccount())) {
            throw new BusinessException("Cannot update another tenant's beneficiary");
        }
        beneficiary.setUsageCount(beneficiary.getUsageCount() + 1);
        beneficiary.setTrustScore(Math.min(100, beneficiary.getTrustScore() + 5));
        beneficiary = beneficiaryRepository.save(beneficiary);
        auditService.log(AuditEventType.BENEFICIARY_USED, actor, null, beneficiary.getAccount().getBank().getId(), "Beneficiary trust updated", AuditStatus.SUCCESS);
        return toResponse(beneficiary);
    }

    public boolean isTrusted(Beneficiary beneficiary) {
        return beneficiary.getTrustScore() >= TRUSTED_SCORE || beneficiary.getUsageCount() >= TRUSTED_USAGE_COUNT;
    }

    private boolean canAccess(User actor, Account account) {
        if (isPlatform(actor)) {
            return true;
        }
        if (actor.getBank() == null || account.getBank() == null || !Objects.equals(actor.getBank().getId(), account.getBank().getId())) {
            return false;
        }
        return actor.getBranch() == null || account.getBranch() != null && Objects.equals(actor.getBranch().getId(), account.getBranch().getId());
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getAccount().getId(),
                beneficiary.getAccount().getAccountNumber(),
                beneficiary.getBeneficiaryAccount(),
                beneficiary.getTrustScore(),
                beneficiary.getUsageCount(),
                beneficiary.getCreatedAt(),
                beneficiary.getUpdatedAt()
        );
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

    private String normalizeAccount(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
