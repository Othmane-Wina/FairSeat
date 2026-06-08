package com.pfa.fairseatbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient queueWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8084/v1/queue")
                .build();
    }

    @Bean
    public WebClient discoveryWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8082/v1/games")
                .build();
    }

    @Bean
    public WebClient paymentWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8087/v1/payments")
                .build();
    }
}