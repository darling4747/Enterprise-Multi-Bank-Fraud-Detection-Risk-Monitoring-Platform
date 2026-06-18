package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByCustomerId(String customerId);

    Optional<Customer> findByCustomerId(String customerId);

    List<Customer> findByBankId(Long bankId);

    List<Customer> findByBranchId(Long branchId);
}
