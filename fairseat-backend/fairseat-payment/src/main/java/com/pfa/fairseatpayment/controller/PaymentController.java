package com.pfa.fairseatpayment.controller;

import com.pfa.fairseatpayment.dto.PaymentRequestDTO;
import com.pfa.fairseatpayment.dto.PaymentResponseDTO;
import com.pfa.fairseatpayment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<PaymentResponseDTO> chargeCard(@Valid @RequestBody PaymentRequestDTO request) {
        PaymentResponseDTO response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}