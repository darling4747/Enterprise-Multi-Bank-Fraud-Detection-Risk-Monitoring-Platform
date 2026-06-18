package com.bank.frauddetection.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/notifications/**").authenticated()
                        .requestMatchers("/api/banks/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/branches/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/customers/**", "/api/accounts/**", "/api/beneficiaries/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST", "RISK_OFFICER", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/customers/**", "/api/accounts/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/beneficiaries/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.PATCH, "/api/customers/**", "/api/accounts/**", "/api/beneficiaries/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/security-incidents/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/security-incidents/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/security-incidents/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/audit-logs/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/ml/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "RISK_OFFICER", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST", "RISK_OFFICER", "AUDITOR")
                        .requestMatchers("/api/reports/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transactions/ingest").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/transactions/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transactions/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.PUT, "/api/transactions/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.PATCH, "/api/transactions/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/transactions/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST", "RISK_OFFICER", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/fraud/**", "/api/alerts/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN", "BRANCH_MANAGER", "FRAUD_ANALYST", "RISK_OFFICER", "AUDITOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/fraud/**", "/api/alerts/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "FRAUD_ANALYST")
                        .requestMatchers("/api/users/bank-admins/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/users/employees/**").hasRole("BANK_ADMIN")
                        .requestMatchers("/api/users/**", "/api/settings/**").hasAnyRole("PLATFORM_ADMIN", "SUPER_ADMIN", "BANK_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
