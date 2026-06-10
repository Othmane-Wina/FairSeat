package com.pfa.fairseatidentity.controller;

import com.pfa.fairseatidentity.dto.ConfirmOtpRequest;
import com.pfa.fairseatidentity.dto.VerifyPhoneRequest;
import com.pfa.fairseatidentity.service.AuthTokenResponse;
import com.pfa.fairseatidentity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/identity")
@RequiredArgsConstructor
@Slf4j
public class IdentityController {

    private final RedisTemplate<String, String> redisTemplate;
    private final IdentityService identityService;

    // Route PUBLIQUE — pas de JWT requis (configuré dans le Gateway SecurityConfig)
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @RequestBody VerifyPhoneRequest req) {
        identityService.sendOtp(req.phone());
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + req.phone()));
    }

    // Route PUBLIQUE — retourne le JWT si OTP correct
    @PostMapping("/confirm-otp")
    public ResponseEntity<AuthTokenResponse> confirmOtp(
            @RequestBody ConfirmOtpRequest req) {
        AuthTokenResponse token = identityService.confirmOtp(req.phone(), req.otp());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/dev/otp")
    public ResponseEntity<Map<String, String>> getOtpForTesting(
            @RequestParam String phone) {
        // IMPORTANT : désactiver en production
        // Protéger avec un profil Spring : @Profile("dev")
        String otp = redisTemplate.opsForValue().get("otp::" + phone);
        if (otp == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No OTP found for this phone"));
        }
        return ResponseEntity.ok(Map.of("otp", otp, "phone", phone));
    }
}