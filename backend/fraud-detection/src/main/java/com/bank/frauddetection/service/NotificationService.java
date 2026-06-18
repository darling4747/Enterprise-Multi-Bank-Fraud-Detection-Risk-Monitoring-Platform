package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.notification.NotificationResponse;
import com.bank.frauddetection.entity.Notification;
import com.bank.frauddetection.entity.User;
import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import com.bank.frauddetection.enums.NotificationType;
import com.bank.frauddetection.enums.RoleType;
import com.bank.frauddetection.exception.BusinessException;
import com.bank.frauddetection.exception.ResourceNotFoundException;
import com.bank.frauddetection.repository.NotificationRepository;
import com.bank.frauddetection.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public NotificationResponse notify(User user, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void notifyPlatformUsers(NotificationType type, String message) {
        userRepository.findAll().stream()
                .filter(this::isPlatform)
                .forEach(user -> notify(user, type, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String username) {
        User user = findUser(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        User user = findUser(username);
        return notificationRepository.countByUserIdAndReadFlagFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(Long id, String username) {
        User user = findUser(username);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Cannot read another user's notification");
        }
        notification.setReadFlag(true);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);
        auditService.log(AuditEventType.NOTIFICATION_READ, user, user, bankId(user), "Notification marked read", AuditStatus.SUCCESS);
        return toResponse(notification);
    }

    @Transactional
    public List<NotificationResponse> markAllRead(String username) {
        User user = findUser(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(notification -> {
                    notification.setReadFlag(true);
                    notification.setReadAt(notification.getReadAt() == null ? Instant.now() : notification.getReadAt());
                    return notificationRepository.save(notification);
                })
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isReadFlag(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private boolean isPlatform(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.PLATFORM_ADMIN || role.getName() == RoleType.SUPER_ADMIN);
    }

    private Long bankId(User user) {
        return user.getBank() == null ? null : user.getBank().getId();
    }
}
