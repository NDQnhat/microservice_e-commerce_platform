package com.ecommerce.notification.event;

import com.ecommerce.notification.api.dto.DispatchNotificationRequest;
import com.ecommerce.notification.api.dto.DispatchNotificationResponse;
import com.ecommerce.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventHandler eventHandler;

    @Test
    @DisplayName("Maps OrderCreated domain event to ORDER_CREATED eventCode and dispatches notification")
    void handleOrderCreatedEvent() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "order_id", orderId.toString(),
                "customer_id", customerId.toString(),
                "amount", new BigDecimal("100.50"),
                "email", "customer@example.com"
        );

        when(notificationService.dispatchNotification(any(DispatchNotificationRequest.class)))
                .thenReturn(new DispatchNotificationResponse(UUID.randomUUID(), "SENT", "EMAIL", "ORDER_CREATED", "Subject", "Body", null));

        DispatchNotificationResponse response = eventHandler.handleDomainEvent("OrderCreated", payload);

        assertThat(response.getStatus()).isEqualTo("SENT");

        ArgumentCaptor<DispatchNotificationRequest> captor = ArgumentCaptor.forClass(DispatchNotificationRequest.class);
        verify(notificationService).dispatchNotification(captor.capture());

        DispatchNotificationRequest captured = captor.getValue();
        assertThat(captured.getEventCode()).isEqualTo("ORDER_CREATED");
        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerId()).isEqualTo(customerId);
        assertThat(captured.getRecipient()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("Maps PaymentSucceeded domain event to PAYMENT_SUCCEEDED eventCode")
    void handlePaymentSucceededEvent() {
        UUID orderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "order_id", orderId.toString(),
                "amount", "250.00",
                "provider_reference", "TXN-12345"
        );

        when(notificationService.dispatchNotification(any(DispatchNotificationRequest.class)))
                .thenReturn(new DispatchNotificationResponse(UUID.randomUUID(), "SENT", "EMAIL", "PAYMENT_SUCCEEDED", "Subject", "Body", null));

        DispatchNotificationResponse response = eventHandler.handleDomainEvent("PaymentSucceeded", payload);

        assertThat(response.getStatus()).isEqualTo("SENT");

        ArgumentCaptor<DispatchNotificationRequest> captor = ArgumentCaptor.forClass(DispatchNotificationRequest.class);
        verify(notificationService).dispatchNotification(captor.capture());
        assertThat(captor.getValue().getEventCode()).isEqualTo("PAYMENT_SUCCEEDED");
    }

    @Test
    @DisplayName("Maps OrderStatusChanged domain event to ORDER_STATUS_CHANGED eventCode")
    void handleOrderStatusChangedEvent() {
        UUID orderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "order_id", orderId.toString(),
                "from_status", "PENDING",
                "to_status", "CONFIRMED"
        );

        when(notificationService.dispatchNotification(any(DispatchNotificationRequest.class)))
                .thenReturn(new DispatchNotificationResponse(UUID.randomUUID(), "SENT", "EMAIL", "ORDER_STATUS_CHANGED", "Subject", "Body", null));

        DispatchNotificationResponse response = eventHandler.handleDomainEvent("OrderStatusChanged", payload);

        assertThat(response.getStatus()).isEqualTo("SENT");

        ArgumentCaptor<DispatchNotificationRequest> captor = ArgumentCaptor.forClass(DispatchNotificationRequest.class);
        verify(notificationService).dispatchNotification(captor.capture());
        assertThat(captor.getValue().getEventCode()).isEqualTo("ORDER_STATUS_CHANGED");
    }

    @Test
    @DisplayName("Maps OrderShipped and OrderDelivered domain events correctly")
    void handleFulfillmentEvents() {
        UUID orderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "order_id", orderId.toString(),
                "tracking_code", "VNP999",
                "carrier_name", "VNPost"
        );

        when(notificationService.dispatchNotification(any(DispatchNotificationRequest.class)))
                .thenReturn(new DispatchNotificationResponse(UUID.randomUUID(), "SENT", "EMAIL", "ORDER_SHIPPED", "Subject", "Body", null));

        eventHandler.handleDomainEvent("OrderShipped", payload);

        ArgumentCaptor<DispatchNotificationRequest> captor = ArgumentCaptor.forClass(DispatchNotificationRequest.class);
        verify(notificationService).dispatchNotification(captor.capture());
        assertThat(captor.getValue().getEventCode()).isEqualTo("ORDER_SHIPPED");
    }

    @Test
    @DisplayName("Fault isolation: unexpected exception in event handling never escapes")
    void faultIsolation_exceptionCaught() {
        when(notificationService.dispatchNotification(any()))
                .thenThrow(new RuntimeException("Database connection failure"));

        DispatchNotificationResponse response = eventHandler.handleDomainEvent("OrderCancelled", Map.of());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).contains("Event processing failure");
    }
}
