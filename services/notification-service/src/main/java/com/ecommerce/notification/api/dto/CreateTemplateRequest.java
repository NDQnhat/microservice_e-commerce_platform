package com.ecommerce.notification.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTemplateRequest {

    @NotBlank(message = "Event code is required")
    private String eventCode;

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    public CreateTemplateRequest() {
    }

    public CreateTemplateRequest(String eventCode, String channel, String subject, String bodyTemplate) {
        this.eventCode = eventCode;
        this.channel = channel;
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
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
}
