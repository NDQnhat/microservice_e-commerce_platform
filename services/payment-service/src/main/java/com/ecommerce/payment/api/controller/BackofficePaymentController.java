package com.ecommerce.payment.api.controller;

import com.ecommerce.payment.api.dto.ManualReconcileRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/payments")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPER_ADMIN', 'OPS_ADMIN', 'ORDER_OPERATIONS_ADMIN')")
public class BackofficePaymentController {

    private final PaymentService paymentService;

    public BackofficePaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ==========================================
    // API-PAY-002: Query Payment Anomalies (FR-039)
    // ==========================================
    @GetMapping("/anomalies")
    public ResponseEntity<Page<PaymentTransactionDto>> getAnomalies(
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable) {

        Page<PaymentTransactionDto> anomalies = paymentService.getAnomalies(status, pageable);
        return ResponseEntity.ok(anomalies);
    }

    // ==========================================
    // API-PAY-003: Manual Reconciliation (FR-039, BR-012)
    // ==========================================
    @PostMapping("/{transactionId}/reconcile")
    public ResponseEntity<PaymentTransactionDto> manualReconcile(
            @PathVariable UUID transactionId,
            @Valid @RequestBody ManualReconcileRequest request) {

        PaymentTransactionDto dto = paymentService.reconcilePayment(transactionId, request);
        return ResponseEntity.ok(dto);
    }

    // ==========================================
    // Manual Timeout Scan Trigger
    // ==========================================
    @PostMapping("/timeout-check")
    public ResponseEntity<Map<String, Object>> triggerTimeoutCheck(
            @RequestParam(value = "timeoutMinutes", defaultValue = "15") int timeoutMinutes) {

        int processed = paymentService.checkTimeouts(timeoutMinutes);
        return ResponseEntity.ok(Map.of(
                "processed", processed,
                "timeoutMinutes", timeoutMinutes,
                "timestamp", Instant.now()
        ));
    }
}
