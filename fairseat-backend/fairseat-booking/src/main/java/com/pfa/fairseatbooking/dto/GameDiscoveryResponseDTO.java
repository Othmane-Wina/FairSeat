package com.pfa.fairseatbooking.dto;

import com.pfa.fairseatbooking.domain.GameStatus;

import java.time.LocalDateTime;

public record GameDiscoveryResponseDTO(
        Long id,
        String title,
        String description,
        Double basePrice,
        GameStatus status,
        LocalDateTime eventDateTime
) {}