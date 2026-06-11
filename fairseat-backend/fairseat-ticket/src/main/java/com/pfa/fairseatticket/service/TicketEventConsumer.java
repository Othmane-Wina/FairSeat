package com.pfa.fairseatticket.service;

import com.pfa.fairseatticket.domain.Ticket;
import com.pfa.fairseatticket.domain.TicketStatus;
import com.pfa.fairseatticket.event.BookingConfirmedEvent;
import com.pfa.fairseatticket.event.TicketTransferredEvent;
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
        log.info("Processing ticket generation for bookingId={} userId={}", event.bookingId(), event.userId());

        if (ticketRepository.findByBookingId(event.bookingId()).isPresent()) {
            log.info("Ticket already exists for bookingId={}, skipping duplicate event", event.bookingId());
            return;
        }

        String ticketSecret = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Ticket newTicket = Ticket.builder()
                .bookingId(event.bookingId())
                .userId(event.userId())
                .gameId(event.gameId())
                .ticketSecret(ticketSecret)
                .status(TicketStatus.ACTIVE)
                .build();

        Ticket savedTicket = ticketRepository.save(newTicket);
        log.info("Generated ticketId={} for bookingId={}", savedTicket.getId(), event.bookingId());
    }

    @Transactional
    @KafkaListener(topics = "ticket-transfers", groupId = "ticket-generation-group")
    public void consumeTicketTransfer(TicketTransferredEvent event) {
        log.info("Processing ticket transfer ticketId={} previousOwnerId={} newOwnerId={}",
                event.ticketId(), event.previousOwnerId(), event.newOwnerId());

        Ticket ticket = ticketRepository.findById(event.ticketId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found in database"));

        if (!ticket.getUserId().equals(event.previousOwnerId())) {
            throw new SecurityException("Transfer failed: seller does not match current ticket owner");
        }

        ticket.setUserId(event.newOwnerId());
        ticketRepository.save(ticket);
        log.info("Transferred ticketId={} to userId={}", ticket.getId(), event.newOwnerId());
    }
}
