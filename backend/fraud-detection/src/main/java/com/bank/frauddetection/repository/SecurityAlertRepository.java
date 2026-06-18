package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.SecurityAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findByBankIdOrderByCreatedAtDesc(Long bankId);

    List<SecurityAlert> findByBranchIdOrderByCreatedAtDesc(Long branchId);
}
