package com.pfa.fairseatpayment.service;

import com.pfa.fairseatpayment.domain.PaymentStatus;
import com.pfa.fairseatpayment.dto.PaymentRequestDTO;
import com.pfa.fairseatpayment.dto.PaymentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final StringRedisTemplate redisTemplate;

    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        // 1. Construct the unique cache key for this specific payment attempt
        String cacheKey = "idempotency:payment:" + request.idempotencyKey();

        // 2. IDEMPOTENCY CHECK: Did we already process this exact request?
        String existingTransactionId = redisTemplate.opsForValue().get(cacheKey);

        if (existingTransactionId != null) {
            log.warn("♻️ Idempotency trigger! Duplicate request detected for key [{}]. Returning cached result.", request.idempotencyKey());
            return new PaymentResponseDTO(
                    existingTransactionId,
                    PaymentStatus.COMPLETED,
                    "Payment already processed successfully."
            );
        }

        // 3. Process the actual payment (Simulating calling Stripe or a Bank Gateway)
        log.info("💳 Processing new payment of {} MAD for Booking #{} (User: {})",
                request.amount(), request.bookingId(), request.userId());

        simulateBankNetworkDelay();
        String newTransactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 4. ATOMIC SAVE: Save the transaction ID to Redis for 24 hours.
        // If the user clicks pay again tomorrow, we let it through. If they click it in 5 minutes, we block it.
        redisTemplate.opsForValue().set(cacheKey, newTransactionId, 24, TimeUnit.HOURS);

        log.info("✅ Payment successful! Generated Transaction ID: {}", newTransactionId);

        return new PaymentResponseDTO(
                newTransactionId,
                PaymentStatus.COMPLETED,
                "Payment processed successfully."
        );
    }

    private void simulateBankNetworkDelay() {
        try {
            Thread.sleep(1500); // Simulate a 1.5 second delay communicating with the bank
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}