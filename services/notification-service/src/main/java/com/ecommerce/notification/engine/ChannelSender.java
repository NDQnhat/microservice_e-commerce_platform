package com.ecommerce.notification.engine;

public interface ChannelSender {

    boolean supports(String channel);

    SendResult send(String recipient, String subject, String body, boolean simulateFailure);
}
