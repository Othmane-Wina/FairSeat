package com.pfa.fairseatbooking.dto;

public record BookingItemResponseDTO(
        Long id,
        String seatNumber,
        Double price
) {}
