package com.pfa.fairseatmarketplace.domain;

public enum ListingStatus {
    AVAILABLE,
    LOCKED_FOR_CHECKOUT, // Optimistic lock state
    SOLD,
    CANCELLED
}
