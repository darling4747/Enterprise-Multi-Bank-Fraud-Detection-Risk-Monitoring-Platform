package com.bank.frauddetection.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    NewTopic transactionTopic(@Value("${app.kafka.topics.transactions:transactions.created}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic fraudTopic(@Value("${app.kafka.topics.fraud:fraud.detected}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
