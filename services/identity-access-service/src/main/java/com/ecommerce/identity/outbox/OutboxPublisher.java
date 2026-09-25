package com.ecommerce.identity.outbox;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.identity.domain.model.OutboxEventRecord;
import com.ecommerce.identity.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String DEFAULT_TOPIC = "identity.events";
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventRecord> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, 50)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        for (OutboxEventRecord record : pendingEvents) {
            processRecord(record, kafkaTemplate);
        }
    }

    public void processRecord(OutboxEventRecord record, KafkaTemplate<String, String> kafkaTemplate) {
        try {
            String topic = resolveTopic(record.getEventType());

            if (kafkaTemplate != null) {
                kafkaTemplate.send(topic, record.getAggregateId(), record.getPayload()).get();
            } else {
                log.info("KafkaTemplate not configured; simulating successful dispatch for outbox event {}", record.getId());
            }

            record.setStatus(OutboxStatus.SENT);
            record.setSentAt(Instant.now());
            record.setErrorMessage(null);
            outboxEventRepository.save(record);
            log.info("Successfully published outbox event {} of type {}", record.getId(), record.getEventType());
        } catch (Exception ex) {
            int retries = record.getRetryCount() + 1;
            record.setRetryCount(retries);
            record.setErrorMessage(ex.getMessage());

            if (retries >= MAX_RETRIES) {
                record.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} permanently failed after {} retries: {}", record.getId(), retries, ex.getMessage());
            } else {
                log.warn("Outbox event {} delivery failed (attempt {}/{}): {}", record.getId(), retries, MAX_RETRIES, ex.getMessage());
            }
            outboxEventRepository.save(record);
        }
    }

    private String resolveTopic(String eventType) {
        if ("RoleAssigned".equals(eventType)) {
            return "identity.events";
        }
        return DEFAULT_TOPIC;
    }
}
