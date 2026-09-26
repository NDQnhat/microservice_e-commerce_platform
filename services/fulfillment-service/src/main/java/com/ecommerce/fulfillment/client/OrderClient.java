package com.ecommerce.fulfillment.client;

import java.util.UUID;

public interface OrderClient {

    /**
     * Retrieves the current status string of an order from order-service.
     */
    String getOrderStatus(UUID orderId);

    /**
     * Updates an order's status in order-service.
     */
    void updateOrderStatus(UUID orderId, String targetStatus, String reason);
}
