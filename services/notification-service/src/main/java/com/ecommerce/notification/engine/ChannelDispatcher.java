package com.ecommerce.notification.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ChannelDispatcher.class);

    private final List<ChannelSender> senders;

    public ChannelDispatcher(List<ChannelSender> senders) {
        this.senders = senders;
    }

    /**
     * Dispatches notification with complete fault isolation.
     * Guaranteed never to throw exceptions to caller.
     */
    public SendResult dispatch(String channel, String recipient, String subject, String body, boolean simulateFailure) {
        String effectiveChannel = (channel != null && !channel.isBlank()) ? channel.toUpperCase() : "EMAIL";

        try {
            ChannelSender targetSender = senders.stream()
                    .filter(s -> s.supports(effectiveChannel))
                    .findFirst()
                    .orElse(null);

            if (targetSender == null) {
                log.warn("No sender found for channel [{}]. Available channels: {}", effectiveChannel,
                        senders.stream().map(s -> s.getClass().getSimpleName()).toList());
                return SendResult.failure(effectiveChannel, recipient, "Unsupported channel: " + effectiveChannel);
            }

            return targetSender.send(recipient, subject, body, simulateFailure);
        } catch (Exception ex) {
            log.error("Unexpected exception during dispatch for channel [{}]: {}", effectiveChannel, ex.getMessage(), ex);
            return SendResult.failure(effectiveChannel, recipient, "Dispatch error: " + ex.getMessage());
        }
    }
}
