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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RedissonClient redissonClient;
    private final BookingMapper bookingMapper;

    private static final long WAIT_TIME_SECONDS = 2;   // Max time a thread waits in line to grab a lock
    private static final long LEASE_TIME_SECONDS = 10; // Auto-release safety expiration timeout
    private static final double SEAT_FIXED_PRICE = 50.0; // Simulated static ticket price for now

    @Transactional
    public BookingResponseDTO initiateBooking(BookingRequestDTO request) {
        // 1. Enforce strict business guardrail constraint
        if (request.seatNumbers() == null || request.seatNumbers().isEmpty()) {
            throw new IllegalArgumentException("Booking must include at least one seat reservation selection.");
        }
        if (request.seatNumbers().size() > 4) {
            throw new IllegalArgumentException("Business Guardrail Violation: Maximum permitted tickets per booking order is 4.");
        }

        List<RLock> acquiredLocks = new ArrayList<>();
        boolean allLocksAcquired = true;

        log.info("[Thread: {}] Initiating checkout lock acquisition cycle for {} seats...",
                Thread.currentThread().getName(), request.seatNumbers().size());

        try {
            // 2. Loop and attempt to lock every single requested seat sequentially
            for (String seat : request.seatNumbers()) {
                String lockKey = String.format("lock::game::%d::seat::%s", request.gameId(), seat);
                RLock lock = redissonClient.getLock(lockKey);

                boolean hasLock = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
                if (!hasLock) {
                    log.warn("❌ [Thread: {}] Failed to acquire lock for seat: {}. Already locked by someone else.",
                            Thread.currentThread().getName(), seat);
                    allLocksAcquired = false;
                    break; // Break execution out immediately to trigger rolling rollback release
                }

                acquiredLocks.add(lock);
                log.info("🔒 [Thread: {}] Successfully locked key: {}", Thread.currentThread().getName(), lockKey);
            }

            // 3. Rollback handling if any single lock acquisition failed
            if (!allLocksAcquired) {
                releaseAllLocks(acquiredLocks);
                throw new IllegalStateException("One or more selected seats are no longer available. Please choose different seats.");
            }

            // 4. LOCKS ACQUIRED SECURELY -> Safe to execute transactional database writes
            log.info("🎯 [Thread: {}] All seat locks acquired cleanly. Forging database records...", Thread.currentThread().getName());

            double totalAmount = request.seatNumbers().size() * SEAT_FIXED_PRICE;
            LocalDateTime now = LocalDateTime.now();

            // Build parent aggregate order row
            Booking booking = Booking.builder()
                    .userId(request.userId())
                    .gameId(request.gameId())
                    .totalAmount(totalAmount)
                    .status(BookingStatus.PENDING)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(10)) // 10-minute checkout timer countdown window
                    .build();

            // Build and link separate child line items rows
            List<BookingItem> items = request.seatNumbers().stream()
                    .map(seat -> BookingItem.builder()
                            .booking(booking)
                            .seatNumber(seat)
                            .price(SEAT_FIXED_PRICE)
                            .build())
                    .toList();

            booking.setItems(items);

            // Save aggregate tree down to PostgreSQL via JPA cascade settings
            Booking savedBooking = bookingRepository.save(booking);
            log.info("💾 [Thread: {}] Saved reservation sequence #{} safely into booking_db.",
                    Thread.currentThread().getName(), savedBooking.getId());

            return bookingMapper.toResponseDTO(savedBooking);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAllLocks(acquiredLocks);
            throw new RuntimeException("Thread transaction lifecycle interrupted unexpectedly", e);
        }
        // Note: We deliberately DO NOT release the locks inside a 'finally' block here!
        // The locks must stay active in Redis for the full 10-minute checkout window
        // to prevent other users from buying them while this user fills out their payment details.
    }

    private void releaseAllLocks(List<RLock> locks) {
        for (RLock lock : locks) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        log.info("🔓 Released all rollback tracking hold states cleanly.");
    }
}