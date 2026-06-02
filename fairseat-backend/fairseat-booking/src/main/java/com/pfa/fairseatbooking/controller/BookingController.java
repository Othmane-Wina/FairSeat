package com.pfa.fairseatbooking.controller;

import com.pfa.fairseatbooking.dto.BookingRequestDTO;
import com.pfa.fairseatbooking.dto.BookingResponseDTO;
import com.pfa.fairseatbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/reserve")
    public ResponseEntity<?> holdSeats(@RequestBody BookingRequestDTO request) {
        try {
            BookingResponseDTO response = bookingService.initiateBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // Catches maximum ticket count violations or empty inputs
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Invalid Booking Request",
                    "reason", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            // Catches high-concurrency lock acquisition check failures
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Seat Allocation Conflict",
                    "reason", e.getMessage()
            ));
        } catch (SecurityException e) {
            // Catches invalid waiting room bypass attempts
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Queue Clearance Security Violation",
                    "reason", e.getMessage()
            ));
        }
    }
}