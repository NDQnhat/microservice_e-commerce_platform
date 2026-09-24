package com.ecommerce.notification.api.dto;

import java.util.UUID;

public class NotificationTemplateDto {

    private UUID id;
    private String eventCode;
    private String channel;
    private String subject;
    private String bodyTemplate;
    private String status;

    public NotificationTemplateDto() {
    }

    public NotificationTemplateDto(UUID id, String eventCode, String channel,
                                   String subject, String bodyTemplate, String status) {
        this.id = id;
        this.eventCode = eventCode;
        this.channel = channel;
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
