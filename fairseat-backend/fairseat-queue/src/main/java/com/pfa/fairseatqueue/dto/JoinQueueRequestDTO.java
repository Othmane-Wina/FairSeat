package com.pfa.fairseatqueue.dto;

import jakarta.validation.constraints.NotNull;

public record JoinQueueRequestDTO(
        @NotNull(message = "Game ID is required")
        Long gameId
) {}
