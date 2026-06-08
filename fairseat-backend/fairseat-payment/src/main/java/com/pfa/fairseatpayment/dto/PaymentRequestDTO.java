package com.pfa.fairseatpayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequestDTO(
        @NotNull(message = "Booking ID is required")
        Long bookingId,

        @NotBlank(message = "User ID is required")
        String userId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        Double amount,

        @NotBlank(message = "Idempotency key is required to prevent double billing")
        String idempotencyKey
) {}