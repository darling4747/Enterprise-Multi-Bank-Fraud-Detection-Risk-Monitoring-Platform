package com.bank.frauddetection.dto.notification;

import com.bank.frauddetection.enums.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
