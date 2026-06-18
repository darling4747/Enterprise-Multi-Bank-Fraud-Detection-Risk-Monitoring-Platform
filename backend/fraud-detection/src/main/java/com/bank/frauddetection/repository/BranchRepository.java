package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByBankId(Long bankId);

    Optional<Branch> findByBankIdAndCode(Long bankId, String code);

    boolean existsByBankIdAndCode(Long bankId, String code);

    boolean existsByIfscCode(String ifscCode);
}
