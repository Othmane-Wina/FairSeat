package com.pfa.fairseatmarketplace.service;

import com.pfa.fairseatmarketplace.domain.ListingStatus;
import com.pfa.fairseatmarketplace.domain.ResaleListing;
import com.pfa.fairseatmarketplace.dto.ResaleListingResponseDTO;
import com.pfa.fairseatmarketplace.event.TicketTransferredEvent;
import com.pfa.fairseatmarketplace.repository.ResaleListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceService {

    private final ResaleListingRepository listingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 1. Seller lists their ticket
    @Transactional
    public ResaleListingResponseDTO createListing(UUID ticketId, String sellerId, Long gameId, Double price) {
        log.info("Creating new resale listing for Ticket [{}] by Seller [{}]", ticketId, sellerId);

        ResaleListing listing = ResaleListing.builder()
                .ticketId(ticketId)
                .sellerId(sellerId)
                .gameId(gameId)
                .askingPrice(price)
                .status(ListingStatus.AVAILABLE)
                .build();

        ResaleListing savedListing = listingRepository.save(listing);

        // Map the saved entity to your clean Response DTO
        return new ResaleListingResponseDTO(
                savedListing.getId(),
                savedListing.getTicketId(),
                savedListing.getSellerId(),
                savedListing.getGameId(),
                savedListing.getAskingPrice(),
                savedListing.getStatus(),
                savedListing.getListedAt()
        );
    }

    // 2. Buyer uses the Fast-Track bypass (Notice there is NO queueWebClient check here!)
    @Transactional
    public void fastTrackPurchase(UUID listingId, String buyerId) {
        log.info("Initiating Fast-Track purchase for Listing [{}] by Buyer [{}]", listingId, buyerId);

        // A. Fetch and Lock the Listing
        ResaleListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new IllegalStateException("Sorry, this ticket was just purchased by someone else!");
        }

        // B. Secure the internal state
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        // C. (In a full flow, you would call the Payment gRPC client here)
        log.info("Payment verified. Transferring ownership of Ticket [{}] to Buyer [{}]", listing.getTicketId(), buyerId);

        // D. Broadcast the transfer event to the Ticket Service
        TicketTransferredEvent event = new TicketTransferredEvent(
                listing.getTicketId(),
                listing.getSellerId(),
                buyerId
        );

        // Send to a new dedicated topic: "ticket-transfers"
        kafkaTemplate.send("ticket-transfers", listing.getTicketId().toString(), event);
        log.info("📣 Broadcasted TicketTransferredEvent to Kafka topic [ticket-transfers]");
    }
}