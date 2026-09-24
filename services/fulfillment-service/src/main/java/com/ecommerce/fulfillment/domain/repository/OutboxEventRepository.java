package com.ecommerce.fulfillment.domain.repository;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.fulfillment.domain.model.OutboxEventRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventRecord, UUID> {
    List<OutboxEventRecord> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
