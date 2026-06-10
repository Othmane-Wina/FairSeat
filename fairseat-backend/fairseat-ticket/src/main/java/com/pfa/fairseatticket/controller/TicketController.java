package com.pfa.fairseatticket.controller;

import com.pfa.fairseatticket.dto.TicketCodeResponseDTO;
import com.pfa.fairseatticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{ticketId}/secure-code")
    public ResponseEntity<?> getSecureTicketCode(
            @PathVariable UUID ticketId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            // Hand off to the business layer
            TicketCodeResponseDTO responsePayload = ticketService.generateSecurePayload(ticketId, userId);
            return ResponseEntity.ok(responsePayload);

        } catch (SecurityException e) {
            // 403 Forbidden for ownership mismatch
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));

        } catch (IllegalArgumentException e) {
            // 404 Not Found if the ticket doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
