package com.ecommerce.notification.domain.repository;

import com.ecommerce.notification.domain.model.NotificationTemplate;
import com.ecommerce.notification.domain.model.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID>, JpaSpecificationExecutor<NotificationTemplate> {
    Optional<NotificationTemplate> findByEventCodeAndChannelAndStatus(String eventCode, String channel, TemplateStatus status);

    Optional<NotificationTemplate> findFirstByEventCodeAndChannelAndStatus(String eventCode, String channel, TemplateStatus status);

    Optional<NotificationTemplate> findFirstByEventCodeAndStatus(String eventCode, TemplateStatus status);

    List<NotificationTemplate> findByEventCodeAndStatus(String eventCode, TemplateStatus status);
}
