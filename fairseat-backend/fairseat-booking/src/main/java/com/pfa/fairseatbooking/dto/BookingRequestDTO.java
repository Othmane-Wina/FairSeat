package com.pfa.fairseatbooking.dto;

import java.util.List;

public record BookingRequestDTO(
        String userId,
        Long gameId,
        List<String> seatNumbers, // Allows a user to select multiple seats in one click (e.g., "ZONE_A_4", "ZONE_A_5")
        String idempotencyKey
) {}