package com.pfa.fairseatqueue.controller;

import com.pfa.fairseatqueue.dto.JoinQueueRequestDTO;
import com.pfa.fairseatqueue.dto.QueueStatusResponseDTO;
import com.pfa.fairseatqueue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/join")
    public ResponseEntity<Map<String, String>> enterWaitingRoom(@RequestBody JoinQueueRequestDTO request) {
        queueService.joinQueue(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "Successfully entered virtual waiting room queue system."
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponseDTO> pollQueueStatus(
            @RequestParam Long gameId,
            @RequestParam String userId) {
        return ResponseEntity.ok(queueService.getQueueStatus(gameId, userId));
    }

    @DeleteMapping("/clearance/consume")
    public ResponseEntity<Map<String, String>> consumeClearancePass(
            @RequestParam Long gameId,
            @RequestParam String userId) {

        String releasedKey = "queue::released::game::" + gameId;

        // Atomically remove the user from the cleared set
        Boolean removed = redisTemplate.opsForSet().remove(releasedKey, userId) > 0;

        if (removed) {
            return ResponseEntity.ok(Map.of("message", "Clearance token consumed and invalidated successfully."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Token Invalidation Failed",
                    "reason", "No active clearance pass found for this user context."
            ));
        }
    }
}