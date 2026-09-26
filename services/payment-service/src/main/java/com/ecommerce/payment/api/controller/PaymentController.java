package com.ecommerce.payment.api.controller;

import com.ecommerce.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.payment.api.dto.PaymentCallbackRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ==========================================
    // Initiate Payment (PAY-T01: [none] -> INITIATED)
    // ==========================================
    @PostMapping("/initiate")
    public ResponseEntity<PaymentTransactionDto> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {

        PaymentTransactionDto dto = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ==========================================
    // API-PAY-001: Gateway Callback (PAY-T02, PAY-T03)
    // ==========================================
    @PostMapping("/callback")
    public ResponseEntity<PaymentTransactionDto> handleCallback(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @Valid @RequestBody PaymentCallbackRequest request) {

        String secret = webhookSecret != null && !webhookSecret.isBlank() ? webhookSecret : signature;
        PaymentTransactionDto dto = paymentService.handleCallback(request, secret);
        return ResponseEntity.ok(dto);
    }

    // ==========================================
    // Timeout Detection Trigger (PAY-T04: INITIATED -> TIMEOUT)
    // ==========================================
    @PostMapping("/timeout-check")
    public ResponseEntity<Map<String, Object>> checkTimeouts(
            @RequestParam(value = "timeoutMinutes", defaultValue = "15") int timeoutMinutes) {

        int processed = paymentService.checkTimeouts(timeoutMinutes);
        return ResponseEntity.ok(Map.of(
                "processed", processed,
                "timeoutMinutes", timeoutMinutes,
                "timestamp", Instant.now()
        ));
    }
}
