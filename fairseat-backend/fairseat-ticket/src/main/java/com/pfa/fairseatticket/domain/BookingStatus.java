package com.pfa.fairseatticket.domain;

public enum BookingStatus {
    PENDING,   // Seat temporarily held during checkout
    CONFIRMED, // Payment cleared, ticket issued
    CANCELLED, // Held time expired or user abandoned cart
    REFUNDED   // Match cancelled or customer refunded
}