package com.pfa.fairseatqueue.service;

import com.pfa.fairseatqueue.domain.QueueStatus;
import com.pfa.fairseatqueue.dto.JoinQueueRequestDTO;
import com.pfa.fairseatqueue.dto.QueueStatusResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String QUEUE_KEY = "queue::game::";
    private static final String TOKEN_KEY = "admission::";

    public void joinQueue(String userId, JoinQueueRequestDTO request) {
        String gameId = String.valueOf(request.gameId());
        String queueKey = QUEUE_KEY + gameId;
        Double existing = redisTemplate.opsForZSet().score(queueKey, userId);
        if (existing != null) return; // idempotent
        redisTemplate.opsForZSet().add(queueKey, userId, System.currentTimeMillis());
        log.info("User [{}] joined queue for game #{}", userId, request.gameId());
    }

    public QueueStatusResponseDTO getQueueStatus(Long gameId, String userId) {

        // Vérifier le token d'admission EN PREMIER
        String admissionToken = redisTemplate.opsForValue()
                .get(admissionKey(gameId, userId));

        if (admissionToken != null) {
            return new QueueStatusResponseDTO(
                    userId, gameId, 0L, QueueStatus.RELEASED, admissionToken);
        }

        // BUG FIX 4: l'ordre des vérifications était correct mais la clé
        // pouvait être désalignée entre scheduler et service.
        // La méthode admissionKey() est maintenant package-visible (static)
        // et partagée avec QueueReleaseScheduler pour garantir la cohérence.
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY + gameId, userId);

        if (rank == null) {
            // L'user n'est ni dans la file ni admis → vraiment absent
            return new QueueStatusResponseDTO(
                    userId, gameId, -1L, QueueStatus.NOT_IN_QUEUE, null);
        }
        log.info("Queue status check: userId={} gameId={} admissionKey={} token={}",
                userId, gameId, admissionKey(gameId, userId), admissionToken);
        // rank est 0-indexed, on expose position 1-indexed
        return new QueueStatusResponseDTO(
                userId, gameId, rank + 1, QueueStatus.WAITING, null);
    }

    /**
     * Consomme (invalide) le token d'admission — à appeler une seule fois
     * lors de la réservation effective pour éviter la réutilisation.
     */
    public boolean consumeAdmission(Long gameId, String userId) {
        return Boolean.TRUE.equals(redisTemplate.delete(admissionKey(gameId, userId)));
    }

    // BUG FIX 3 (suite): méthode package-visible pour que QueueReleaseScheduler
    // utilise exactement la même clé et qu'il n'y ait plus de désalignement.
    static String admissionKey(Long gameId, String userId) {
//        return TOKEN_KEY + gameId + "::" + userId;
        return "admission::" + gameId + "::" + userId;
    }
}
