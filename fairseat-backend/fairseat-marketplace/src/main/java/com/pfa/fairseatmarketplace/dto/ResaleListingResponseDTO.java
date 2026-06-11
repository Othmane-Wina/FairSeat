package com.pfa.fairseatmarketplace.dto;

import com.pfa.fairseatmarketplace.domain.ListingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResaleListingResponseDTO(
        UUID listingId,
        UUID ticketId,
        String sellerId,
        Long gameId,
        BigDecimal askingPrice,
        ListingStatus status,
        LocalDateTime listedAt
) {}
