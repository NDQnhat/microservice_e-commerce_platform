package com.ecommerce.fulfillment.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        @JsonProperty("shipment_id") UUID shipmentId,
        @JsonProperty("order_id") UUID orderId,
        @JsonProperty("carrier_name") String carrierName,
        @JsonProperty("tracking_code") String trackingCode,
        @JsonProperty("shipped_at") Instant shippedAt,
        @JsonProperty("occurred_at") Instant occurredAt
) {
    public OrderShippedEvent(UUID shipmentId, UUID orderId, String carrierName, String trackingCode, Instant shippedAt) {
        this(shipmentId, orderId, carrierName, trackingCode, shippedAt, Instant.now());
    }
}
