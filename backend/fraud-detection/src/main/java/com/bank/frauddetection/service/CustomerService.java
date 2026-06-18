package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.customer.CustomerCreateRequest;
import com.bank.frauddetection.dto.customer.CustomerResponse;
import com.bank.frauddetection.entity.Bank;
import com.bank.frauddetection.entity.Branch;
import com.bank.frauddetection.entity.Customer;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.CustomerStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.BankRepository;
import com.bank.frauddetection.repository.BranchRepository;
import com.bank.frauddetection.repository.CustomerRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CustomerService(
            CustomerRepository customerRepository,
            BankRepository bankRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.customerRepository = customerRepository;
        this.bankRepository = bankRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String actorUsername) {
        User actor = findUser(actorUsername);
        return customerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(customer -> canAccess(actor, customer.getBank(), customer.getBranch()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request, String actorUsername) {
        User actor = findUser(actorUsername);
        ensureCanCreate(actor);
        String customerId = request.customerId().trim().toUpperCase();
        if (customerRepository.existsByCustomerId(customerId)) {
            throw new BusinessException("Customer ID already exists");
        }
        Bank bank = resolveBank(actor, request.bankId());
        Branch branch = resolveBranch(actor, bank, request.branchId());
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setBank(bank);
        customer.setBranch(branch);
        customer.setCustomerType(request.customerType());
        customer.setFullName(request.fullName().trim());
        customer.setEmail(blankToNull(request.email()));
        customer.setPhone(blankToNull(request.phone()));
        customer = customerRepository.save(customer);
        auditService.log(AuditEventType.CUSTOMER_CREATED, actor, null, bank.getId(), "Customer created: " + customerId, AuditStatus.SUCCESS);
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, CustomerStatus status, String actorUsername) {
        User actor = findUser(actorUsername);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        if (!canAccess(actor, customer.getBank(), customer.getBranch())) {
            throw new BusinessException("Cannot update another tenant's customer");
        }
        customer.setStatus(status);
        return toResponse(customerRepository.save(customer));
    }

    private void ensureCanCreate(User actor) {
        if (isPlatform(actor) || hasRole(actor, RoleType.BANK_ADMIN) || hasRole(actor, RoleType.BRANCH_MANAGER)) {
            return;
        }
        throw new BusinessException("Insufficient privileges to create customers");
    }

    private Bank resolveBank(User actor, Long bankId) {
        if (isPlatform(actor)) {
            if (bankId == null) {
                throw new BusinessException("Bank id is required");
            }
            return bankRepository.findById(bankId).orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + bankId));
        }
        if (actor.getBank() == null) {
            throw new BusinessException("User is not assigned to a bank");
        }
        if (bankId != null && !Objects.equals(actor.getBank().getId(), bankId)) {
            throw new BusinessException("Cannot create customer for another bank");
        }
        return actor.getBank();
    }

    private Branch resolveBranch(User actor, Bank bank, Long branchId) {
        if (hasRole(actor, RoleType.BRANCH_MANAGER)) {
            if (actor.getBranch() == null) {
                throw new BusinessException("Branch Manager is not assigned to a branch");
            }
            return actor.getBranch();
        }
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        if (!branch.getBank().getId().equals(bank.getId())) {
            throw new BusinessException("Branch does not belong to selected bank");
        }
        return branch;
    }

    private boolean canAccess(User actor, Bank bank, Branch branch) {
        if (isPlatform(actor)) {
            return true;
        }
        if (actor.getBank() == null || bank == null || !actor.getBank().getId().equals(bank.getId())) {
            return false;
        }
        return actor.getBranch() == null || branch != null && actor.getBranch().getId().equals(branch.getId());
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerId(),
                customer.getBank().getId(),
                customer.getBank().getCode(),
                customer.getBranch() == null ? null : customer.getBranch().getId(),
                customer.getBranch() == null ? null : customer.getBranch().getCode(),
                customer.getCustomerType(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
