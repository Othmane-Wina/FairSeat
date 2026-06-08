package com.pfa.fairseatbooking.service;

import com.pfa.fairseatbooking.domain.Booking;
import com.pfa.fairseatbooking.domain.BookingItem;
import com.pfa.fairseatbooking.domain.BookingStatus;
import com.pfa.fairseatbooking.dto.BookingRequestDTO;
import com.pfa.fairseatbooking.dto.BookingResponseDTO;
import com.pfa.fairseatbooking.dto.GameDiscoveryResponseDTO;
import com.pfa.fairseatbooking.event.BookingConfirmedEvent;
import com.pfa.fairseatbooking.mapper.BookingMapper;
import com.pfa.fairseatbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RedissonClient redissonClient;
    private final BookingMapper bookingMapper;
    private final WebClient queueWebClient;
    private final WebClient discoveryWebClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${fairseat.lock.wait-time-seconds}")
    private long waitTimeSeconds;

    @Value("${fairseat.lock.lease-time-seconds}")
    private long leaseTimeSeconds;

    @Transactional
    public BookingResponseDTO initiateBooking(BookingRequestDTO request) {
        // 1. High-Level Transaction Flow
        validateRequestGuardrails(request);
        verifyQueueClearance(request.userId(), request.gameId());
        double seatPrice = fetchDynamicSeatPrice(request.gameId());

        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            acquiredLocks = acquireSeatLocks(request.gameId(), request.seatNumbers());

            Booking savedBooking = saveBookingTransaction(request, seatPrice);
            publishBookingEvent(savedBooking); // Broadcast the event to the cluster
            consumeQueueClearancePass(request.userId(), request.gameId());

            return bookingMapper.toResponseDTO(savedBooking);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAllLocks(acquiredLocks);
            throw new RuntimeException("Thread transaction lifecycle interrupted unexpectedly", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER METHODS (Implementation Details)
    // ─────────────────────────────────────────────────────────────────────────

    private void validateRequestGuardrails(BookingRequestDTO request) {
        if (request.seatNumbers() == null || request.seatNumbers().isEmpty()) {
            throw new IllegalArgumentException("Booking must include at least one seat reservation selection.");
        }
        if (request.seatNumbers().size() > 4) {
            throw new IllegalArgumentException("Business Guardrail Violation: Maximum permitted tickets per booking order is 4.");
        }
    }

    private void verifyQueueClearance(String userId, Long gameId) {
        log.info("[Thread: {}] Sending background verification check to fairseat-queue for User [{}]",
                Thread.currentThread().getName(), userId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> queueResponse = queueWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/status")
                            .queryParam("gameId", gameId)
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (queueResponse == null || !"RELEASED".equals(queueResponse.get("status"))) {
                log.warn("🚨 Security Handshake Failure! User [{}] attempted checkout access without an active RELEASED token.", userId);
                throw new SecurityException("Access Denied: Your position in the virtual waiting room has not been cleared yet.");
            }
            log.info("🟢 Security Handshake Verified. User [{}] holds an active RELEASED token.", userId);

        } catch (Exception e) {
            if (e instanceof SecurityException) throw e;
            log.error("Network communication failure to queue microservice", e);
            throw new IllegalStateException("Verification infrastructure currently unreachable. Please try again shortly.");
        }
    }

    private double fetchDynamicSeatPrice(Long gameId) {
        log.info("[Thread: {}] Fetching active game catalog information from fairseat-discovery for Game ID #{}",
                Thread.currentThread().getName(), gameId);
        try {
            GameDiscoveryResponseDTO gameCatalog = discoveryWebClient.get()
                    .uri("/{id}", gameId)
                    .retrieve()
                    .bodyToMono(GameDiscoveryResponseDTO.class)
                    .block();

            if (gameCatalog == null || gameCatalog.basePrice() == null) {
                throw new IllegalArgumentException("The requested game event could not be found or has no active pricing configured.");
            }
            log.info("🎯 Dynamic price lookup success! Base Price for Game #{} is {} MAD.", gameId, gameCatalog.basePrice());
            return gameCatalog.basePrice();

        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw e;
            log.error("Network communication failure to discovery microservice", e);
            throw new IllegalStateException("Catalog validation infrastructure currently unreachable. Please try again shortly.");
        }
    }

    private List<RLock> acquireSeatLocks(Long gameId, List<String> seatNumbers) throws InterruptedException {
        List<RLock> acquiredLocks = new ArrayList<>();

        for (String seat : seatNumbers) {
            String lockKey = String.format("lock::game::%d::seat::%s", gameId, seat);
            RLock lock = redissonClient.getLock(lockKey);

            boolean hasLock = lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);
            if (!hasLock) {
                log.warn("❌ [Thread: {}] Failed to acquire lock for seat: {}.", Thread.currentThread().getName(), seat);
                releaseAllLocks(acquiredLocks); // Instantly free any locks acquired so far to prevent deadlocks
                throw new IllegalStateException("One or more selected seats are no longer available. Please choose different seats.");
            }
            acquiredLocks.add(lock);
        }
        return acquiredLocks;
    }

    private Booking saveBookingTransaction(BookingRequestDTO request, double seatPrice) {
        double totalAmount = request.seatNumbers().size() * seatPrice;
        LocalDateTime now = LocalDateTime.now();

        Booking booking = Booking.builder()
                .userId(request.userId())
                .gameId(request.gameId())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plusMinutes(10))
                .build();

        List<BookingItem> items = request.seatNumbers().stream()
                .map(seat -> BookingItem.builder()
                        .booking(booking)
                        .seatNumber(seat)
                        .price(seatPrice)
                        .build())
                .toList();

        booking.setItems(items);
        Booking savedBooking = bookingRepository.save(booking);
        log.info("💾 Saved reservation sequence #{} safely into booking_db.", savedBooking.getId());

        return savedBooking;
    }

    private void publishBookingEvent(Booking savedBooking) {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                savedBooking.getId(),
                savedBooking.getUserId(),
                savedBooking.getGameId(),
                savedBooking.getStatus()
        );

        // Send to the "booking-events" topic using the booking ID as the partition key for strict ordering
        kafkaTemplate.send("booking-events", String.valueOf(savedBooking.getId()), event);
        log.info("📣 Broadcasted BookingConfirmedEvent to Kafka topic [booking-events] for Booking ID #{}", savedBooking.getId());
    }

    private void consumeQueueClearancePass(String userId, Long gameId) {
        log.info("[Thread: {}] Order finalized. Commencing one-time clearance token consumption sequence...", Thread.currentThread().getName());
        try {
            queueWebClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/clearance/consume")
                            .queryParam("gameId", gameId)
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("🎯 Successfully consumed user [{}] waiting-room clearance ticket.", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate waiting room token over network wrapper channels", e);
        }
    }

    private void releaseAllLocks(List<RLock> locks) {
        for (RLock lock : locks) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}