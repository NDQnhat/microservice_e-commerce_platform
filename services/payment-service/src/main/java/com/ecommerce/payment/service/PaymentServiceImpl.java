package com.ecommerce.payment.service;

import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.payment.api.dto.ManualReconcileRequest;
import com.ecommerce.payment.api.dto.PaymentCallbackRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.client.ExceptionClient;
import com.ecommerce.payment.client.InventoryClient;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.domain.event.PaymentFailedEvent;
import com.ecommerce.payment.domain.event.PaymentSucceededEvent;
import com.ecommerce.payment.domain.event.PaymentTimeoutEvent;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import com.ecommerce.payment.domain.repository.PaymentTransactionRepository;
import com.ecommerce.payment.domain.statemachine.PaymentStateMachine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentTransactionRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentOutboxService outboxService;
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final ExceptionClient exceptionClient;
    private final ObjectMapper objectMapper;

    @Value("${payment.webhook.secret:gateway-shared-secret-12345}")
    private String configuredWebhookSecret;

    @Value("${payment.timeout.minutes:15}")
    private int defaultTimeoutMinutes;

    public PaymentServiceImpl(PaymentTransactionRepository paymentRepository,
                              PaymentStateMachine stateMachine,
                              PaymentOutboxService outboxService,
                              OrderClient orderClient,
                              InventoryClient inventoryClient,
                              ExceptionClient exceptionClient,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.stateMachine = stateMachine;
        this.outboxService = outboxService;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.exceptionClient = exceptionClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PaymentTransactionDto initiatePayment(InitiatePaymentRequest request) {
        stateMachine.validateTransition(null, PaymentStatus.INITIATED);

        PaymentTransaction transaction = new PaymentTransaction(request.getOrderId(), request.getAmount());
        PaymentTransaction saved = paymentRepository.save(transaction);
        log.info("Initiated payment transaction {} for order {}", saved.getId(), saved.getOrderId());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PaymentTransactionDto handleCallback(PaymentCallbackRequest request, String webhookSecret) {
        // 1. Webhook Signature / Secret Verification
        verifyWebhookSecret(webhookSecret);

        UUID orderId = request.getOrderId();
        String result = request.getResult();
        String providerRef = request.getProviderReference();

        List<PaymentTransaction> existingTransactions = paymentRepository.findByOrderIdOrderByAttemptedAtDesc(orderId);

        // 2. Idempotency Check (Section 9 Step 6)
        Optional<PaymentTransaction> identicalRetry = existingTransactions.stream()
                .filter(tx -> providerRef != null && providerRef.equalsIgnoreCase(tx.getProviderReference()))
                .filter(tx -> isMatchingResult(tx.getStatus(), result))
                .findFirst();

        if (identicalRetry.isPresent()) {
            log.info("Idempotent callback retry detected for order {} and tx {}. Returning 200 OK without re-processing.",
                    orderId, identicalRetry.get().getId());
            return mapToDto(identicalRetry.get());
        }

        // 3. Late / Stale Callback Check (Section 9 Step 5)
        String currentOrderStatus = orderClient.getOrderStatus(orderId);
        if ("EXPIRED".equalsIgnoreCase(currentOrderStatus) || "CANCELLED".equalsIgnoreCase(currentOrderStatus)) {
            String payload = serializePayload(request);
            exceptionClient.createExceptionRecord(
                    "LATE_OR_STALE_PAYMENT_CALLBACK",
                    orderId.toString(),
                    "ORDER",
                    "LATE_PAYMENT_CALLBACK",
                    String.format("Payment callback received for order in terminal state: %s", currentOrderStatus),
                    payload
            );
            log.warn("Late payment callback received for order {} in state {}. Rejected.", orderId, currentOrderStatus);
            throw new BusinessRuleException("BR-001",
                    String.format("Order is in terminal state %s. Late payment callback cannot be applied.", currentOrderStatus));
        }

        // 4. Duplicate Callback Handling (Section 9 Step 4 & BR-002)
        boolean orderAlreadyPaid = "PAID".equalsIgnoreCase(currentOrderStatus);
        boolean alreadySucceeded = existingTransactions.stream()
                .anyMatch(tx -> tx.getStatus() == PaymentStatus.SUCCEEDED);

        if ("SUCCESS".equalsIgnoreCase(result) && (alreadySucceeded || orderAlreadyPaid)) {
            String payload = serializePayload(request);
            exceptionClient.createExceptionRecord(
                    "DUPLICATE_PAYMENT",
                    orderId.toString(),
                    "ORDER",
                    "BR-002",
                    String.format("Duplicate SUCCESS callback received for order %s which already has a SUCCEEDED payment.", orderId),
                    payload
            );
            log.warn("Duplicate SUCCESS callback received for order {}. Rejected under BR-002.", orderId);
            throw new BusinessRuleException("BR-002",
                    "Order already has a succeeded payment. Duplicate payment rejected.");
        }

        // 5. Standard Processing Flow
        PaymentTransaction tx = existingTransactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.INITIATED)
                .findFirst()
                .orElseGet(() -> new PaymentTransaction(orderId, request.getAmount()));

        if ("SUCCESS".equalsIgnoreCase(result)) {
            stateMachine.validateTransition(tx.getStatus(), PaymentStatus.SUCCEEDED);
            tx.markSucceeded(providerRef);
            PaymentTransaction saved = paymentRepository.save(tx);

            // Publish Outbox Event: PaymentSucceeded
            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    saved.getId(),
                    saved.getOrderId(),
                    saved.getProviderReference(),
                    saved.getAmount(),
                    saved.getConfirmedAt()
            );
            outboxService.recordEvent(saved.getId().toString(), "PaymentSucceeded", event);

            // Drive Order Transition: ORD-T02 (RESERVED -> PAID)
            orderClient.updateOrderStatus(orderId, "PAID", "Payment succeeded via gateway: " + providerRef);

            log.info("Payment succeeded for order {} (tx: {})", orderId, saved.getId());
            return mapToDto(saved);
        } else {
            stateMachine.validateTransition(tx.getStatus(), PaymentStatus.FAILED);
            tx.markFailed(providerRef);
            PaymentTransaction saved = paymentRepository.save(tx);

            // Publish Outbox Event: PaymentFailed
            PaymentFailedEvent event = new PaymentFailedEvent(
                    saved.getId(),
                    saved.getOrderId(),
                    saved.getProviderReference(),
                    saved.getAmount(),
                    "Gateway callback reported payment failure",
                    saved.getConfirmedAt()
            );
            outboxService.recordEvent(saved.getId().toString(), "PaymentFailed", event);

            // Trigger Inventory Compensation (FR-020)
            inventoryClient.releaseStock(orderId, "Payment failed: " + providerRef);

            // Drive Order Transition: ORD-T03 (RESERVED -> PAYMENT_FAILED)
            orderClient.updateOrderStatus(orderId, "PAYMENT_FAILED", "Payment failed via gateway: " + providerRef);

            log.info("Payment failed for order {} (tx: {})", orderId, saved.getId());
            return mapToDto(saved);
        }
    }

    @Override
    @Transactional
    public PaymentTransactionDto reconcilePayment(UUID transactionId, ManualReconcileRequest request) {
        if (request == null ||
                request.getEvidenceReference() == null || request.getEvidenceReference().trim().isEmpty() ||
                request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessRuleException("BR-012", "Manual reconciliation requires evidence reference and logged reason.");
        }

        PaymentTransaction transaction = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Payment transaction not found: " + transactionId));

        if (transaction.getStatus() == PaymentStatus.SUCCEEDED) {
            throw new BusinessRuleException("BR-002", "Payment transaction is already in SUCCEEDED status");
        }

        transaction.markReconciled(request.getEvidenceReference().trim(), request.getReason().trim());
        PaymentTransaction saved = paymentRepository.save(transaction);

        // Transactional Outbox: PaymentSucceeded with reconciliation metadata
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                saved.getId(),
                saved.getOrderId(),
                saved.getProviderReference() != null ? saved.getProviderReference() : request.getEvidenceReference(),
                saved.getAmount(),
                saved.getConfirmedAt(),
                true,
                request.getEvidenceReference(),
                request.getReason()
        );
        outboxService.recordEvent(saved.getId().toString(), "PaymentSucceeded", event);

        // Drive formal Order Transition ORD-T02
        orderClient.updateOrderStatus(saved.getOrderId(), "PAID", "Manual reconciliation: " + request.getReason());

        log.info("Payment transaction {} manually reconciled for order {}", saved.getId(), saved.getOrderId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentTransactionDto> getAnomalies(PaymentStatus status, Pageable pageable) {
        Page<PaymentTransaction> page;
        if (status != null) {
            page = paymentRepository.findByStatus(status, pageable);
        } else {
            page = paymentRepository.findByStatusIn(List.of(PaymentStatus.FAILED, PaymentStatus.TIMEOUT), pageable);
        }
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional
    public int checkTimeouts(int timeoutMinutes) {
        int window = timeoutMinutes > 0 ? timeoutMinutes : defaultTimeoutMinutes;
        Instant cutoff = Instant.now().minus(window, ChronoUnit.MINUTES);

        List<PaymentTransaction> timedOutList = paymentRepository.findTimedOutTransactions(PaymentStatus.INITIATED, cutoff);
        log.info("Found {} timed out payment transactions older than {} minutes", timedOutList.size(), window);

        for (PaymentTransaction tx : timedOutList) {
            tx.markTimeout();
            PaymentTransaction saved = paymentRepository.save(tx);

            // Outbox Event: PaymentTimeout
            PaymentTimeoutEvent event = new PaymentTimeoutEvent(
                    saved.getId(),
                    saved.getOrderId(),
                    saved.getAmount(),
                    saved.getConfirmedAt()
            );
            outboxService.recordEvent(saved.getId().toString(), "PaymentTimeout", event);

            // Trigger Inventory Compensation (FR-020)
            inventoryClient.releaseStock(saved.getOrderId(), "Payment timeout after " + window + " minutes");

            // Drive Order Transition: ORD-T04 (RESERVED -> EXPIRED)
            orderClient.updateOrderStatus(saved.getOrderId(), "EXPIRED", "Payment timeout after " + window + " minutes");

            log.info("Payment transaction {} timed out for order {}", saved.getId(), saved.getOrderId());
        }

        return timedOutList.size();
    }

    private void verifyWebhookSecret(String webhookSecret) {
        if (configuredWebhookSecret == null || configuredWebhookSecret.isBlank()) {
            return;
        }
        if (webhookSecret == null || !webhookSecret.equals(configuredWebhookSecret)) {
            log.warn("Webhook authentication failed. Provided secret did not match configured secret.");
            throw new AuthenticationFailedException("Invalid or missing webhook signature/secret");
        }
    }

    private boolean isMatchingResult(PaymentStatus status, String result) {
        if ("SUCCESS".equalsIgnoreCase(result) && status == PaymentStatus.SUCCEEDED) {
            return true;
        }
        return "FAILED".equalsIgnoreCase(result) && status == PaymentStatus.FAILED;
    }

    private String serializePayload(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
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
                tx.getConfirmedAt(),
                tx.getEvidenceReference(),
                tx.getReconciliationReason()
        );
    }
}
