package com.ecommerce.payment.client;

import java.util.UUID;

public interface InventoryClient {
    void releaseStock(UUID orderId, String reason);
}
