package com.pfa.fairseatmarketplace.controller;

import com.pfa.fairseatmarketplace.dto.CreateListingRequestDTO;
import com.pfa.fairseatmarketplace.dto.FastTrackPurchaseRequestDTO;
import com.pfa.fairseatmarketplace.dto.ResaleListingResponseDTO;
import com.pfa.fairseatmarketplace.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @PostMapping("/listings")
    public ResponseEntity<ResaleListingResponseDTO> createResaleListing(
            @Valid @RequestBody CreateListingRequestDTO request) {

        ResaleListingResponseDTO response = marketplaceService.createListing(
                request.ticketId(),
                request.sellerId(),
                request.gameId(),
                request.price()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/listings/{listingId}/fast-track")
    public ResponseEntity<Map<String, String>> executeFastTrackPurchase(
            @PathVariable UUID listingId,
            @Valid @RequestBody FastTrackPurchaseRequestDTO request) {

        marketplaceService.fastTrackPurchase(listingId, request.buyerId());
        return ResponseEntity.ok(Map.of("message", "Fast-Track purchase successful! Ownership transferred."));
    }
}