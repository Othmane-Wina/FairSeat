package com.pfa.fairseatbooking.event;

import com.pfa.fairseatbooking.domain.BookingStatus;

public record BookingConfirmedEvent(
        Long bookingId,
        String userId,
        Long gameId,
        BookingStatus status
) {}