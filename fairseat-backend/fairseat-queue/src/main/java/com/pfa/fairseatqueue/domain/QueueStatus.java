package com.pfa.fairseatqueue.domain;

public enum QueueStatus {
    WAITING,       // User is in the Sorted Set waiting for their turn
    RELEASED,      // User has been cleared by the batch scheduler and can proceed to checkout
    NOT_IN_QUEUE   // User is not tracked by the virtual waiting room system
}