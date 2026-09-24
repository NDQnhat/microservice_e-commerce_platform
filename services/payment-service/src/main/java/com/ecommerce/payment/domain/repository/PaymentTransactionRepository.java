package com.ecommerce.payment.domain.repository;

import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrderId(UUID orderId);
    Optional<PaymentTransaction> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
    Page<PaymentTransaction> findByStatus(PaymentStatus status, Pageable pageable);
}
