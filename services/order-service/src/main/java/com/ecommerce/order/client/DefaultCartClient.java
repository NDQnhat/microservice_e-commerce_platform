package com.ecommerce.order.client;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.order.client.dto.CartDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.UUID;

@Component
public class DefaultCartClient implements CartClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultCartClient.class);

    private final RestClient restClient;

    public DefaultCartClient(@Value("${services.cart.url:http://localhost:8084}") String cartUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(cartUrl)
                .build();
    }

    @Override
    public CartDto getCart(UUID customerId) {
        try {
            return restClient.get()
                    .uri("/api/v1/customers/{customerId}/cart", customerId)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .retrieve()
                    .body(CartDto.class);
        } catch (Exception ex) {
            log.warn("Remote call to cart-service failed or unavailable for customer {}: {}", customerId, ex.getMessage());
            return new CartDto(UUID.randomUUID(), customerId, "ACTIVE", Collections.emptyList());
        }
    }

    @Override
    public void clearCart(UUID customerId) {
        try {
            restClient.delete()
                    .uri("/api/v1/customers/{customerId}/cart", customerId)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Remote clearCart on cart-service failed or unavailable for customer {}: {}", customerId, ex.getMessage());
        }
    }
}
