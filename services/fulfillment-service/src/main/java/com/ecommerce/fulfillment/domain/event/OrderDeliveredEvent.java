package com.ecommerce.fulfillment.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record OrderDeliveredEvent(
        @JsonProperty("shipment_id") UUID shipmentId,
        @JsonProperty("order_id") UUID orderId,
        @JsonProperty("carrier_name") String carrierName,
        @JsonProperty("tracking_code") String trackingCode,
        @JsonProperty("delivered_at") Instant deliveredAt,
        @JsonProperty("occurred_at") Instant occurredAt
) {
    public OrderDeliveredEvent(UUID shipmentId, UUID orderId, String carrierName, String trackingCode, Instant deliveredAt) {
        this(shipmentId, orderId, carrierName, trackingCode, deliveredAt, Instant.now());
    }
}
