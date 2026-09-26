package com.ecommerce.notification.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDispatchServiceTest {

    private ChannelDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        EmailChannelSender emailSender = new EmailChannelSender();
        SmsChannelSender smsSender = new SmsChannelSender();
        dispatcher = new ChannelDispatcher(List.of(emailSender, smsSender));
    }

    @Test
    @DisplayName("Dispatches email notification successfully")
    void shouldDispatchEmailSuccessfully() {
        SendResult result = dispatcher.dispatch("EMAIL", "user@example.com", "Welcome", "Hello Alice", false);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getChannel()).isEqualTo("EMAIL");
        assertThat(result.getRecipient()).isEqualTo("user@example.com");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Dispatches SMS notification successfully")
    void shouldDispatchSmsSuccessfully() {
        SendResult result = dispatcher.dispatch("SMS", "+84901234567", "Code", "Your OTP is 123456", false);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getChannel()).isEqualTo("SMS");
        assertThat(result.getRecipient()).isEqualTo("+84901234567");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Simulated failure in email dispatch returns failure SendResult without throwing")
    void shouldSimulateEmailFailureGracefully() {
        SendResult result = dispatcher.dispatch("EMAIL", "fail@example.com", "Alert", "Body", true);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getChannel()).isEqualTo("EMAIL");
        assertThat(result.getErrorMessage()).contains("Email delivery failed");
    }

    @Test
    @DisplayName("Simulated failure in SMS dispatch returns failure SendResult without throwing")
    void shouldSimulateSmsFailureGracefully() {
        SendResult result = dispatcher.dispatch("SMS", "+12345FAIL", null, "FAIL_SIMULATION message", false);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getChannel()).isEqualTo("SMS");
        assertThat(result.getErrorMessage()).contains("SMS delivery failed");
    }

    @Test
    @DisplayName("Unsupported channel returns failure SendResult without throwing (Fault Isolation)")
    void shouldHandleUnsupportedChannelGracefully() {
        SendResult result = dispatcher.dispatch("PUSH_NOTIFICATION", "token123", "Title", "Body", false);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getChannel()).isEqualTo("PUSH_NOTIFICATION");
        assertThat(result.getErrorMessage()).contains("Unsupported channel");
    }

    @Test
    @DisplayName("Fault isolation: unexpected sender exception is caught and returned as failure result")
    void faultIsolation_senderExceptionCaught() {
        ChannelSender brokenSender = new ChannelSender() {
            @Override
            public boolean supports(String channel) {
                return "BROKEN".equalsIgnoreCase(channel);
            }

            @Override
            public SendResult send(String recipient, String subject, String body, boolean simulateFailure) {
                throw new RuntimeException("Underlying network connection dropped abruptly!");
            }
        };

        ChannelDispatcher faultTolerantDispatcher = new ChannelDispatcher(List.of(brokenSender));

        SendResult result = faultTolerantDispatcher.dispatch("BROKEN", "test@test.com", "Subj", "Body", false);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).contains("Underlying network connection dropped abruptly!");
    }
}
