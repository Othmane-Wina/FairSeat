package com.pfa.fairseatbooking.mapper;

import com.pfa.fairseatbooking.domain.Booking;
import com.pfa.fairseatbooking.domain.BookingItem;
import com.pfa.fairseatbooking.dto.BookingItemResponseDTO;
import com.pfa.fairseatbooking.dto.BookingResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingMapper {

    public BookingResponseDTO toResponseDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingItemResponseDTO> itemDTOs = Collections.emptyList();
        if (booking.getItems() != null) {
            itemDTOs = booking.getItems().stream()
                    .map(this::toItemDTO)
                    .collect(Collectors.toList());
        }

        return new BookingResponseDTO(
                booking.getId(),
                booking.getUserId(),
                booking.getGameId(),
                itemDTOs,
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getExpiresAt()
        );
    }

    private BookingItemResponseDTO toItemDTO(BookingItem item) {
        if (item == null) {
            return null;
        }
        return new BookingItemResponseDTO(
                item.getId(),
                item.getSeatNumber(),
                item.getPrice()
        );
    }
}