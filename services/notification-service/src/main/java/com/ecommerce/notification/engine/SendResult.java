package com.ecommerce.notification.engine;

import java.time.Instant;

public class SendResult {

    private final boolean successful;
    private final String channel;
    private final String recipient;
    private final String errorMessage;
    private final Instant timestamp;

    private SendResult(boolean successful, String channel, String recipient, String errorMessage) {
        this.successful = successful;
        this.channel = channel;
        this.recipient = recipient;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }

    public static SendResult success(String channel, String recipient) {
        return new SendResult(true, channel, recipient, null);
    }

    public static SendResult failure(String channel, String recipient, String errorMessage) {
        return new SendResult(false, channel, recipient, errorMessage);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
