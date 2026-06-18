package com.bank.frauddetection.entity;

import com.bank.frauddetection.enums.BankStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "banks")
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 160)
    private String headOffice;

    @Column(length = 120)
    private String headOfficeCity;

    @Column(length = 120)
    private String headOfficeState;

    @Column(length = 120)
    private String headOfficeCountry;

    @Column(unique = true, length = 40)
    private String swiftCode;

    @Column(unique = true, length = 80)
    private String licenseNumber;

    @Column(length = 160)
    private String contactEmail;

    @Column(length = 40)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BankStatus status = BankStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadOffice() {
        return headOffice;
    }

    public void setHeadOffice(String headOffice) {
        this.headOffice = headOffice;
    }

    public String getHeadOfficeCity() {
        return headOfficeCity;
    }

    public void setHeadOfficeCity(String headOfficeCity) {
        this.headOfficeCity = headOfficeCity;
    }

    public String getHeadOfficeState() {
        return headOfficeState;
    }

    public void setHeadOfficeState(String headOfficeState) {
        this.headOfficeState = headOfficeState;
    }

    public String getHeadOfficeCountry() {
        return headOfficeCountry;
    }

    public void setHeadOfficeCountry(String headOfficeCountry) {
        this.headOfficeCountry = headOfficeCountry;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public BankStatus getStatus() {
        return status;
    }

    public void setStatus(BankStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
