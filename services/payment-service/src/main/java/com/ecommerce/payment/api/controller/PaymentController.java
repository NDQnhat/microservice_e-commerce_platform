package com.ecommerce.payment.api.controller;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.payment.api.dto.PaymentCallbackRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.domain.model.OutboxEventRecord;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import com.ecommerce.payment.domain.repository.OutboxEventRepository;
import com.ecommerce.payment.domain.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentTransactionRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentTransactionRepository paymentRepository,
                             OutboxEventRepository outboxEventRepository,
                             ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/initiate")
    @Transactional
    public ResponseEntity<PaymentTransactionDto> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {

        PaymentTransaction transaction = new PaymentTransaction(request.getOrderId(), request.getAmount());
        PaymentTransaction saved = paymentRepository.save(transaction);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @PostMapping("/callback")
    @Transactional
    public ResponseEntity<PaymentTransactionDto> handleCallback(
            @Valid @RequestBody PaymentCallbackRequest request) {

        List<PaymentTransaction> transactions = paymentRepository.findByOrderId(request.getOrderId());
        Optional<PaymentTransaction> initiatedTx = transactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.INITIATED)
                .findFirst();

        // BR-002: Check for duplicate SUCCESS callback
        boolean alreadySucceeded = transactions.stream()
                .anyMatch(t -> t.getStatus() == PaymentStatus.SUCCEEDED);

        if ("SUCCESS".equalsIgnoreCase(request.getResult())) {
            if (alreadySucceeded) {
                throw new BusinessRuleException("BR-002", "Order already has a succeeded payment. Duplicate rejected.");
            }

            PaymentTransaction tx = initiatedTx.orElseGet(() ->
                    new PaymentTransaction(request.getOrderId(), request.getAmount()));

            tx.setProviderReference(request.getProviderReference());
            tx.setStatus(PaymentStatus.SUCCEEDED);
            tx.setConfirmedAt(Instant.now());
            PaymentTransaction saved = paymentRepository.save(tx);

            // Outbox: PaymentSucceeded
            publishOutboxEvent("PaymentSucceeded", saved);

            return ResponseEntity.ok(mapToDto(saved));
        } else {
            PaymentTransaction tx = initiatedTx.orElseGet(() ->
                    new PaymentTransaction(request.getOrderId(), request.getAmount()));

            tx.setProviderReference(request.getProviderReference());
            tx.setStatus(PaymentStatus.FAILED);
            tx.setConfirmedAt(Instant.now());
            PaymentTransaction saved = paymentRepository.save(tx);

            // Outbox: PaymentFailed
            publishOutboxEvent("PaymentFailed", saved);

            return ResponseEntity.ok(mapToDto(saved));
        }
    }

    private void publishOutboxEvent(String eventType, PaymentTransaction tx) {
        try {
            String payloadJson = objectMapper.writeValueAsString(tx.getId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Payment",
                    tx.getId().toString(),
                    eventType,
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event: " + eventType, e);
        }
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
