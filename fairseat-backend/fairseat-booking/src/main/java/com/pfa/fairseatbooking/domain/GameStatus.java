package com.pfa.fairseatbooking.domain;

public enum GameStatus {
    SCHEDULED,   // Match is planned and visible in the catalog
    LIVE,        // Game is currently being played live in the stadium
    COMPLETED,   // The match has ended
    CANCELLED    // Event has been called off
}