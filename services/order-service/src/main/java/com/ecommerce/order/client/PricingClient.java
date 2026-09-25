package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.PriceResponseDto;

import java.util.UUID;

public interface PricingClient {

    PriceResponseDto getEffectivePrice(UUID skuId);
}
