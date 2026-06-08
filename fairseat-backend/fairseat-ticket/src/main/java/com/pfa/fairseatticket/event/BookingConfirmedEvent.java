package com.pfa.fairseatticket.event;

import com.pfa.fairseatticket.domain.BookingStatus;

public record BookingConfirmedEvent(
        Long bookingId,
        String userId,
        Long gameId,
        BookingStatus status
) {}