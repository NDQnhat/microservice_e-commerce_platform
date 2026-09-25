package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.CartDto;

import java.util.UUID;

public interface CartClient {

    CartDto getCart(UUID customerId);

    void clearCart(UUID customerId);
}
