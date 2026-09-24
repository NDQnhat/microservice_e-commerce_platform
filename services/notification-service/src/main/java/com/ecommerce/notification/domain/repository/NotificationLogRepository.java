package com.ecommerce.notification.domain.repository;

import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    Page<NotificationLog> findByStatus(NotificationDeliveryStatus status, Pageable pageable);
}
