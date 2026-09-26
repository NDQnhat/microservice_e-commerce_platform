package com.ecommerce.notification.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelSender.class);
    public static final String CHANNEL_NAME = "EMAIL";

    @Override
    public boolean supports(String channel) {
        return CHANNEL_NAME.equalsIgnoreCase(channel);
    }

    @Override
    public SendResult send(String recipient, String subject, String body, boolean simulateFailure) {
        String targetRecipient = recipient != null && !recipient.isBlank() ? recipient : "customer@example.com";

        if (simulateFailure || targetRecipient.toLowerCase().contains("fail") || (subject != null && subject.contains("FAIL_SIMULATION"))) {
            log.warn("Simulated failure or channel unavailable sending email to: {}", targetRecipient);
            return SendResult.failure(CHANNEL_NAME, targetRecipient, "Email delivery failed: SMTP connection timeout or channel unavailable");
        }

        log.info("Sending Email to: [{}] with Subject: [{}]", targetRecipient, subject);
        log.debug("Email Body:\n{}", body);
        return SendResult.success(CHANNEL_NAME, targetRecipient);
    }
}
