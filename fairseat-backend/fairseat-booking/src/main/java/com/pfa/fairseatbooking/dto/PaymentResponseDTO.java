package com.pfa.fairseatbooking.dto;

import com.pfa.fairseatbooking.domain.PaymentStatus;

public record PaymentResponseDTO(
        String transactionId,
        PaymentStatus status,
        String message
) {}