package com.bank.frauddetection.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class FraudConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.transactions:transactions.created}",
            groupId = "${spring.kafka.consumer.group-id:fraud-detection-service}"
    )
    public void onTransactionCreated(String payload) {
        log.debug("Received transaction event: {}", payload);
    }
}
