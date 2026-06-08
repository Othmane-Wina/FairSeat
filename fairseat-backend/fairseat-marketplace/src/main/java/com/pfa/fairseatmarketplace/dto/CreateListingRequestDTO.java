package com.pfa.fairseatmarketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateListingRequestDTO(
        @NotNull(message = "Ticket ID is required")
        UUID ticketId,

        @NotBlank(message = "Seller ID cannot be blank")
        String sellerId,

        @NotNull(message = "Game ID is required")
        Long gameId,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        Double price
) {}