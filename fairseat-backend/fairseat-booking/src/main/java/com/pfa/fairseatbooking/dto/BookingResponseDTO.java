package com.pfa.fairseatbooking.dto;

import com.pfa.fairseatbooking.domain.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponseDTO(
        Long id,
        String userId,
        Long gameId,
        List<BookingItemResponseDTO> items,
        Double totalAmount,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {}