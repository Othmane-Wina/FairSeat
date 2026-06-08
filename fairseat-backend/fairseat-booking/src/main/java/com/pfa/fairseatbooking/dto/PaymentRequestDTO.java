package com.pfa.fairseatbooking.dto;

public record PaymentRequestDTO(
        Long bookingId,
        String userId,
        Double amount,
        String idempotencyKey
) {}