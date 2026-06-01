package com.pfa.fairseatqueue.dto;

public record JoinQueueRequestDTO(
        String userId,
        Long gameId
) {}