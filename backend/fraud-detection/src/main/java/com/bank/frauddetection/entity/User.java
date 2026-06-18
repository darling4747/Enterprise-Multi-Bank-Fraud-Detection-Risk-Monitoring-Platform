package com.bank.frauddetection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import com.bank.frauddetection.enums.PasswordStatus;
import com.bank.frauddetection.enums.UserStatus;

@Entity
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 140)
    private String fullName;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(length = 80)
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PasswordStatus passwordStatus = PasswordStatus.PERMANENT;

    private boolean mustChangePassword = false;

    private Instant temporaryPasswordExpiresAt;

    @Column(length = 160)
    private String visibleTemporaryPassword;

    private Instant lastLoginAt;

    private int failedLoginAttempts = 0;

    private Instant accountLockedUntil;

    private Boolean mfaEnabled = false;

    @Column(length = 80)
    private String mfaSecret;

    private Instant mfaEnabledAt;

    @Column(columnDefinition = "text")
    private String profilePhotoDataUrl;

    private Boolean criticalAlertEmails = true;

    private Boolean dailySummaryReport = false;

    private Integer sessionTimeoutMinutes = 30;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public PasswordStatus getPasswordStatus() {
        return passwordStatus;
    }

    public void setPasswordStatus(PasswordStatus passwordStatus) {
        this.passwordStatus = passwordStatus;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Instant getTemporaryPasswordExpiresAt() {
        return temporaryPasswordExpiresAt;
    }

    public void setTemporaryPasswordExpiresAt(Instant temporaryPasswordExpiresAt) {
        this.temporaryPasswordExpiresAt = temporaryPasswordExpiresAt;
    }

    public String getVisibleTemporaryPassword() {
        return visibleTemporaryPassword;
    }

    public void setVisibleTemporaryPassword(String visibleTemporaryPassword) {
        this.visibleTemporaryPassword = visibleTemporaryPassword;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getAccountLockedUntil() {
        return accountLockedUntil;
    }

    public void setAccountLockedUntil(Instant accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    public boolean isMfaEnabled() {
        return Boolean.TRUE.equals(mfaEnabled);
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public Instant getMfaEnabledAt() {
        return mfaEnabledAt;
    }

    public void setMfaEnabledAt(Instant mfaEnabledAt) {
        this.mfaEnabledAt = mfaEnabledAt;
    }

    public String getProfilePhotoDataUrl() {
        return profilePhotoDataUrl;
    }

    public void setProfilePhotoDataUrl(String profilePhotoDataUrl) {
        this.profilePhotoDataUrl = profilePhotoDataUrl;
    }

    public boolean isCriticalAlertEmails() {
        return !Boolean.FALSE.equals(criticalAlertEmails);
    }

    public void setCriticalAlertEmails(boolean criticalAlertEmails) {
        this.criticalAlertEmails = criticalAlertEmails;
    }

    public boolean isDailySummaryReport() {
        return Boolean.TRUE.equals(dailySummaryReport);
    }

    public void setDailySummaryReport(boolean dailySummaryReport) {
        this.dailySummaryReport = dailySummaryReport;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes == null ? 30 : sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
