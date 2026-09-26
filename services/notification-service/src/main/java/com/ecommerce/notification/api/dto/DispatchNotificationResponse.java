package com.ecommerce.notification.api.dto;

import java.util.UUID;

public class DispatchNotificationResponse {

    private UUID notificationLogId;
    private String status;
    private String channel;
    private String eventCode;
    private String renderedSubject;
    private String renderedBody;
    private String errorMessage;

    public DispatchNotificationResponse() {
    }

    public DispatchNotificationResponse(UUID notificationLogId, String status, String channel,
                                        String eventCode, String renderedSubject, String renderedBody,
                                        String errorMessage) {
        this.notificationLogId = notificationLogId;
        this.status = status;
        this.channel = channel;
        this.eventCode = eventCode;
        this.renderedSubject = renderedSubject;
        this.renderedBody = renderedBody;
        this.errorMessage = errorMessage;
    }

    public UUID getNotificationLogId() {
        return notificationLogId;
    }

    public void setNotificationLogId(UUID notificationLogId) {
        this.notificationLogId = notificationLogId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public void setRenderedSubject(String renderedSubject) {
        this.renderedSubject = renderedSubject;
    }

    public String getRenderedBody() {
        return renderedBody;
    }

    public void setRenderedBody(String renderedBody) {
        this.renderedBody = renderedBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
