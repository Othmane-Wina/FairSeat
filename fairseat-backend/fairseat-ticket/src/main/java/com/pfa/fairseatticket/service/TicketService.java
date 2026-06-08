package com.pfa.fairseatticket.service;

import com.pfa.fairseatticket.domain.Ticket;
import com.pfa.fairseatticket.dto.TicketCodeResponseDTO;
import com.pfa.fairseatticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TotpService totpService;

    public TicketCodeResponseDTO generateSecurePayload(UUID ticketId, String userId) {
        // 1. Fetch the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found for ID: " + ticketId));

        // 2. Validate Ownership (Business Rule)
        if (!ticket.getUserId().equals(userId)) {
            log.warn("🚨 Security Alert: User [{}] attempted to access ticket [{}] owned by [{}]",
                    userId, ticketId, ticket.getUserId());
            throw new SecurityException("Access Denied: You do not own this ticket.");
        }

        // 3. Generate cryptographic payload
        String liveTotpCode = totpService.generateTotp(ticket.getTicketSecret());
        long expiresInSeconds = 15 - ((System.currentTimeMillis() / 1000) % 15);

        // 4. Return safely typed DTO
        return new TicketCodeResponseDTO(
                ticket.getId(),
                ticket.getGameId(),
                ticket.getStatus(),
                liveTotpCode,
                expiresInSeconds
        );
    }
}