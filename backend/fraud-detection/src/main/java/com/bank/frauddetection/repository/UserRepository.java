package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByMfaSecret(String mfaSecret);

    List<User> findByBankId(Long bankId);

    @Query("""
            select count(u) > 0
            from User u
            join u.roles r
            where r.name = :role
              and u.bank.id = :bankId
              and u.enabled = true
              and (u.status is null or u.status <> :inactiveStatus)
              and (:excludedUserId is null or u.id <> :excludedUserId)
            """)
    boolean existsActiveRoleInBank(
            @Param("bankId") Long bankId,
            @Param("role") RoleType role,
            @Param("inactiveStatus") UserStatus inactiveStatus,
            @Param("excludedUserId") Long excludedUserId
    );

    @Query("""
            select count(u) > 0
            from User u
            join u.roles r
            where r.name = :role
              and u.branch.id = :branchId
              and u.enabled = true
              and (u.status is null or u.status <> :inactiveStatus)
              and (:excludedUserId is null or u.id <> :excludedUserId)
            """)
    boolean existsActiveRoleInBranch(
            @Param("branchId") Long branchId,
            @Param("role") RoleType role,
            @Param("inactiveStatus") UserStatus inactiveStatus,
            @Param("excludedUserId") Long excludedUserId
    );
}
