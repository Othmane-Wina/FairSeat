package com.pfa.fairseatticket.dto;

import com.pfa.fairseatticket.domain.TicketStatus;
import java.util.UUID;

public record TicketCodeResponseDTO(
        UUID ticketId,
        Long gameId,
        TicketStatus status,
        String liveCode,
        long expiresInSeconds
) {}