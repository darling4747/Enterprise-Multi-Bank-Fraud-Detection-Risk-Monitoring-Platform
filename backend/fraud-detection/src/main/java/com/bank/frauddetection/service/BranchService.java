package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.branch.BranchCreateRequest;
import com.bank.frauddetection.dto.branch.BranchResponse;
import com.bank.frauddetection.entity.Bank;
import com.bank.frauddetection.entity.Branch;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.BranchStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.BankRepository;
import com.bank.frauddetection.repository.BranchRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final BankRepository bankRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BranchService(
            BranchRepository branchRepository,
            BankRepository bankRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.branchRepository = branchRepository;
        this.bankRepository = bankRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(String actorUsername, Long bankId) {
        User actor = findUser(actorUsername);
        Long resolvedBankId = resolveBankId(actor, bankId);
        return (resolvedBankId == null ? branchRepository.findAll() : branchRepository.findByBankId(resolvedBankId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BranchResponse createBranch(BranchCreateRequest request, String actorUsername) {
        User actor = findUser(actorUsername);
        Long bankId = resolveBankId(actor, request.bankId());
        if (bankId == null) {
            throw new BusinessException("Bank id is required");
        }
        Bank bank = bankRepository.findById(bankId).orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + bankId));
        String code = request.code().trim().toUpperCase();
        if (branchRepository.existsByBankIdAndCode(bank.getId(), code)) {
            throw new BusinessException("Branch code already exists for this bank");
        }
        String ifscCode = normalizeOptionalUpper(request.ifscCode());
        if (ifscCode != null && branchRepository.existsByIfscCode(ifscCode)) {
            throw new BusinessException("IFSC code already exists");
        }
        Branch branch = new Branch();
        branch.setBank(bank);
        branch.setCode(code);
        branch.setName(request.name().trim());
        branch.setIfscCode(ifscCode);
        branch.setCity(blankToNull(request.city()));
        branch.setState(blankToNull(request.state()));
        branch.setAddress(blankToNull(request.address()));
        branch.setManagerName(blankToNull(request.managerName()));
        branch = branchRepository.save(branch);
        auditService.log(AuditEventType.BRANCH_CREATED, actor, null, bank.getId(), "Branch created: " + code, AuditStatus.SUCCESS);
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse disableBranch(Long id, String actorUsername) {
        User actor = findUser(actorUsername);
        Branch branch = branchRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
        Long actorBankId = actor.getBank() == null ? null : actor.getBank().getId();
        if (!isPlatform(actor) && !branch.getBank().getId().equals(actorBankId)) {
            throw new BusinessException("Cannot modify another bank's branch");
        }
        branch.setStatus(BranchStatus.DISABLED);
        branch = branchRepository.save(branch);
        auditService.log(AuditEventType.BRANCH_DISABLED, actor, null, branch.getBank().getId(), "Branch disabled: " + branch.getCode(), AuditStatus.SUCCESS);
        return toResponse(branch);
    }

    private Long resolveBankId(User actor, Long requestedBankId) {
        if (isPlatform(actor)) {
            return requestedBankId;
        }
        if (hasRole(actor, RoleType.BANK_ADMIN)) {
            if (actor.getBank() == null) {
                throw new BusinessException("Bank Admin is not assigned to a bank");
            }
            if (requestedBankId != null && !actor.getBank().getId().equals(requestedBankId)) {
                throw new BusinessException("Bank Admin cannot access another bank");
            }
            return actor.getBank().getId();
        }
        throw new BusinessException("Insufficient privileges");
    }

    private boolean isPlatform(User user) {
        return hasRole(user, RoleType.PLATFORM_ADMIN) || hasRole(user, RoleType.SUPER_ADMIN);
    }

    private boolean hasRole(User user, RoleType roleType) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleType);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getBank().getId(),
                branch.getBank().getCode(),
                branch.getCode(),
                branch.getName(),
                branch.getIfscCode(),
                branch.getCity(),
                branch.getState(),
                branch.getAddress(),
                branch.getManagerName(),
                branch.getStatus(),
                branch.getCreatedAt()
        );
    }

    private String normalizeOptionalUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
