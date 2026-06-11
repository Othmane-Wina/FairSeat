package com.pfa.fairseatmarketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequestDTO(
        @NotNull(message = "Ticket ID is required")
        UUID ticketId,

        @NotNull(message = "Game ID is required")
        Long gameId,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price
) {}
