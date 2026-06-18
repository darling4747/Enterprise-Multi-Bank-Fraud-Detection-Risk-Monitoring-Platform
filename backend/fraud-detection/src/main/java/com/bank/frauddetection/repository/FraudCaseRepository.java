package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.FraudCase;
import com.bank.frauddetection.enums.FraudCaseStatus;
import com.bank.frauddetection.enums.RiskLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {
    Optional<FraudCase> findByTransactionId(Long transactionId);

    List<FraudCase> findTop10ByOrderByCreatedAtDesc();

    long countByStatus(FraudCaseStatus status);

    long countByRiskLevel(RiskLevel riskLevel);

    @Query("""
            select f from FraudCase f
            where (:bankId is null or f.transaction.bank.id = :bankId)
              and (:branchId is null or f.transaction.branch.id = :branchId)
            order by f.createdAt desc
            """)
    List<FraudCase> search(
            @Param("bankId") Long bankId,
            @Param("branchId") Long branchId
    );
}
