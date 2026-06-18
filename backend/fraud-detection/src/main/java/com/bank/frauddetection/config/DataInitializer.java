package com.bank.frauddetection.config;

import com.bank.frauddetection.entity.Role;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.repository.RoleRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.util.Arrays;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    ApplicationRunner seedBankingRoles(
            JdbcTemplate jdbcTemplate,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.super-admin.username:superadmin}") String username,
            @Value("${app.bootstrap.super-admin.email:superadmin@bank.local}") String email,
            @Value("${app.bootstrap.super-admin.password:SuperAdmin@123}") String password
    ) {
        return args -> {
            jdbcTemplate.execute("alter table if exists roles drop constraint if exists roles_name_check");
            jdbcTemplate.execute("alter table if exists audit_logs drop constraint if exists audit_logs_event_type_check");
            jdbcTemplate.execute("alter table if exists fraud_cases drop constraint if exists fraud_cases_status_check");
            migrateUserLifecycleSchema(jdbcTemplate);
            migrateTransactionTenantSchema(jdbcTemplate);
            migrateLegacyRole(jdbcTemplate, "ROLE_ADMIN", RoleType.SUPER_ADMIN.name());
            migrateLegacyRole(jdbcTemplate, "ROLE_ANALYST", RoleType.FRAUD_ANALYST.name());
            migrateLegacyRole(jdbcTemplate, "ROLE_MANAGER", RoleType.RISK_OFFICER.name());
            migrateLegacyRole(jdbcTemplate, "ROLE_AUDITOR", RoleType.AUDITOR.name());
            migrateLegacyRole(jdbcTemplate, "ROLE_USER", RoleType.FRAUD_ANALYST.name());
            migrateFraudCaseSchema(jdbcTemplate);

            Arrays.stream(RoleType.values())
                    .forEach(roleType -> roleRepository.findByName(roleType)
                            .orElseGet(() -> roleRepository.save(new Role(roleType))));

            if (!userRepository.existsByUsername(username)) {
                Role platformAdmin = roleRepository.findByName(RoleType.PLATFORM_ADMIN).orElseThrow();
                Role superAdmin = roleRepository.findByName(RoleType.SUPER_ADMIN).orElseThrow();
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setFullName("Bootstrap Super Admin");
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setRoles(Set.of(platformAdmin, superAdmin));
                userRepository.save(user);
            } else {
                User user = userRepository.findByUsername(username).orElseThrow();
                Role platformAdmin = roleRepository.findByName(RoleType.PLATFORM_ADMIN).orElseThrow();
                Role superAdmin = roleRepository.findByName(RoleType.SUPER_ADMIN).orElseThrow();
                user.getRoles().add(platformAdmin);
                user.getRoles().add(superAdmin);
                userRepository.save(user);
            }
        };
    }

    private void migrateUserLifecycleSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("alter table if exists app_users add column if not exists bank_id bigint");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists branch_id bigint");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists employee_id varchar(80)");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists created_by bigint");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists status varchar(30)");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists password_status varchar(30)");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists must_change_password boolean default false");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists temporary_password_expires_at timestamp(6) with time zone");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists last_login_at timestamp(6) with time zone");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists failed_login_attempts integer default 0");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists account_locked_until timestamp(6) with time zone");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists mfa_enabled boolean default false");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists mfa_secret varchar(80)");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists mfa_enabled_at timestamp(6) with time zone");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists profile_photo_data_url text");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists critical_alert_emails boolean default true");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists daily_summary_report boolean default false");
        jdbcTemplate.execute("alter table if exists app_users add column if not exists session_timeout_minutes integer default 30");
        jdbcTemplate.update("update app_users set status = 'ACTIVE' where status is null");
        jdbcTemplate.update("update app_users set password_status = 'PERMANENT' where password_status is null");
        jdbcTemplate.update("update app_users set must_change_password = false where must_change_password is null");
        jdbcTemplate.update("update app_users set failed_login_attempts = 0 where failed_login_attempts is null");
        jdbcTemplate.update("update app_users set mfa_enabled = false where mfa_enabled is null");
        jdbcTemplate.update("update app_users set critical_alert_emails = true where critical_alert_emails is null");
        jdbcTemplate.update("update app_users set daily_summary_report = false where daily_summary_report is null");
        jdbcTemplate.update("update app_users set session_timeout_minutes = 30 where session_timeout_minutes is null");
        jdbcTemplate.execute("create unique index if not exists ux_app_users_mfa_secret on app_users (mfa_secret) where mfa_secret is not null");
    }

    private void migrateTransactionTenantSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists bank_id bigint");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists branch_id bigint");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists transaction_type varchar(40)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists step integer");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists oldbalance_org numeric(19,2)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists newbalance_orig numeric(19,2)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists oldbalance_dest numeric(19,2)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists newbalance_dest numeric(19,2)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists account_type varchar(30)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists customer_type varchar(30)");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists beneficiary_trusted boolean default false");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists known_device boolean default false");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists known_location boolean default false");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists transaction_hour integer");
        jdbcTemplate.execute("alter table if exists bank_transactions add column if not exists daily_transaction_pattern varchar(30)");
        jdbcTemplate.update("update bank_transactions set account_type = 'INDIVIDUAL' where account_type is null");
        jdbcTemplate.update("update bank_transactions set customer_type = 'RETAIL' where customer_type is null");
        jdbcTemplate.update("update bank_transactions set beneficiary_trusted = false where beneficiary_trusted is null");
        jdbcTemplate.update("update bank_transactions set known_device = false where known_device is null");
        jdbcTemplate.update("update bank_transactions set known_location = false where known_location is null");
        jdbcTemplate.update("update bank_transactions set daily_transaction_pattern = 'NORMAL' where daily_transaction_pattern is null");
    }

    private void migrateFraudCaseSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists investigation_notes varchar(4000)");
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists reviewed_by varchar(120)");
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists reviewed_at timestamp(6) with time zone");
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists priority varchar(30) default 'MEDIUM'");
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists assigned_to bigint");
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists assigned_by bigint");
        jdbcTemplate.execute("alter table if exists fraud_cases add column if not exists assigned_at timestamp(6) with time zone");
        jdbcTemplate.update("update fraud_cases set status = 'UNDER_REVIEW' where status = 'IN_REVIEW'");
        jdbcTemplate.update("update fraud_cases set status = 'CLOSED' where status = 'RESOLVED'");
        jdbcTemplate.update("update fraud_cases set priority = case risk_level when 'CRITICAL' then 'CRITICAL' when 'HIGH' then 'HIGH' when 'MEDIUM' then 'MEDIUM' else 'LOW' end");
    }

    private void migrateLegacyRole(JdbcTemplate jdbcTemplate, String oldName, String newName) {
        Long oldId = roleId(jdbcTemplate, oldName);
        if (oldId == null) {
            return;
        }

        Long newId = roleId(jdbcTemplate, newName);
        if (newId == null) {
            jdbcTemplate.update("update roles set name = ? where id = ?", newName, oldId);
            return;
        }

        jdbcTemplate.update("update user_roles set role_id = ? where role_id = ?", newId, oldId);
        jdbcTemplate.update("delete from roles where id = ?", oldId);
    }

    private Long roleId(JdbcTemplate jdbcTemplate, String name) {
        try {
            return jdbcTemplate.queryForObject("select id from roles where name = ?", Long.class, name);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
}
