package com.bank.frauddetection.service;

import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.entity.UserPasswordHistory;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.repository.UserPasswordHistoryRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private static final int HISTORY_LIMIT = 5;

    private final UserPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(UserPasswordHistoryRepository passwordHistoryRepository, PasswordEncoder passwordEncoder) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validateStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 12) {
            throw new BusinessException("Password must be at least 12 characters");
        }
        if (!rawPassword.matches(".*[A-Z].*")) {
            throw new BusinessException("Password must contain an uppercase letter");
        }
        if (!rawPassword.matches(".*[a-z].*")) {
            throw new BusinessException("Password must contain a lowercase letter");
        }
        if (!rawPassword.matches(".*\\d.*")) {
            throw new BusinessException("Password must contain a number");
        }
        if (!rawPassword.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessException("Password must contain a special character");
        }
    }

    public void validateNewPassword(User user, String rawPassword) {
        validateStrength(rawPassword);
        if (user.getPasswordHash() != null && passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException("Cannot reuse the current password");
        }
        List<UserPasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        boolean reused = history.stream()
                .limit(HISTORY_LIMIT)
                .anyMatch(item -> passwordEncoder.matches(rawPassword, item.getPasswordHash()));
        if (reused) {
            throw new BusinessException("Cannot reuse the last 5 passwords");
        }
    }

    public void rememberPassword(User user) {
        rememberPasswordHash(user, user.getPasswordHash());
    }

    public void rememberPasswordHash(User user, String passwordHash) {
        if (user.getId() == null || passwordHash == null) {
            return;
        }
        UserPasswordHistory history = new UserPasswordHistory();
        history.setUser(user);
        history.setPasswordHash(passwordHash);
        passwordHistoryRepository.save(history);

        List<UserPasswordHistory> allHistory = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (allHistory.size() > HISTORY_LIMIT) {
            passwordHistoryRepository.deleteAll(allHistory.subList(HISTORY_LIMIT, allHistory.size()));
        }
    }
}
