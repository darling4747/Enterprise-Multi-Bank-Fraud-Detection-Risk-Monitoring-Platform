package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Alert;
import com.bank.frauddetection.enums.AlertStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findTop10ByOrderByCreatedAtDesc();

    List<Alert> findByFraudCaseId(Long fraudCaseId);

    long countByStatus(AlertStatus status);

    void deleteByTransactionId(Long transactionId);

    @Query("""
            select a from Alert a
            where (:status is null or a.status = :status)
              and (:bankId is null or a.transaction.bank.id = :bankId)
              and (:branchId is null or a.transaction.branch.id = :branchId)
            order by a.createdAt desc
            """)
    List<Alert> search(
            @Param("status") AlertStatus status,
            @Param("bankId") Long bankId,
            @Param("branchId") Long branchId
    );
}
