package com.bank.frauddetection.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCreatedEvent(
        Long transactionId,
        String reference,
        String customerId,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}
