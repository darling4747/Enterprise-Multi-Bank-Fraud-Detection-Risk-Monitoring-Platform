package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Bank;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, Long> {
    Optional<Bank> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsBySwiftCode(String swiftCode);

    boolean existsByLicenseNumber(String licenseNumber);
}
