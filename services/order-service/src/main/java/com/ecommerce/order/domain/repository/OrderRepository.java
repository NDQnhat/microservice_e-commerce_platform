package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Page<Order> findByCustomerIdOrderByPlacedAtDesc(UUID customerId, Pageable pageable);
    Page<Order> findByStatusOrderByPlacedAtDesc(OrderStatus status, Pageable pageable);
    List<Order> findByStatusAndPlacedAtBefore(OrderStatus status, Instant cutoff);
}
