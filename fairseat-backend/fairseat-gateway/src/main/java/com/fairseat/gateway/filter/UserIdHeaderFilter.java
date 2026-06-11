package com.fairseat.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

  // S'exécute après le filtre de sécurité (qui a déjà validé le JWT)
  @Override
  public int getOrder() {
    return -1; // priorité haute, mais après Spring Security
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .filter(auth -> auth != null && auth.getPrincipal() instanceof Jwt)
            .map(auth -> (Jwt) auth.getPrincipal())
            .map(jwt -> {
              String userId = jwt.getSubject(); // claim "sub" = l'ID Keycloak de l'user

              // On log pour vérifier pendant le dev
              log.debug("JWT sub extracted: {}", userId);

              // On ajoute X-User-Id dans les headers vers les microservices aval
              ServerHttpRequest mutatedRequest = exchange.getRequest()
                      .mutate()
                      .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Email");
                      })
                      .header("X-User-Id", userId)
                      .header("X-User-Email", jwt.getClaimAsString("email"))
                      .build();

              return exchange.mutate().request(mutatedRequest).build();
            })
            .defaultIfEmpty(exchange) // route publique sans JWT → passe quand même
            .flatMap(ex -> chain.filter(ex));
  }
}
