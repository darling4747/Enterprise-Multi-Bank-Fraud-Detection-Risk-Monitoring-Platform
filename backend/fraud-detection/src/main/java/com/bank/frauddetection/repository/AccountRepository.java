package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByBankId(Long bankId);

    List<Account> findByBranchId(Long branchId);
}
