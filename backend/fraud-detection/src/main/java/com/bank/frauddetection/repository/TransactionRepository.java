package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.enums.RiskLevel;
import com.bank.frauddetection.enums.TransactionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReference(String reference);

    List<Transaction> findTop10ByOrderByCreatedAtDesc();

    long countByStatus(TransactionStatus status);

    long countByRiskLevel(RiskLevel riskLevel);

    @Query("""
            select t from Transaction t
            where (:customerId is null or t.customerId = :customerId)
              and (:status is null or t.status = :status)
              and (:riskLevel is null or t.riskLevel = :riskLevel)
              and (:bankId is null or t.bank.id = :bankId)
              and (:branchId is null or t.branch.id = :branchId)
              and (:fromDate is null or t.createdAt >= :fromDate)
              and (:toDate is null or t.createdAt <= :toDate)
            order by t.createdAt desc
            """)
    List<Transaction> search(
            @Param("customerId") String customerId,
            @Param("status") TransactionStatus status,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("bankId") Long bankId,
            @Param("branchId") Long branchId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.createdAt >= :fromDate")
    java.math.BigDecimal sumAmountSince(@Param("fromDate") Instant fromDate);
}
