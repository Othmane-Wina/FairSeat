package com.pfa.fairseatbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient queueWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8084/v1/queue") // Queue Microservice port
                .build();
    }
}