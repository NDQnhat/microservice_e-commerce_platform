package com.ecommerce.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DispatchNotificationRequest {

    @NotBlank(message = "Event code is required")
    private String eventCode;

    private String channel; // Optional, defaults to EMAIL

    private UUID orderId;

    private UUID customerId;

    private String recipient; // e.g. email or phone number

    private Map<String, Object> parameters = new HashMap<>();

    private boolean simulateFailure; // For testing and fault isolation simulation

    public DispatchNotificationRequest() {
    }

    public DispatchNotificationRequest(String eventCode, String channel, UUID orderId, UUID customerId,
                                       String recipient, Map<String, Object> parameters, boolean simulateFailure) {
        this.eventCode = eventCode;
        this.channel = channel;
        this.orderId = orderId;
        this.customerId = customerId;
        this.recipient = recipient;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.simulateFailure = simulateFailure;
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

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
