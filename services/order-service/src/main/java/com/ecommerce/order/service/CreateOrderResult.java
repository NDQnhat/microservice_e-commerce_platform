package com.ecommerce.order.service;

import com.ecommerce.order.api.dto.OrderResponse;

public class CreateOrderResult {

    private final OrderResponse response;
    private final boolean isReplay;

    public CreateOrderResult(OrderResponse response, boolean isReplay) {
        this.response = response;
        this.isReplay = isReplay;
    }

    public OrderResponse getResponse() {
        return response;
    }

    public boolean isReplay() {
        return isReplay;
    }
}
