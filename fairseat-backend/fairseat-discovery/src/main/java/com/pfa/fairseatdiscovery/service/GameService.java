package com.pfa.fairseatdiscovery.service;

import com.pfa.fairseatdiscovery.domain.Game;
import com.pfa.fairseatdiscovery.domain.GameStatus;
import com.pfa.fairseatdiscovery.dto.GameResponseDTO;
import com.pfa.fairseatdiscovery.mapper.GameMapper; // Import our new mapper
import com.pfa.fairseatdiscovery.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GameMapper gameMapper; // Injected here

    private static final String CACHE_KEY = "games::active";
    private static final long CACHE_TTL_MINUTES = 5;

    @SuppressWarnings("unchecked")
    public List<GameResponseDTO> getActiveGames() {
        // 1. Fetch from Redis Cache
        try {
            List<GameResponseDTO> cachedDTOs = (List<GameResponseDTO>) redisTemplate.opsForValue().get(CACHE_KEY);
            if (cachedDTOs != null) {
                log.info("🎯 Cache HIT! Retrieving active games catalog directly from Redis.");
                return cachedDTOs;
            }
        } catch (Exception e) {
            log.error("Failed to read from Redis cache, falling back safely to Database", e);
        }

        // 2. Cache MISS -> Fetch from PostgreSQL
        log.info("⚠️ Cache MISS! Querying active games catalog from PostgreSQL (discovery_db).");
        List<Game> activeGames = gameRepository.findByStatusOrderByEventDateTimeAsc(GameStatus.SCHEDULED);

        // 3. Map entities to clean DTO records using the decoupled mapper component
        List<GameResponseDTO> gameDTOs = activeGames.stream()
                .map(gameMapper::toResponseDTO)
                .collect(Collectors.toList());

        // 4. Save the copy inside Redis
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, gameDTOs, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("💾 Successfully populated Redis cache with active games DTO catalog.");
        } catch (Exception e) {
            log.error("Failed to populate Redis cache", e);
        }

        return gameDTOs;
    }
}