package com.pfa.fairseatbooking.service;

import com.pfa.fairseatbooking.domain.Booking;
import com.pfa.fairseatbooking.domain.BookingItem;
import com.pfa.fairseatbooking.domain.BookingStatus;
import com.pfa.fairseatbooking.dto.BookingRequestDTO;
import com.pfa.fairseatbooking.dto.BookingResponseDTO;
import com.pfa.fairseatbooking.mapper.BookingMapper;
import com.pfa.fairseatbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    private final WebClient queueWebClient; // Injected our communication client

    private static final long WAIT_TIME_SECONDS = 2;
    private static final long LEASE_TIME_SECONDS = 10;
    private static final double SEAT_FIXED_PRICE = 50.0;

    @Transactional
    public BookingResponseDTO initiateBooking(BookingRequestDTO request) {
        // 1. Structural Guardrail Check
        if (request.seatNumbers() == null || request.seatNumbers().isEmpty()) {
            throw new IllegalArgumentException("Booking must include at least one seat reservation selection.");
        }
        if (request.seatNumbers().size() > 4) {
            throw new IllegalArgumentException("Business Guardrail Violation: Maximum permitted tickets per booking order is 4.");
        }

        // 2. CRITICAL NEW TASK: Inter-Service Security Handshake (Verify Gate Pass)
        log.info("[Thread: {}] Sending background verification check to fairseat-queue for User [{}]",
                Thread.currentThread().getName(), request.userId());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> queueResponse = queueWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/status")
                            .queryParam("gameId", request.gameId())
                            .queryParam("userId", request.userId())
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Safe to use blocking extraction because Project Loom handles virtual execution fluidly

            if (queueResponse == null || !"RELEASED".equals(queueResponse.get("status"))) {
                log.warn("🚨 Security Handshake Failure! User [{}] attempted checkout access without an active RELEASED token.", request.userId());
                throw new SecurityException("Access Denied: Your position in the virtual waiting room has not been cleared yet.");
            }

            log.info("🟢 Security Handshake Verified. User [{}] holds an active RELEASED token. Proceeding to transactional locking...", request.userId());

        } catch (Exception e) {
            if (e instanceof SecurityException) throw e;
            log.error("Network communication failure to queue microservice", e);
            throw new IllegalStateException("Verification infrastructure currently unreachable. Please try again shortly.");
        }

        // 3. LOCKS ACQUISITION CYCLE
        List<RLock> acquiredLocks = new ArrayList<>();
        boolean allLocksAcquired = true;

        try {
            for (String seat : request.seatNumbers()) {
                String lockKey = String.format("lock::game::%d::seat::%s", request.gameId(), seat);
                RLock lock = redissonClient.getLock(lockKey);

                boolean hasLock = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
                if (!hasLock) {
                    log.warn("❌ [Thread: {}] Failed to acquire lock for seat: {}.", Thread.currentThread().getName(), seat);
                    allLocksAcquired = false;
                    break;
                }
                acquiredLocks.add(lock);
            }

            if (!allLocksAcquired) {
                releaseAllLocks(acquiredLocks);
                throw new IllegalStateException("One or more selected seats are no longer available. Please choose different seats.");
            }

            // 4. WRITE TARGET TO POSTGRESQL
            double totalAmount = request.seatNumbers().size() * SEAT_FIXED_PRICE;
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
                            .price(SEAT_FIXED_PRICE)
                            .build())
                    .toList();

            booking.setItems(items);

            Booking savedBooking = bookingRepository.save(booking);
            log.info("💾 Saved reservation sequence #{} safely into booking_db.", savedBooking.getId());

            log.info("[Thread: {}] Order finalized. Commencing one-time clearance token consumption sequence...", Thread.currentThread().getName());
            try {
                queueWebClient.delete()
                        .uri(uriBuilder -> uriBuilder
                                .path("/clearance/consume")
                                .queryParam("gameId", request.gameId())
                                .queryParam("userId", request.userId())
                                .build())
                        .retrieve()
                        .toBodilessEntity()
                        .block();
                log.info("🎯 Successfully consumed user [{}] waiting-room clearance ticket.", request.userId());
            } catch (Exception e) {
                log.error("Failed to invalidate waiting room token over network wrapper channels", e);
                // In production, you could push a failure message to a dead-letter queue here
            }

            return bookingMapper.toResponseDTO(savedBooking);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAllLocks(acquiredLocks);
            throw new RuntimeException("Thread transaction lifecycle interrupted unexpectedly", e);
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