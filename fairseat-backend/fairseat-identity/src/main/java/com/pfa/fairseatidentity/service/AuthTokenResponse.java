package com.pfa.fairseatidentity.service;

// Ce que l'on retourne au mobile après validation OTP
public record AuthTokenResponse(
        String accessToken,    // Le JWT — utilisé dans toutes les requêtes suivantes
        String refreshToken,   // Pour renouveler le JWT quand il expire
        Integer expiresIn      // Durée de validité en secondes (3600 = 1h)
) {}