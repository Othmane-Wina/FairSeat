package com.pfa.fairseatticket.service;

import com.pfa.fairseatticket.domain.Ticket;
import com.pfa.fairseatticket.domain.TicketStatus;
import com.pfa.fairseatticket.event.BookingConfirmedEvent;
import com.pfa.fairseatticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventConsumer {

    private final TicketRepository ticketRepository;

    @Transactional
    @KafkaListener(topics = "booking-events", groupId = "ticket-generation-group")
    public void consumeBookingEvent(BookingConfirmedEvent event) {
        log.info("📥 [KAFKA CONSUMER WAKE-UP] Received new Booking Event from cluster!");
        log.info("Processing Ticket Generation for Booking ID: #{} | User: [{}]", event.bookingId(), event.userId());

        try {
            // 1. Generate the secure cryptographic secret for the TOTP QR Code
            String ticketSecret = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // 2. Build the official ticket record
            Ticket newTicket = Ticket.builder()
                    .bookingId(event.bookingId())
                    .userId(event.userId())
                    .gameId(event.gameId())
                    .ticketSecret(ticketSecret)
                    .status(TicketStatus.ACTIVE)
                    .build();

            // 3. Save to ticket_db
            Ticket savedTicket = ticketRepository.save(newTicket);

            log.info("✅ Successfully generated & saved Ticket [{}] with Secret [{}] for Game #{}",
                    savedTicket.getId(), ticketSecret, event.gameId());
            log.info("---------------------------------------------------");

        } catch (Exception e) {
            log.error("❌ Failed to process ticket generation for Booking ID: {}", event.bookingId(), e);
        }
    }
}