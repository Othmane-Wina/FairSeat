package com.pfa.fairseatqueue.service;

import com.pfa.fairseatqueue.dto.JoinQueueRequestDTO;
import com.pfa.fairseatqueue.domain.QueueStatus;
import com.pfa.fairseatqueue.dto.QueueStatusResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUEUE_KEY_PREFIX = "queue::game::";
    private static final String RELEASED_SET_PREFIX = "queue::released::game::";

    public void joinQueue(JoinQueueRequestDTO request) {
        String queueKey = QUEUE_KEY_PREFIX + request.gameId();
        double score = (double) System.currentTimeMillis();

        log.info("📥 User [{}] entering waiting room for game #{}", request.userId(), request.gameId());
        redisTemplate.opsForZSet().add(queueKey, request.userId(), score);
    }

    public QueueStatusResponseDTO getQueueStatus(Long gameId, String userId) {
        String queueKey = QUEUE_KEY_PREFIX + gameId;
        String releasedKey = RELEASED_SET_PREFIX + gameId;

        // 1. Check if the user has already been cleared/released
        Boolean isReleased = redisTemplate.opsForSet().isMember(releasedKey, userId);
        if (Boolean.TRUE.equals(isReleased)) {
            log.info("🟢 User [{}] line position cleared! Granting access token.", userId);
            String mockAccessTicket = "PASS-" + UUID.nameUUIDFromBytes((userId + gameId).getBytes());
            return new QueueStatusResponseDTO(userId, gameId, 0L, QueueStatus.RELEASED, mockAccessTicket); // Updated here
        }

        // 2. If not released, calculate their exact real-time rank/position in line
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);

        if (rank == null) {
            return new QueueStatusResponseDTO(userId, gameId, -1L, QueueStatus.NOT_IN_QUEUE, null); // Updated here
        }

        log.info("⏳ User [{}] polled status. Rank position in waiting room: {}", userId, rank + 1);
        return new QueueStatusResponseDTO(userId, gameId, rank + 1, QueueStatus.WAITING, null); // Updated here
    }
}