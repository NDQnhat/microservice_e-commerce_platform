package com.ecommerce.payment.api.controller;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.payment.api.dto.ManualReconcileRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.domain.model.OutboxEventRecord;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import com.ecommerce.payment.domain.repository.OutboxEventRepository;
import com.ecommerce.payment.domain.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/payments")
public class BackofficePaymentController {

    private final PaymentTransactionRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BackofficePaymentController(PaymentTransactionRepository paymentRepository,
                                       OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/anomalies")
    public ResponseEntity<Page<PaymentTransactionDto>> getAnomalies(
            @RequestParam(defaultValue = "FAILED") PaymentStatus status,
            Pageable pageable) {

        Page<PaymentTransaction> transactions = paymentRepository.findByStatus(status, pageable);
        return ResponseEntity.ok(transactions.map(this::mapToDto));
    }

    @PostMapping("/{transactionId}/reconcile")
    @Transactional
    public ResponseEntity<PaymentTransactionDto> manualReconcile(
            @PathVariable UUID transactionId,
            @Valid @RequestBody ManualReconcileRequest request) {

        PaymentTransaction transaction = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Payment transaction not found: " + transactionId));

        // Enforce BR-012: evidence_reference and reason must be present (enforced via @Valid)
        if (request.getEvidenceReference().trim().isEmpty() || request.getReason().trim().isEmpty()) {
            throw new BusinessRuleException("BR-012", "Manual reconciliation requires evidence reference and logged reason.");
        }

        transaction.setStatus(PaymentStatus.SUCCEEDED);
        transaction.setConfirmedAt(Instant.now());
        PaymentTransaction saved = paymentRepository.save(transaction);

        // Transactional Outbox: PaymentSucceeded
        try {
            String payloadJson = objectMapper.writeValueAsString(saved.getId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Payment",
                    saved.getId().toString(),
                    "PaymentSucceeded",
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PaymentSucceeded event", e);
        }

        return ResponseEntity.ok(mapToDto(saved));
    }

    private PaymentTransactionDto mapToDto(PaymentTransaction tx) {
        return new PaymentTransactionDto(
                tx.getId(),
                tx.getOrderId(),
                tx.getProviderReference(),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getAttemptedAt(),
                tx.getConfirmedAt()
        );
    }
}
