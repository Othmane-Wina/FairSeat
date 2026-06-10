package com.pfa.fairseatticket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Kafka record sent to recovery after retries. topic={} partition={} offset={}",
                        record.topic(), record.partition(), record.offset(), exception),
                new FixedBackOff(1_000L, 3L)
        );
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, SecurityException.class);
        return errorHandler;
    }
}
