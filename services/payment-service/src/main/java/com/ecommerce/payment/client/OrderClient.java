package com.ecommerce.payment.client;

import java.util.UUID;

public interface OrderClient {
    String getOrderStatus(UUID orderId);
    void updateOrderStatus(UUID orderId, String targetStatus, String reason);
}
