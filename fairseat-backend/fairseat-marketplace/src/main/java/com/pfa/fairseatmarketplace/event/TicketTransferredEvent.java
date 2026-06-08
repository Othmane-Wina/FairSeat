package com.pfa.fairseatmarketplace.event;

import java.util.UUID;

public record TicketTransferredEvent(
        UUID ticketId,
        String previousOwnerId,
        String newOwnerId
) {}