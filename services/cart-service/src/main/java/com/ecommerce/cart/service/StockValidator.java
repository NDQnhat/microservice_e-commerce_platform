package com.ecommerce.cart.service;

import java.util.UUID;

public interface StockValidator {
    void validateStock(UUID skuId, int requestedQuantity);
}
