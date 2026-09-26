package com.ecommerce.payment.service;

import com.ecommerce.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.payment.api.dto.ManualReconcileRequest;
import com.ecommerce.payment.api.dto.PaymentCallbackRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {
    PaymentTransactionDto initiatePayment(InitiatePaymentRequest request);
    PaymentTransactionDto handleCallback(PaymentCallbackRequest request, String webhookSecret);
    PaymentTransactionDto reconcilePayment(UUID transactionId, ManualReconcileRequest request);
    Page<PaymentTransactionDto> getAnomalies(PaymentStatus status, Pageable pageable);
    int checkTimeouts(int timeoutMinutes);
}
