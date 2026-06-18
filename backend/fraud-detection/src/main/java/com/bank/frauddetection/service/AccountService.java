package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.account.AccountCreateRequest;
import com.bank.frauddetection.dto.account.AccountResponse;
import com.bank.frauddetection.entity.Account;
import com.bank.frauddetection.entity.Branch;
import com.bank.frauddetection.entity.Customer;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AccountStatus;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.NotificationType;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.AccountRepository;
import com.bank.frauddetection.repository.BranchRepository;
import com.bank.frauddetection.repository.CustomerRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(String actorUsername) {
        User actor = findUser(actorUsername);
        return accountRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(account -> canAccess(actor, account))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest request, String actorUsername) {
        User actor = findUser(actorUsername);
        ensureCanCreate(actor);
        String accountNumber = request.accountNumber().trim().toUpperCase();
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new BusinessException("Account number already exists");
        }
        Customer customer = customerRepository.findByCustomerId(request.customerId().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        if (!canAccess(actor, customer)) {
            throw new BusinessException("Cannot create account for another tenant's customer");
        }
        Branch branch = resolveBranch(actor, customer, request.branchId());
        Account account = new Account();
        account.setCustomer(customer);
        account.setBank(customer.getBank());
        account.setBranch(branch);
        account.setAccountNumber(accountNumber);
        account.setAccountType(request.accountType());
        account.setBalance(request.balance() == null ? BigDecimal.ZERO : request.balance());
        account.setCurrency(defaultCurrency(request.currency()));
        account = accountRepository.save(account);
        auditService.log(AuditEventType.ACCOUNT_CREATED, actor, null, customer.getBank().getId(), "Account created: " + accountNumber, AuditStatus.SUCCESS);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse updateStatus(Long id, AccountStatus status, String actorUsername) {
        User actor = findUser(actorUsername);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        if (!canAccess(actor, account)) {
            throw new BusinessException("Cannot update another tenant's account");
        }
        account.setStatus(status);
        account = accountRepository.save(account);
        auditService.log(AuditEventType.ACCOUNT_STATUS_CHANGED, actor, null, account.getBank().getId(), "Account status changed to " + status + ": " + account.getAccountNumber(), AuditStatus.SUCCESS);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse approveBranchOperation(Long id, String actorUsername) {
        User actor = findUser(actorUsername);
        if (!hasRole(actor, RoleType.BRANCH_MANAGER) && !hasRole(actor, RoleType.BANK_ADMIN) && !isPlatform(actor)) {
            throw new BusinessException("Insufficient privileges to approve branch operations");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        if (!canAccess(actor, account)) {
            throw new BusinessException("Cannot approve another tenant's account operation");
        }
        auditService.log(AuditEventType.ACCOUNT_STATUS_CHANGED, actor, null, account.getBank().getId(), "Branch operation approved for account " + account.getAccountNumber(), AuditStatus.SUCCESS);
        notificationService.notifyPlatformUsers(NotificationType.SECURITY_INCIDENT, "Branch operation approved for account " + account.getAccountNumber());
        return toResponse(account);
    }

    private void ensureCanCreate(User actor) {
        if (isPlatform(actor) || hasRole(actor, RoleType.BANK_ADMIN) || hasRole(actor, RoleType.BRANCH_MANAGER)) {
            return;
        }
        throw new BusinessException("Insufficient privileges to create accounts");
    }

    private Branch resolveBranch(User actor, Customer customer, Long branchId) {
        if (hasRole(actor, RoleType.BRANCH_MANAGER)) {
            if (actor.getBranch() == null) {
                throw new BusinessException("Branch Manager is not assigned to a branch");
            }
            return actor.getBranch();
        }
        if (branchId == null) {
            return customer.getBranch();
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        if (!branch.getBank().getId().equals(customer.getBank().getId())) {
            throw new BusinessException("Branch does not belong to customer bank");
        }
        return branch;
    }

    private boolean canAccess(User actor, Customer customer) {
        if (isPlatform(actor)) {
            return true;
        }
        if (actor.getBank() == null || !Objects.equals(actor.getBank().getId(), customer.getBank().getId())) {
            return false;
        }
        return actor.getBranch() == null || customer.getBranch() != null && Objects.equals(actor.getBranch().getId(), customer.getBranch().getId());
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

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomer().getCustomerId(),
                account.getCustomer().getFullName(),
                account.getBank().getId(),
                account.getBank().getCode(),
                account.getBranch() == null ? null : account.getBranch().getId(),
                account.getBranch() == null ? null : account.getBranch().getCode(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
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

    private String defaultCurrency(String currency) {
        return currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
    }
}
