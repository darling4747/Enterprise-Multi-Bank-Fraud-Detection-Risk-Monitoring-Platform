package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.UserPasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {
    List<UserPasswordHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
