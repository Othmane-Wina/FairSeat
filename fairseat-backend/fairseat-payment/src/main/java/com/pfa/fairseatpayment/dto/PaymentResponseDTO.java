package com.pfa.fairseatpayment.dto;

import com.pfa.fairseatpayment.domain.PaymentStatus;

public record PaymentResponseDTO(
        String transactionId,
        PaymentStatus status,
        String message
) {}