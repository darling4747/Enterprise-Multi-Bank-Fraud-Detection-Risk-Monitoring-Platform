package com.bank.frauddetection.dto.audit;

import com.bank.frauddetection.enums.AuditEventType;
import com.bank.frauddetection.enums.AuditStatus;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        AuditEventType eventType,
        Long performedByUserId,
        Long targetUserId,
        Long bankId,
        String ipAddress,
        String userAgent,
        String description,
        Instant timestamp,
        AuditStatus status
) {
}
