package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.model.OrderTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderTimelineEventRepository extends JpaRepository<OrderTimelineEvent, UUID> {
    List<OrderTimelineEvent> findByOrderIdOrderByOccurredAtAsc(UUID orderId);
}
