package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.auth.AuthResponse;
import com.bank.frauddetection.dto.auth.ChangePasswordRequest;
import com.bank.frauddetection.dto.auth.LoginRequest;
import com.bank.frauddetection.dto.auth.MfaSetupResponse;
import com.bank.frauddetection.dto.auth.MfaVerifyRequest;
import com.bank.frauddetection.dto.auth.ProfileResponse;
import com.bank.frauddetection.dto.auth.ProfileUpdateRequest;
import com.bank.frauddetection.dto.auth.RefreshTokenRequest;
import com.bank.frauddetection.dto.auth.RegisterRequest;
import com.bank.frauddetection.dto.auth.UserSessionResponse;
import com.bank.frauddetection.dto.auth.UserPreferencesResponse;
import com.bank.frauddetection.dto.auth.UserPreferencesUpdateRequest;
import com.bank.frauddetection.entity.Role;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.entity.UserSession;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.PasswordStatus;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.SecurityIncidentType;
import com.bank.frauddetection.enums.UserStatus;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.repository.RoleRepository;
import com.bank.frauddetection.repository.UserRepository;
import com.bank.frauddetection.security.JwtService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final TotpService totpService;
    private final PasswordPolicyService passwordPolicyService;
    private final UserSessionService userSessionService;
    private final SecurityIncidentService securityIncidentService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            AuditService auditService,
            TotpService totpService,
            PasswordPolicyService passwordPolicyService,
            UserSessionService userSessionService,
            SecurityIncidentService securityIncidentService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.totpService = totpService;
        this.passwordPolicyService = passwordPolicyService;
        this.userSessionService = userSessionService;
        this.securityIncidentService = securityIncidentService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        passwordPolicyService.validateStrength(request.password());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(findOrCreateRole(RoleType.FRAUD_ANALYST)));
        user = userRepository.save(user);
        passwordPolicyService.rememberPassword(user);
        auditService.log(AuditEventType.USER_CREATED, null, user, bankId(user), "Self-service analyst registration", AuditStatus.SUCCESS);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        return toAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null, null, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceId, String userAgent, String ipAddress) {
        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElseThrow(() -> new BusinessException("Invalid username or password"));

        if (user.getPasswordStatus() == PasswordStatus.TEMPORARY) {
            auditService.log(AuditEventType.FIRST_LOGIN_ATTEMPT, user, user, bankId(user), "Temporary credential login attempt", AuditStatus.SUCCESS);
            if (user.getTemporaryPasswordExpiresAt() != null && user.getTemporaryPasswordExpiresAt().isBefore(Instant.now())) {
                user.setPasswordStatus(PasswordStatus.EXPIRED);
                userRepository.save(user);
                auditService.log(AuditEventType.TEMP_PASSWORD_EXPIRED, user, user, bankId(user), "Temporary password expired", AuditStatus.FAILURE);
                throw new BusinessException("Temporary password expired. Ask your administrator to reset it.");
            }
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (RuntimeException ex) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setStatus(UserStatus.LOCKED);
                user.setAccountLockedUntil(Instant.now().plusSeconds(900));
                auditService.log(AuditEventType.ACCOUNT_LOCKED, user, user, bankId(user), "Account locked after failed login attempts", AuditStatus.FAILURE);
                securityIncidentService.recordForUser(user, SecurityIncidentType.MULTIPLE_FAILED_LOGINS, RiskLevel.HIGH, "Account locked after repeated failed password attempts");
            }
            userRepository.save(user);
            auditService.log(AuditEventType.LOGIN_FAILED, user, user, bankId(user), "Login failed", AuditStatus.FAILURE);
            throw ex;
        }

        if (user.isMfaEnabled()) {
            if (isBlank(request.mfaCode())) {
                auditService.log(AuditEventType.MFA_CHALLENGE_REQUIRED, user, user, bankId(user), "MFA code required after password verification", AuditStatus.SUCCESS);
                return toAuthResponse(null, user, true);
            }
            if (!totpService.verify(user.getMfaSecret(), request.mfaCode())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setStatus(UserStatus.LOCKED);
                    user.setAccountLockedUntil(Instant.now().plusSeconds(900));
                    auditService.log(AuditEventType.ACCOUNT_LOCKED, user, user, bankId(user), "Account locked after failed MFA attempts", AuditStatus.FAILURE);
                    securityIncidentService.recordForUser(user, SecurityIncidentType.MULTIPLE_MFA_FAILURES, RiskLevel.HIGH, "Account locked after repeated failed MFA attempts");
                }
                userRepository.save(user);
                auditService.log(AuditEventType.MFA_CHALLENGE_FAILED, user, user, bankId(user), "Invalid MFA code", AuditStatus.FAILURE);
                throw new BusinessException("Invalid MFA code");
            }
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        if (user.getStatus() == UserStatus.LOCKED && user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isBefore(Instant.now())) {
            user.setStatus(UserStatus.ACTIVE);
            user.setAccountLockedUntil(null);
        }
        userRepository.save(user);
        auditService.log(AuditEventType.LOGIN_SUCCESS, user, user, bankId(user), "Login success", AuditStatus.SUCCESS);
        UserSessionService.CreatedSession createdSession = userSessionService.createSession(user, deviceId, userAgent, ipAddress);
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()), createdSession.session().getId());
        return toAuthResponse(token, user, false, createdSession.refreshToken(), createdSession.session().getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()));
        return toAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        UserSession session = userSessionService.useRefreshToken(request.refreshToken());
        User user = session.getUser();
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()), session.getId());
        return toAuthResponse(token, user, false, request.refreshToken(), session.getId());
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> sessions(String username, Long currentSessionId) {
        User user = findUser(username);
        return userSessionService.listSessions(user, currentSessionId);
    }

    @Transactional
    public void logoutOtherSessions(String username, Long currentSessionId) {
        User user = findUser(username);
        userSessionService.revokeOtherSessions(user, currentSessionId);
    }

    public Long currentSessionId(String accessToken) {
        return jwtService.extractSessionId(accessToken);
    }

    @Transactional
    public MfaSetupResponse startMfaSetup(String username) {
        User user = findUser(username);
        if (user.isMfaEnabled()) {
            throw new BusinessException("MFA is already enabled");
        }
        String secret;
        do {
            secret = totpService.generateSecret();
        } while (userRepository.existsByMfaSecret(secret));
        user.setMfaSecret(secret);
        userRepository.save(user);
        String otpAuthUri = totpService.otpAuthUri(user.getUsername(), secret);
        auditService.log(AuditEventType.MFA_SETUP_STARTED, user, user, bankId(user), "MFA setup started", AuditStatus.SUCCESS);
        return new MfaSetupResponse(secret, otpAuthUri, totpService.qrCodeDataUri(otpAuthUri));
    }

    @Transactional
    public AuthResponse verifyMfaSetup(String username, MfaVerifyRequest request) {
        User user = findUser(username);
        if (isBlank(user.getMfaSecret())) {
            throw new BusinessException("Start MFA setup before verifying a code");
        }
        if (!totpService.verify(user.getMfaSecret(), request.code())) {
            auditService.log(AuditEventType.MFA_CHALLENGE_FAILED, user, user, bankId(user), "MFA setup rejected: invalid code", AuditStatus.FAILURE);
            throw new BusinessException("Invalid MFA code");
        }
        user.setMfaEnabled(true);
        user.setMfaEnabledAt(Instant.now());
        user = userRepository.save(user);
        auditService.log(AuditEventType.MFA_ENABLED, user, user, bankId(user), "MFA enabled", AuditStatus.SUCCESS);
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()));
        return toAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse disableMfa(String username, MfaVerifyRequest request) {
        User user = findUser(username);
        if (!user.isMfaEnabled()) {
            String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()));
            return toAuthResponse(token, user);
        }
        if (!totpService.verify(user.getMfaSecret(), request.code())) {
            auditService.log(AuditEventType.MFA_CHALLENGE_FAILED, user, user, bankId(user), "MFA disable rejected: invalid code", AuditStatus.FAILURE);
            throw new BusinessException("Invalid MFA code");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaEnabledAt(null);
        user = userRepository.save(user);
        auditService.log(AuditEventType.MFA_DISABLED, user, user, bankId(user), "MFA disabled", AuditStatus.SUCCESS);
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()));
        return toAuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(String username) {
        User user = findUser(username);
        return new ProfileResponse(user.getFullName(), user.getEmail(), user.getProfilePhotoDataUrl());
    }

    @Transactional
    public ProfileResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = findUser(username);
        Long userId = user.getId();
        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new BusinessException("Email already exists");
                });
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setProfilePhotoDataUrl(isBlank(request.profilePhotoDataUrl()) ? null : request.profilePhotoDataUrl());
        user = userRepository.save(user);
        auditService.log(AuditEventType.USER_UPDATED, user, user, bankId(user), "User updated their profile", AuditStatus.SUCCESS);
        return new ProfileResponse(user.getFullName(), user.getEmail(), user.getProfilePhotoDataUrl());
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse preferences(String username) {
        User user = findUser(username);
        return toPreferencesResponse(user);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(String username, UserPreferencesUpdateRequest request) {
        User user = findUser(username);
        user.setCriticalAlertEmails(request.criticalAlertEmails());
        user.setDailySummaryReport(request.dailySummaryReport());
        user.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
        user = userRepository.save(user);
        auditService.log(AuditEventType.USER_UPDATED, user, user, bankId(user), "User updated notification preferences", AuditStatus.SUCCESS);
        return toPreferencesResponse(user);
    }

    @Transactional
    public AuthResponse changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditService.log(AuditEventType.LOGIN_FAILED, user, user, bankId(user), "Password change rejected: current password mismatch", AuditStatus.FAILURE);
            throw new BusinessException("Current password is incorrect");
        }
        passwordPolicyService.validateNewPassword(user, request.newPassword());
        String previousPasswordHash = user.getPasswordHash();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordStatus(PasswordStatus.PERMANENT);
        user.setMustChangePassword(false);
        user.setTemporaryPasswordExpiresAt(null);
        user.setVisibleTemporaryPassword(null);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        passwordPolicyService.rememberPasswordHash(user, previousPasswordHash);
        auditService.log(AuditEventType.PASSWORD_CHANGED, user, user, bankId(user), "User changed password", AuditStatus.SUCCESS);
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getUsername()));
        return toAuthResponse(token, user);
    }

    private Role findOrCreateRole(RoleType roleType) {
        return roleRepository.findByName(roleType).orElseGet(() -> roleRepository.save(new Role(roleType)));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private AuthResponse toAuthResponse(String token, User user) {
        return toAuthResponse(token, user, false, null, null);
    }

    private AuthResponse toAuthResponse(String token, User user, boolean mfaRequired) {
        return toAuthResponse(token, user, mfaRequired, null, null);
    }

    private AuthResponse toAuthResponse(String token, User user, boolean mfaRequired, String refreshToken, Long sessionId) {
        return new AuthResponse(
                token,
                "Bearer",
                refreshToken,
                sessionId,
                token == null ? null : jwtService.expiresAtFromNow(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                bankId(user),
                user.isMustChangePassword(),
                user.isMfaEnabled(),
                mfaRequired
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UserPreferencesResponse toPreferencesResponse(User user) {
        return new UserPreferencesResponse(
                user.isCriticalAlertEmails(),
                user.isDailySummaryReport(),
                user.getSessionTimeoutMinutes(),
                user.isMfaEnabled()
        );
    }

    private Long bankId(User user) {
        return user.getBank() == null ? null : user.getBank().getId();
    }
}
