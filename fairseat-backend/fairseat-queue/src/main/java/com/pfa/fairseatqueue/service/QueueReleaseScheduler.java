package com.pfa.fairseatqueue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueReleaseScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUEUE_KEY_PREFIX = "queue::game::";
    private static final String RELEASED_SET_PREFIX = "queue::released::game::";

    // Configurable metering parameters
    private static final Long BATCH_SIZE = 5L; // Simulating small batches of 5 users for easy local testing

    // Automatically triggers every 10 seconds (10000 milliseconds)
    @Scheduled(fixedRate = 10000)
    public void releaseUserBatch() {
        // Hardcoded for gameId 1 for our current local validation testing
        Long currentTestGameId = 1L;
        String queueKey = QUEUE_KEY_PREFIX + currentTestGameId;
        String releasedKey = RELEASED_SET_PREFIX + currentTestGameId;

        log.info("⚙️ Batch Release Chrono triggered. Inspecting virtual waiting room lines...");

        // 1. Fetch the oldest N users (lowest scores/timestamps) currently waiting in line
        Set<Object> usersToRelease = redisTemplate.opsForZSet().range(queueKey, 0, BATCH_SIZE - 1);

        if (usersToRelease == null || usersToRelease.isEmpty()) {
            log.info("✔ Waiting room is completely empty. No gating required.");
            return;
        }

        log.info("🔓 Processing flow control gate: Graduating a batch of {} users to RELEASED state.", usersToRelease.size());

        // 2. Add these users to the allowed set and remove them from the waiting list atomically
        for (Object user : usersToRelease) {
            String userId = (String) user;

            // Allow them through the gate pass
            redisTemplate.opsForSet().add(releasedKey, userId);

            // Pull them out of the line
            redisTemplate.opsForZSet().remove(queueKey, userId);

            log.info("🟢 User [{}] cleared to proceed to checkout.", userId);
        }
    }
}