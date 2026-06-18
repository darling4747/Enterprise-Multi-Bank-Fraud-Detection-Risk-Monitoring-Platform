package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.bank.BankCreateRequest;
import com.bank.frauddetection.dto.bank.BankResponse;
import com.bank.frauddetection.entity.Bank;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.BankStatus;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.BankRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {

    private final BankRepository bankRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BankService(BankRepository bankRepository, UserRepository userRepository, AuditService auditService) {
        this.bankRepository = bankRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BankResponse> listBanks() {
        return bankRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public BankResponse createBank(BankCreateRequest request, String actorUsername) {
        User actor = requireRole(actorUsername, RoleType.PLATFORM_ADMIN);
        String code = request.code().trim().toUpperCase();
        if (bankRepository.existsByCode(code)) {
            throw new BusinessException("Bank code already exists");
        }
        String swiftCode = normalizeOptionalUpper(request.swiftCode());
        if (swiftCode != null && bankRepository.existsBySwiftCode(swiftCode)) {
            throw new BusinessException("SWIFT code already exists");
        }
        String licenseNumber = normalizeOptionalUpper(request.licenseNumber());
        if (licenseNumber != null && bankRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BusinessException("License number already exists");
        }
        Bank bank = new Bank();
        bank.setCode(code);
        bank.setName(request.name().trim());
        bank.setHeadOffice(blankToNull(request.headOffice()));
        bank.setHeadOfficeCity(blankToNull(request.headOfficeCity()));
        bank.setHeadOfficeState(blankToNull(request.headOfficeState()));
        bank.setHeadOfficeCountry(blankToNull(request.headOfficeCountry()));
        bank.setSwiftCode(swiftCode);
        bank.setLicenseNumber(licenseNumber);
        bank.setContactEmail(blankToNull(request.contactEmail()));
        bank.setContactPhone(blankToNull(request.contactPhone()));
        bank = bankRepository.save(bank);
        auditService.log(AuditEventType.BANK_CREATED, actor, null, bank.getId(), "Platform Admin created bank " + code, AuditStatus.SUCCESS);
        return toResponse(bank);
    }

    @Transactional
    public BankResponse disableBank(Long id, String actorUsername) {
        User actor = requireRole(actorUsername, RoleType.PLATFORM_ADMIN);
        Bank bank = bankRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + id));
        bank.setStatus(BankStatus.DISABLED);
        bank = bankRepository.save(bank);
        auditService.log(AuditEventType.BANK_DISABLED, actor, null, bank.getId(), "Platform Admin disabled bank " + bank.getCode(), AuditStatus.SUCCESS);
        return toResponse(bank);
    }

    private User requireRole(String username, RoleType roleType) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        if (user.getRoles().stream().noneMatch(role -> role.getName() == roleType || role.getName() == RoleType.SUPER_ADMIN && roleType == RoleType.PLATFORM_ADMIN)) {
            throw new BusinessException("Insufficient privileges");
        }
        return user;
    }

    private BankResponse toResponse(Bank bank) {
        return new BankResponse(
                bank.getId(),
                bank.getCode(),
                bank.getName(),
                bank.getHeadOffice(),
                bank.getHeadOfficeCity(),
                bank.getHeadOfficeState(),
                bank.getHeadOfficeCountry(),
                bank.getSwiftCode(),
                bank.getLicenseNumber(),
                bank.getContactEmail(),
                bank.getContactPhone(),
                bank.getStatus(),
                bank.getCreatedAt(),
                bank.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptionalUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
