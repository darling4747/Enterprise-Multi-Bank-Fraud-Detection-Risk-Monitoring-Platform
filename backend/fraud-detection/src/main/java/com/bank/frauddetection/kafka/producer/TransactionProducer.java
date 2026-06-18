package com.bank.frauddetection.kafka.producer;

import com.bank.frauddetection.kafka.event.FraudDetectedEvent;
import com.bank.frauddetection.kafka.event.TransactionCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String transactionTopic;
    private final String fraudTopic;
    private final boolean enabled;

    public TransactionProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.transactions:transactions.created}") String transactionTopic,
            @Value("${app.kafka.topics.fraud:fraud.detected}") String fraudTopic,
            @Value("${app.kafka.enabled:false}") boolean enabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.transactionTopic = transactionTopic;
        this.fraudTopic = fraudTopic;
        this.enabled = enabled;
    }

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        publish(transactionTopic, event.reference(), event);
    }

    public void publishFraudDetected(FraudDetectedEvent event) {
        publish(fraudTopic, event.transactionReference(), event);
    }

    private void publish(String topic, String key, Object event) {
        if (!enabled) {
            log.debug("Kafka disabled; skipped event for topic {}", topic);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("Kafka publish skipped for topic {}: {}", topic, ex.getMessage());
        }
    }
}
