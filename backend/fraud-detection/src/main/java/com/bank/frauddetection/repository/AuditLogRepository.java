package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByTimestampDesc();

    List<AuditLog> findTop50ByBankIdOrderByTimestampDesc(Long bankId);
}
