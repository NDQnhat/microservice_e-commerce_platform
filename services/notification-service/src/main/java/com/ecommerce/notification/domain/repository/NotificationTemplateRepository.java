package com.ecommerce.notification.domain.repository;

import com.ecommerce.notification.domain.model.NotificationTemplate;
import com.ecommerce.notification.domain.model.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByEventCodeAndChannelAndStatus(String eventCode, String channel, TemplateStatus status);
}
