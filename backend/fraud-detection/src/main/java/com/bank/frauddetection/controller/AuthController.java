package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.auth.AuthResponse;
import com.bank.frauddetection.dto.auth.ChangePasswordRequest;
import com.bank.frauddetection.dto.auth.LoginRequest;
import com.bank.frauddetection.dto.auth.MfaSetupResponse;
import com.bank.frauddetection.dto.auth.MfaVerifyRequest;
import com.bank.frauddetection.dto.auth.ProfileResponse;
import com.bank.frauddetection.dto.auth.ProfileUpdateRequest;
import com.bank.frauddetection.dto.auth.RefreshTokenRequest;
import com.bank.frauddetection.dto.auth.UserSessionResponse;
import com.bank.frauddetection.dto.auth.UserPreferencesResponse;
import com.bank.frauddetection.dto.auth.UserPreferencesUpdateRequest;
import com.bank.frauddetection.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, deviceId(httpRequest), httpRequest.getHeader("User-Agent"), ipAddress(httpRequest));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public AuthResponse me(Principal principal) {
        return authService.currentUser(principal.getName());
    }

    @PostMapping("/change-password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest request, Principal principal) {
        return authService.changePassword(principal.getName(), request);
    }

    @PostMapping("/mfa/setup")
    public MfaSetupResponse startMfaSetup(Principal principal) {
        return authService.startMfaSetup(principal.getName());
    }

    @PostMapping("/mfa/verify")
    public AuthResponse verifyMfaSetup(@Valid @RequestBody MfaVerifyRequest request, Principal principal) {
        return authService.verifyMfaSetup(principal.getName(), request);
    }

    @PostMapping("/mfa/disable")
    public AuthResponse disableMfa(@Valid @RequestBody MfaVerifyRequest request, Principal principal) {
        return authService.disableMfa(principal.getName(), request);
    }

    @GetMapping("/sessions")
    public List<UserSessionResponse> sessions(Principal principal, HttpServletRequest request) {
        return authService.sessions(principal.getName(), currentSessionId(request));
    }

    @PostMapping("/sessions/logout-others")
    public void logoutOtherSessions(Principal principal, HttpServletRequest request) {
        authService.logoutOtherSessions(principal.getName(), currentSessionId(request));
    }

    @GetMapping("/profile")
    public ProfileResponse profile(Principal principal) {
        return authService.profile(principal.getName());
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Principal principal) {
        return authService.updateProfile(principal.getName(), request);
    }

    @GetMapping("/preferences")
    public UserPreferencesResponse preferences(Principal principal) {
        return authService.preferences(principal.getName());
    }

    @PutMapping("/preferences")
    public UserPreferencesResponse updatePreferences(@Valid @RequestBody UserPreferencesUpdateRequest request, Principal principal) {
        return authService.updatePreferences(principal.getName(), request);
    }

    private String deviceId(HttpServletRequest request) {
        String value = request.getHeader("X-Device-Id");
        return value == null || value.isBlank() ? "browser-session" : value;
    }

    private String ipAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Long currentSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authService.currentSessionId(authHeader.substring(7));
    }
}
