package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.user.UserCreateRequest;
import com.bank.frauddetection.dto.user.TemporaryCredentialResponse;
import com.bank.frauddetection.dto.user.UserResponse;
import com.bank.frauddetection.dto.user.UserRoleUpdateRequest;
import com.bank.frauddetection.entity.Bank;
import com.bank.frauddetection.entity.Branch;
import com.bank.frauddetection.entity.Role;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.PasswordStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.UserStatus;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.BankRepository;
import com.bank.frauddetection.repository.BranchRepository;
import com.bank.frauddetection.repository.RoleRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordPolicyService passwordPolicyService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BankRepository bankRepository,
            BranchRepository branchRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            PasswordPolicyService passwordPolicyService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bankRepository = bankRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String actorUsername) {
        User actor = findUser(actorUsername);
        if (hasRole(actor, RoleType.PLATFORM_ADMIN) || hasRole(actor, RoleType.SUPER_ADMIN)) {
            return listUsers();
        }
        if (hasRole(actor, RoleType.BANK_ADMIN) && actor.getBank() != null) {
            return userRepository.findByBankId(actor.getBank().getId()).stream()
                    .filter(this::isEmployeeUser)
                    .map(this::toResponse)
                    .toList();
        }
        throw new BusinessException("Insufficient privileges");
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        return createUser(request, null);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        if (actor != null && !hasRole(actor, RoleType.PLATFORM_ADMIN) && !hasRole(actor, RoleType.SUPER_ADMIN)) {
            throw new BusinessException("Only Platform Admin can use the generic user creation endpoint");
        }
        User user = createUserEntity(request, actor, request.password(), false);
        user = userRepository.save(user);
        passwordPolicyService.rememberPassword(user);
        auditService.log(AuditEventType.USER_CREATED, actor, user, bankId(user), "User account created", AuditStatus.SUCCESS);
        auditService.log(AuditEventType.ROLE_ASSIGNED, actor, user, bankId(user), "Initial roles assigned", AuditStatus.SUCCESS);
        return toResponse(user);
    }

    @Transactional
    public TemporaryCredentialResponse createBankAdmin(UserCreateRequest request, String actorUsername) {
        User actor = requireRole(actorUsername, RoleType.PLATFORM_ADMIN);
        if (request.bankId() == null) {
            throw new BusinessException("Bank id is required for a bank admin");
        }
        UserCreateRequest normalized = new UserCreateRequest(
                request.username(),
                request.email(),
                request.password(),
                request.fullName(),
                request.bankId(),
                request.branchId(),
                request.employeeId(),
                Set.of(RoleType.BANK_ADMIN),
                true
        );
        String temporaryPassword = temporaryPassword();
        User user = createUserEntity(normalized, actor, temporaryPassword, true);
        user = userRepository.save(user);
        passwordPolicyService.rememberPassword(user);
        auditService.log(AuditEventType.USER_CREATED, actor, user, bankId(user), "Platform Admin created Bank Admin account", AuditStatus.SUCCESS);
        auditService.log(AuditEventType.TEMP_PASSWORD_GENERATED, actor, user, bankId(user), "Temporary password generated for Bank Admin", AuditStatus.SUCCESS);
        return new TemporaryCredentialResponse(toResponse(user), temporaryPassword, user.getTemporaryPasswordExpiresAt());
    }

    @Transactional
    public TemporaryCredentialResponse createEmployee(UserCreateRequest request, String actorUsername) {
        User actor = requireRole(actorUsername, RoleType.BANK_ADMIN);
        if (actor.getBank() == null) {
            throw new BusinessException("Bank Admin is not assigned to a bank");
        }
        Set<RoleType> requestedRoles = request.roles();
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new BusinessException("At least one employee role is required");
        }
        if (!allowedEmployeeRoles().containsAll(requestedRoles)) {
            throw new BusinessException("Bank Admin can only create employee roles");
        }
        UserCreateRequest normalized = new UserCreateRequest(
                request.username(),
                request.email(),
                request.password(),
                request.fullName(),
                actor.getBank().getId(),
                request.branchId(),
                request.employeeId(),
                requestedRoles,
                true
        );
        String temporaryPassword = temporaryPassword();
        User user = createUserEntity(normalized, actor, temporaryPassword, true);
        user = userRepository.save(user);
        passwordPolicyService.rememberPassword(user);
        auditService.log(AuditEventType.USER_CREATED, actor, user, bankId(user), "Bank Admin created employee account", AuditStatus.SUCCESS);
        auditService.log(AuditEventType.TEMP_PASSWORD_GENERATED, actor, user, bankId(user), "Temporary password generated for employee", AuditStatus.SUCCESS);
        return new TemporaryCredentialResponse(toResponse(user), temporaryPassword, user.getTemporaryPasswordExpiresAt());
    }

    private User createUserEntity(UserCreateRequest request, User actor, String rawPassword, boolean temporaryPassword) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException("Password is required");
        }
        passwordPolicyService.validateStrength(rawPassword);
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setEnabled(request.enabled());
        user.setBank(resolveBank(request.bankId()));
        user.setBranch(resolveBranch(request.branchId(), user.getBank()));
        user.setEmployeeId(request.employeeId());
        user.setCreatedBy(actor);
        user.setStatus(request.enabled() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        if (temporaryPassword) {
            user.setPasswordStatus(PasswordStatus.TEMPORARY);
            user.setMustChangePassword(true);
            user.setTemporaryPasswordExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            user.setVisibleTemporaryPassword(rawPassword);
        }
        user.setRoles(resolveRoles(request.roles()));
        return user;
    }

    @Transactional
    public UserResponse updateRoles(Long id, UserRoleUpdateRequest request) {
        return updateRoles(id, request, null);
    }

    @Transactional
    public UserResponse updateRoles(Long id, UserRoleUpdateRequest request, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (actor != null && !canAdministerUser(actor, user)) {
            throw new BusinessException("Insufficient privileges to change this user's roles");
        }
        if (actor != null && hasRole(actor, RoleType.BANK_ADMIN) && !allowedEmployeeRoles().containsAll(request.roles())) {
            throw new BusinessException("Bank Admin can only assign employee roles");
        }
        user.setRoles(resolveRoles(request.roles()));
        user = userRepository.save(user);
        auditService.log(AuditEventType.ROLE_CHANGED, actor, user, bankId(user), "User roles changed", AuditStatus.SUCCESS);
        return toResponse(user);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        return setEnabled(id, enabled, null);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (actor != null && !canAdministerUser(actor, user)) {
            throw new BusinessException("Insufficient privileges to update this user");
        }
        user.setEnabled(enabled);
        user.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user = userRepository.save(user);
        if (!enabled) {
            auditService.log(AuditEventType.USER_DEACTIVATED, actor, user, bankId(user), "User deactivated", AuditStatus.SUCCESS);
        } else {
            auditService.log(AuditEventType.USER_UPDATED, actor, user, bankId(user), "User account activated", AuditStatus.SUCCESS);
        }
        return toResponse(user);
    }

    @Transactional
    public TemporaryCredentialResponse resetPasswordByIssuer(Long id, String actorUsername) {
        User actor = findUser(actorUsername);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (!canResetPassword(actor, target)) {
            throw new BusinessException("Only the credential issuer can reset this password");
        }
        String temporaryPassword = temporaryPassword();
        String previousPasswordHash = target.getPasswordHash();
        target.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        target.setPasswordStatus(PasswordStatus.TEMPORARY);
        target.setMustChangePassword(true);
        target.setTemporaryPasswordExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        target.setVisibleTemporaryPassword(temporaryPassword);
        target.setFailedLoginAttempts(0);
        target.setAccountLockedUntil(null);
        target.setEnabled(true);
        target.setStatus(UserStatus.ACTIVE);
        target = userRepository.save(target);
        passwordPolicyService.rememberPasswordHash(target, previousPasswordHash);
        auditService.log(AuditEventType.PASSWORD_RESET_BY_ADMIN, actor, target, bankId(target), "Credential issuer reset temporary password", AuditStatus.SUCCESS);
        return new TemporaryCredentialResponse(toResponse(target), temporaryPassword, target.getTemporaryPasswordExpiresAt());
    }

    @Transactional
    public UserResponse unlockUser(Long id, String actorUsername) {
        User actor = findUser(actorUsername);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (!canAdministerUser(actor, target)) {
            throw new BusinessException("Insufficient privileges to unlock this user");
        }
        target.setFailedLoginAttempts(0);
        target.setAccountLockedUntil(null);
        target.setStatus(UserStatus.ACTIVE);
        target.setEnabled(true);
        target = userRepository.save(target);
        auditService.log(AuditEventType.ACCOUNT_UNLOCKED, actor, target, bankId(target), "User account unlocked", AuditStatus.SUCCESS);
        return toResponse(target);
    }

    @Transactional
    public void deleteUser(Long id) {
        deleteUser(id, null);
    }

    @Transactional
    public void deleteUser(Long id, String actorUsername) {
        User actor = actorUsername == null ? null : findUser(actorUsername);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (actor != null && !canAdministerUser(actor, target)) {
            throw new BusinessException("Insufficient privileges to delete this user");
        }
        userRepository.delete(target);
    }

    private Set<Role> resolveRoles(Set<RoleType> roles) {
        return roles.stream()
                .map(roleType -> roleRepository.findByName(roleType).orElseGet(() -> roleRepository.save(new Role(roleType))))
                .collect(Collectors.toSet());
    }

    private EnumSet<RoleType> allowedEmployeeRoles() {
        return EnumSet.of(RoleType.BRANCH_MANAGER, RoleType.FRAUD_ANALYST, RoleType.RISK_OFFICER, RoleType.AUDITOR);
    }

    private Bank resolveBank(Long bankId) {
        if (bankId == null) {
            return null;
        }
        return bankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + bankId));
    }

    private Branch resolveBranch(Long branchId, Bank bank) {
        if (branchId == null) {
            return null;
        }
        if (bank == null) {
            throw new BusinessException("Bank is required when assigning a branch");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        if (!branch.getBank().getId().equals(bank.getId())) {
            throw new BusinessException("Branch does not belong to the selected bank");
        }
        return branch;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private User requireRole(String username, RoleType roleType) {
        User user = findUser(username);
        if (user.getRoles().stream().noneMatch(role -> role.getName() == roleType || role.getName() == RoleType.SUPER_ADMIN && roleType == RoleType.PLATFORM_ADMIN)) {
            throw new BusinessException("Insufficient privileges");
        }
        return user;
    }

    private boolean canResetPassword(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            return true;
        }
        if (hasRole(actor, RoleType.BANK_ADMIN) && actor.getBank() != null && target.getBank() != null) {
            return actor.getBank().getId().equals(target.getBank().getId()) && isEmployeeUser(target);
        }
        return target.getCreatedBy() != null && actor.getId().equals(target.getCreatedBy().getId());
    }

    private boolean canAdministerUser(User actor, User target) {
        if (hasRole(actor, RoleType.PLATFORM_ADMIN) || hasRole(actor, RoleType.SUPER_ADMIN)) {
            return true;
        }
        if (hasRole(actor, RoleType.BANK_ADMIN) && actor.getBank() != null && target.getBank() != null) {
            return actor.getBank().getId().equals(target.getBank().getId()) && isEmployeeUser(target);
        }
        return target.getCreatedBy() != null && actor.getId().equals(target.getCreatedBy().getId());
    }

    private boolean hasRole(User user, RoleType roleType) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleType);
    }

    private boolean isEmployeeUser(User user) {
        Set<RoleType> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return !roles.isEmpty() && allowedEmployeeRoles().containsAll(roles);
    }

    private String temporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        StringBuilder builder = new StringBuilder("Tmp@7Aa");
        for (int i = 0; i < 10; i++) {
            builder.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private Long bankId(User user) {
        return user.getBank() == null ? null : user.getBank().getId();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBank() == null ? null : user.getBank().getId(),
                user.getBank() == null ? null : user.getBank().getCode(),
                user.getBranch() == null ? null : user.getBranch().getId(),
                user.getBranch() == null ? null : user.getBranch().getCode(),
                user.getEmployeeId(),
                user.isEnabled(),
                user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedBy() == null ? null : user.getCreatedBy().getId(),
                user.getCreatedAt(),
                user.getPasswordStatus() == null ? PasswordStatus.PERMANENT : user.getPasswordStatus(),
                user.isMustChangePassword(),
                user.getTemporaryPasswordExpiresAt(),
                user.getVisibleTemporaryPassword(),
                user.getLastLoginAt(),
                user.getFailedLoginAttempts(),
                user.getAccountLockedUntil()
        );
    }
}
