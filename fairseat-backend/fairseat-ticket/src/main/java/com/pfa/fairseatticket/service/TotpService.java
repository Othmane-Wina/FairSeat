package com.pfa.fairseatticket.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

@Service
public class TotpService {

    // FairSeat requires a fast 15-second rotation window to prevent screenshotting
    private static final int TIME_WINDOW_SECONDS = 15;

    public String generateTotp(String secret) {
        try {
            // 1. Calculate the current time slice (Ticks every 15 seconds)
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long timeSlice = currentTimeSeconds / TIME_WINDOW_SECONDS;

            // 2. Convert time slice to an 8-byte array
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeSlice).array();

            // 3. Initialize the HMAC-SHA1 Cryptographic function using the ticket's secret
            byte[] keyBytes = secret.getBytes();
            SecretKeySpec signKey = new SecretKeySpec(keyBytes, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signKey);

            // 4. Hash the time with the secret
            byte[] hash = mac.doFinal(timeBytes);

            // 5. Dynamic Truncation (Extract a 6-digit number from the hash)
            int offset = hash[hash.length - 1] & 0xF;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= 1000000; // Constrain to 6 digits

            // 6. Format with leading zeros if necessary
            return String.format("%06d", truncatedHash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to securely generate TOTP hash", e);
        }
    }
}