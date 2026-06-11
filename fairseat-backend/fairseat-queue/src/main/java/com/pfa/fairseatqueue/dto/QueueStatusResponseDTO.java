package com.pfa.fairseatqueue.dto;

import com.pfa.fairseatqueue.domain.QueueStatus;

public record QueueStatusResponseDTO(
        String userId,
        Long gameId,
        Long position,
        QueueStatus status,
        String admissionToken
) {}