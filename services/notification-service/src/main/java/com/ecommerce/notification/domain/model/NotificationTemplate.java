package com.ecommerce.notification.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    private UUID id;

    @Column(name = "event_code", nullable = false)
    private String eventCode;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status;

    public NotificationTemplate() {
        this.id = UUID.randomUUID();
        this.status = TemplateStatus.ACTIVE;
    }

    public NotificationTemplate(String eventCode, String channel, String subject, String bodyTemplate) {
        this.id = UUID.randomUUID();
        this.eventCode = eventCode;
        this.channel = channel;
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
        this.status = TemplateStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }
}
