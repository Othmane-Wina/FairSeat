package com.fairseat.gateway.config;

import io.netty.handler.codec.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth

                    // Routes publiques — pas de JWT requis
                    .pathMatchers(String.valueOf(HttpMethod.GET), "/v1/games/**").permitAll()
                    .pathMatchers(String.valueOf(HttpMethod.POST), "/v1/identity/verify").permitAll()
                    .pathMatchers(String.valueOf(HttpMethod.POST), "/v1/identity/confirm-otp").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    // Tout le reste nécessite un JWT valide

                    .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(Customizer.withDefaults())
            )
            .build();
  }
}