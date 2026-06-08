package com.pfa.fairseatmarketplace.dto;

import jakarta.validation.constraints.NotBlank;

public record FastTrackPurchaseRequestDTO(
        @NotBlank(message = "Buyer ID cannot be blank")
        String buyerId
) {}