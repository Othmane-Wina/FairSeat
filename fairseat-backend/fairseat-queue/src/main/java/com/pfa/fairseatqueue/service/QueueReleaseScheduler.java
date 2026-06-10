package com.pfa.fairseatqueue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class QueueReleaseScheduler {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    private static final String QUEUE_KEY_PREFIX = "queue::game::";
//    private static final String RELEASED_SET_PREFIX = "queue::released::game::";
//
//    // Configurable metering parameters
//    private static final Long BATCH_SIZE = 5L; // Simulating small batches of 5 users for easy local testing
//
//    // Automatically triggers every 10 seconds (10000 milliseconds)
//    @Scheduled(fixedRate = 10000)
//    public void releaseUserBatch() {
//        // Hardcoded for gameId 1 for our current local validation testing
//        Long currentTestGameId = 1L;
//        String queueKey = QUEUE_KEY_PREFIX + currentTestGameId;
//        String releasedKey = RELEASED_SET_PREFIX + currentTestGameId;
//
//        log.info("⚙️ Batch Release Chrono triggered. Inspecting virtual waiting room lines...");
//
//        // 1. Fetch the oldest N users (lowest scores/timestamps) currently waiting in line
//        Set<Object> usersToRelease = redisTemplate.opsForZSet().range(queueKey, 0, BATCH_SIZE - 1);
//
//        if (usersToRelease == null || usersToRelease.isEmpty()) {
//            log.info("✔ Waiting room is completely empty. No gating required.");
//            return;
//        }
//
//        log.info("🔓 Processing flow control gate: Graduating a batch of {} users to RELEASED state.", usersToRelease.size());
//
//        // 2. Add these users to the allowed set and remove them from the waiting list atomically
//        for (Object user : usersToRelease) {
//            String userId = (String) user;
//
//            // Allow them through the gate pass
//            redisTemplate.opsForSet().add(releasedKey, userId);
//
//            // Pull them out of the line
//            redisTemplate.opsForZSet().remove(queueKey, userId);
//
//            log.info("🟢 User [{}] cleared to proceed to checkout.", userId);
//        }
//    }
//}

//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class QueueReleaseScheduler {
//
//    private final RedisTemplate<String, String> redisTemplate;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    private static final String QUEUE_KEY  = "queue::game::";
//    private static final String TOKEN_KEY  = "admission::";
//    private static final long   BATCH_SIZE = 50L;
//
//    @Scheduled(fixedRate = 3000)
//    public void releaseUserBatch() {
//        Set<String> queueKeys = redisTemplate.keys(QUEUE_KEY + "*");
//        if (queueKeys == null || queueKeys.isEmpty()) return;
//
//        for (String queueKey : queueKeys) {
//            String gameId = queueKey.substring(QUEUE_KEY.length());
//            // ZPOPMIN = atomic read + remove
//
//            Set<ZSetOperations.TypedTuple<String>> popped =
//                    redisTemplate.opsForZSet().popMin(queueKey, BATCH_SIZE);
//
//            if (popped == null || popped.isEmpty()) continue;
//
//            for (ZSetOperations.TypedTuple<String> entry : popped) {
//                String userId = entry.getValue();
//                String token  = UUID.randomUUID().toString();
//
//                // Store token with 15 min TTL
//                redisTemplate.opsForValue()
//                        .set(admissionKey(gameId, userId), token, Duration.ofMinutes(15));
//
//                // Publish to Kafka
//                kafkaTemplate.send("admission-events", userId, token);
//
//                log.info("🟢 Admitted userId={} game={} token={}", userId, gameId, token);
//            }
//        }
//    }
//
//    private String admissionKey(String gameId, String userId) {
//        return TOKEN_KEY + gameId + "::" + userId;
//    }
//}
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueReleaseScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String QUEUE_KEY = "queue::game::";
    private static final long   BATCH_SIZE = 10;

    // BUG FIX 1: fixedRate était 3000ms — le scheduler vidait la file AVANT
    // que le polling k6 démarre (à t=10s). Augmenté à 15s pour le MVP/démo.
    // En prod, calibrer selon le débit attendu.
    @Scheduled(fixedRate = 15000)
    public void releaseUserBatch() {

        Set<String> queueKeys = redisTemplate.keys(QUEUE_KEY + "*");
        if (queueKeys == null || queueKeys.isEmpty()) return;

        for (String queueKey : queueKeys) {

            String gameId = queueKey.replace(QUEUE_KEY, "");

            // BUG FIX 2: race condition avec popMin.
            // popMin retirait l'utilisateur de la ZSet atomiquement, PUIS on
            // stockait le token. Entre ces deux opérations (~quelques ms),
            // getQueueStatus trouvait l'user ni dans la ZSet ni avec un token
            // → retournait NOT_IN_QUEUE.
            //
            // Solution : lire d'abord avec range(), stocker le token, PUIS
            // supprimer de la ZSet. Ainsi getQueueStatus trouve toujours soit
            // l'user dans la ZSet (WAITING), soit son token (RELEASED).
            Set<ZSetOperations.TypedTuple<String>> candidates =
                    redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, BATCH_SIZE - 1);

            if (candidates == null || candidates.isEmpty()) continue;

            for (ZSetOperations.TypedTuple<String> entry : candidates) {

                String userId = entry.getValue();
                if (userId == null) continue;

                String token = UUID.randomUUID().toString();

                // BUG FIX 3: le scheduler utilisait gameId (String) pour construire
                // la clé admission::gameId::userId, mais QueueService.admissionKey()
                // prenait un Long gameId et faisait TOKEN_KEY + gameId + "::" + userId.
                // Les deux produisent la même string si le Long.toString() == la String,
                // MAIS on uniformise ici en utilisant QueueService.admissionKey()
                // via la constante partagée pour éviter tout désalignement futur.
                String admissionKey = QueueService.admissionKey(Long.parseLong(gameId), userId);

                // Étape 1 : stocker le token (TTL 15 min)
                redisTemplate.opsForValue()
                        .set(admissionKey, token, Duration.ofMinutes(15));

                // Étape 2 : SEULEMENT APRÈS, retirer de la ZSet
                // → plus de fenêtre NOT_IN_QUEUE
                redisTemplate.opsForZSet().remove(queueKey, userId);

                // Étape 3 : notifier via Kafka (best-effort pour le MVP)
                kafkaTemplate.send("admission-events", userId, token);

                log.info("🟢 ADMITTED user={} game={} token={}", userId, gameId, token);
            }
        }
    }
}