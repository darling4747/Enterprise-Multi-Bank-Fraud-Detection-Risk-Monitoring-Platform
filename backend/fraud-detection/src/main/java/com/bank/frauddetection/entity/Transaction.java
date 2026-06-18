package com.bank.frauddetection.entity;

import com.bank.frauddetection.enums.FraudDecision;
import com.bank.frauddetection.enums.AccountType;
import com.bank.frauddetection.enums.CustomerType;
import com.bank.frauddetection.enums.DailyTransactionPattern;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String reference = "TXN-" + UUID.randomUUID();

    @Column(nullable = false, length = 80)
    private String customerId;

    @Column(nullable = false, length = 80)
    private String sourceAccount;

    @Column(nullable = false, length = 80)
    private String destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(length = 80)
    private String merchantCategory;

    @Column(length = 80)
    private String country;

    @Column(length = 80)
    private String ipAddress;

    @Column(length = 120)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountType accountType = AccountType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerType customerType = CustomerType.RETAIL;

    @Column(nullable = false)
    private boolean beneficiaryTrusted = false;

    @Column(nullable = false)
    private boolean knownDevice = false;

    @Column(nullable = false)
    private boolean knownLocation = false;

    private Integer transactionHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DailyTransactionPattern dailyTransactionPattern = DailyTransactionPattern.NORMAL;

    @Column(length = 40)
    private String transactionType;

    private Integer step;

    @Column(precision = 19, scale = 2)
    private BigDecimal oldbalanceOrg;

    @Column(precision = 19, scale = 2)
    private BigDecimal newbalanceOrig;

    @Column(precision = 19, scale = 2)
    private BigDecimal oldbalanceDest;

    @Column(precision = 19, scale = 2)
    private BigDecimal newbalanceDest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudDecision fraudDecision = FraudDecision.APPROVE;

    @Column(nullable = false)
    private int riskScore = 0;

    @Column(length = 1000)
    private String riskSummary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant processedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(String merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType == null ? AccountType.INDIVIDUAL : accountType;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType == null ? CustomerType.RETAIL : customerType;
    }

    public boolean isBeneficiaryTrusted() {
        return beneficiaryTrusted;
    }

    public void setBeneficiaryTrusted(boolean beneficiaryTrusted) {
        this.beneficiaryTrusted = beneficiaryTrusted;
    }

    public boolean isKnownDevice() {
        return knownDevice;
    }

    public void setKnownDevice(boolean knownDevice) {
        this.knownDevice = knownDevice;
    }

    public boolean isKnownLocation() {
        return knownLocation;
    }

    public void setKnownLocation(boolean knownLocation) {
        this.knownLocation = knownLocation;
    }

    public Integer getTransactionHour() {
        return transactionHour;
    }

    public void setTransactionHour(Integer transactionHour) {
        this.transactionHour = transactionHour;
    }

    public DailyTransactionPattern getDailyTransactionPattern() {
        return dailyTransactionPattern;
    }

    public void setDailyTransactionPattern(DailyTransactionPattern dailyTransactionPattern) {
        this.dailyTransactionPattern = dailyTransactionPattern == null ? DailyTransactionPattern.NORMAL : dailyTransactionPattern;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public BigDecimal getOldbalanceOrg() {
        return oldbalanceOrg;
    }

    public void setOldbalanceOrg(BigDecimal oldbalanceOrg) {
        this.oldbalanceOrg = oldbalanceOrg;
    }

    public BigDecimal getNewbalanceOrig() {
        return newbalanceOrig;
    }

    public void setNewbalanceOrig(BigDecimal newbalanceOrig) {
        this.newbalanceOrig = newbalanceOrig;
    }

    public BigDecimal getOldbalanceDest() {
        return oldbalanceDest;
    }

    public void setOldbalanceDest(BigDecimal oldbalanceDest) {
        this.oldbalanceDest = oldbalanceDest;
    }

    public BigDecimal getNewbalanceDest() {
        return newbalanceDest;
    }

    public void setNewbalanceDest(BigDecimal newbalanceDest) {
        this.newbalanceDest = newbalanceDest;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public FraudDecision getFraudDecision() {
        return fraudDecision;
    }

    public void setFraudDecision(FraudDecision fraudDecision) {
        this.fraudDecision = fraudDecision;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(String riskSummary) {
        this.riskSummary = riskSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
