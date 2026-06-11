package com.pfa.fairseatqueue.controller;

import com.pfa.fairseatqueue.dto.JoinQueueRequestDTO;
import com.pfa.fairseatqueue.dto.QueueStatusResponseDTO;
import com.pfa.fairseatqueue.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/join")
    public ResponseEntity<Map<String, String>> enterWaitingRoom(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody JoinQueueRequestDTO request) {
        queueService.joinQueue(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "Successfully entered virtual waiting room queue system."
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponseDTO> pollQueueStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam Long gameId) {
        return ResponseEntity.ok(queueService.getQueueStatus(gameId, userId));
    }

    @DeleteMapping("/clearance/consume")
    public ResponseEntity<Map<String, String>> consumeClearancePass(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam Long gameId) {

        if (queueService.consumeAdmission(gameId, userId)) {
            return ResponseEntity.ok(Map.of("message", "Clearance token consumed and invalidated successfully."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Token Invalidation Failed",
                    "reason", "No active clearance pass found for this user context."
            ));
        }
    }
}
