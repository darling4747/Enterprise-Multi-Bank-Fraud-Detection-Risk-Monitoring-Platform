package com.bank.frauddetection.repository;

import com.bank.frauddetection.entity.Role;
import com.bank.frauddetection.enums.RoleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}
