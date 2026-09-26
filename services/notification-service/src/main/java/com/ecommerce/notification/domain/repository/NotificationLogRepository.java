package com.ecommerce.notification.domain.repository;

import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID>, JpaSpecificationExecutor<NotificationLog> {
    Page<NotificationLog> findByStatus(NotificationDeliveryStatus status, Pageable pageable);

    Page<NotificationLog> findByOrderId(UUID orderId, Pageable pageable);

    Page<NotificationLog> findByStatusAndOrderId(NotificationDeliveryStatus status, UUID orderId, Pageable pageable);

    List<NotificationLog> findByOrderId(UUID orderId);
}
