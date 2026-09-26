package com.ecommerce.payment.domain.repository;

import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrderId(UUID orderId);
    List<PaymentTransaction> findByOrderIdOrderByAttemptedAtDesc(UUID orderId);
    Optional<PaymentTransaction> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
    Optional<PaymentTransaction> findByProviderReference(String providerReference);
    Page<PaymentTransaction> findByStatus(PaymentStatus status, Pageable pageable);
    Page<PaymentTransaction> findByStatusIn(Collection<PaymentStatus> statuses, Pageable pageable);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = :status AND p.attemptedAt < :cutoff")
    List<PaymentTransaction> findTimedOutTransactions(@Param("status") PaymentStatus status, @Param("cutoff") Instant cutoff);
}
