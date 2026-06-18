package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByAccountAccountNumberAndBeneficiaryAccount(String accountNumber, String beneficiaryAccount);

    List<Beneficiary> findByAccountId(Long accountId);

    List<Beneficiary> findByAccountBankId(Long bankId);

    List<Beneficiary> findByAccountBranchId(Long branchId);
}
