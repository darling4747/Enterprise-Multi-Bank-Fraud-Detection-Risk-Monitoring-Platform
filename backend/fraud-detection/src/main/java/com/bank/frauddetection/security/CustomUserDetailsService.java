package com.bank.frauddetection.security;

import com.bank.frauddetection.entity.Role;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.UserStatus;
import com.bank.frauddetection.repository.UserRepository;
import java.time.Instant;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled() || user.getStatus() == UserStatus.INACTIVE)
                .accountLocked(user.getStatus() == UserStatus.LOCKED
                        && user.getAccountLockedUntil() != null
                        && user.getAccountLockedUntil().isAfter(Instant.now()))
                .authorities(user.getRoles().stream()
                        .map(Role::getName)
                        .map(Enum::name)
                        .map(role -> "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }
}
