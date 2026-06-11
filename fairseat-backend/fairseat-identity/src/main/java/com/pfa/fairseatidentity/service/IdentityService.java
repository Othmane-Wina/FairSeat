package com.pfa.fairseatidentity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.mock-password}")
    private String keycloakMockPassword;

    // ÉTAPE 1 : Génère et stocke l'OTP dans Redis avec TTL 5 minutes
    public void sendOtp(String phone) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        redisTemplate.opsForValue().set("otp::" + phone, otp, Duration.ofMinutes(5));
        // En dev : on log l'OTP complet pour pouvoir le tester
        log.info(">>> DEV ONLY — OTP for {} : {}", phone, otp);
    }

    // ÉTAPE 2 : Vérifie l'OTP et retourne un JWT Keycloak
    public AuthTokenResponse confirmOtp(String phone, String otp) {
        String stored = redisTemplate.opsForValue().get("otp::" + phone);

        if (stored == null) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "OTP expired or never requested for this number");
        }
        if (!stored.equals(otp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid OTP — check the logs for the correct code");
        }

        // Supprimer l'OTP — usage unique
        redisTemplate.delete("otp::" + phone);
        log.info("OTP validated for {}. Requesting Keycloak token...", phone);

        return getKeycloakToken(phone);
    }

    // Appelle Keycloak pour obtenir un JWT signé
    private AuthTokenResponse getKeycloakToken(String phone) {
        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", phone);           // username Keycloak = numéro de téléphone
        body.add("password", keycloakMockPassword);  // mot de passe fixe pour la démo

        Map response = rest.postForObject(
                keycloakTokenUri,
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Keycloak returned no response — is it running on port 8080?");
        }

        return new AuthTokenResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                (Integer) response.get("expires_in")
        );
    }
}