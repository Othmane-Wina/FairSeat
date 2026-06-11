package com.pfa.fairseatqueue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class QueueReleaseScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String QUEUE_KEY = "queue::game::";
    private static final long   BATCH_SIZE = 10;

    // BUG FIX 1: fixedRate était 3000ms — le scheduler vidait la file AVANT
    // que le polling k6 démarre (à t=10s). Augmenté à 15s pour le MVP/démo.
    // En prod, calibrer selon le débit attendu.
    @Scheduled(fixedRate = 3000)
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