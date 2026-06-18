package com.bank.frauddetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.frauddetection.dto.auth.LoginRequest;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.PasswordStatus;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.repository.RoleRepository;
import com.bank.frauddetection.repository.UserRepository;
import com.bank.frauddetection.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private TotpService totpService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private SecurityIncidentService securityIncidentService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                userDetailsService,
                jwtService,
                auditService,
                totpService,
                passwordPolicyService,
                userSessionService,
                securityIncidentService
        );
    }

    @Test
    void loginExpiresTemporaryCredentialAndWritesAuditLog() {
        User user = new User();
        user.setUsername("temp-admin");
        user.setEmail("temp-admin@example.com");
        user.setFullName("Temporary Admin");
        user.setPasswordStatus(PasswordStatus.TEMPORARY);
        user.setMustChangePassword(true);
        user.setTemporaryPasswordExpiresAt(Instant.now().minusSeconds(60));

        when(userRepository.findByUsername("temp-admin")).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(new LoginRequest("temp-admin", "Temp@12345678", null), "device", "agent", "127.0.0.1")
        );

        assertEquals("Temporary password expired. Ask your administrator to reset it.", exception.getMessage());
        assertEquals(PasswordStatus.EXPIRED, user.getPasswordStatus());
        verify(userRepository).save(user);
        verify(auditService).log(
                eq(AuditEventType.FIRST_LOGIN_ATTEMPT),
                eq(user),
                eq(user),
                isNull(),
                eq("Temporary credential login attempt"),
                eq(AuditStatus.SUCCESS)
        );
        verify(auditService).log(
                eq(AuditEventType.TEMP_PASSWORD_EXPIRED),
                eq(user),
                eq(user),
                isNull(),
                eq("Temporary password expired"),
                eq(AuditStatus.FAILURE)
        );
        verifyNoInteractions(authenticationManager);
    }
}
