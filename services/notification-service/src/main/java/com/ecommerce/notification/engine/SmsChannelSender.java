package com.ecommerce.notification.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsChannelSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(SmsChannelSender.class);
    public static final String CHANNEL_NAME = "SMS";

    @Override
    public boolean supports(String channel) {
        return CHANNEL_NAME.equalsIgnoreCase(channel);
    }

    @Override
    public SendResult send(String recipient, String subject, String body, boolean simulateFailure) {
        String targetRecipient = recipient != null && !recipient.isBlank() ? recipient : "+10000000000";

        if (simulateFailure || targetRecipient.toLowerCase().contains("fail") || (body != null && body.contains("FAIL_SIMULATION"))) {
            log.warn("Simulated failure or channel unavailable sending SMS to: {}", targetRecipient);
            return SendResult.failure(CHANNEL_NAME, targetRecipient, "SMS delivery failed: Telephony gateway error");
        }

        log.info("Sending SMS to: [{}]", targetRecipient);
        log.debug("SMS Content:\n{}", body);
        return SendResult.success(CHANNEL_NAME, targetRecipient);
    }
}
