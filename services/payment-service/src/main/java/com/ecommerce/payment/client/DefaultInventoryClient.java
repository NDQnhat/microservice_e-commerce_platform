package com.ecommerce.payment.client;

import com.ecommerce.common.context.CorrelationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DefaultInventoryClient implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultInventoryClient.class);

    private final RestClient restClient;

    public DefaultInventoryClient(@Value("${services.inventory.url:http://localhost:8085}") String inventoryUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(inventoryUrl)
                .build();
    }

    @Override
    public void releaseStock(UUID orderId, String reason) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("orderId", orderId);
            body.put("reason", reason);

            restClient.post()
                    .uri("/api/v1/inventory/release?orderId=" + orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully triggered inventory release for order {}: {}", orderId, reason);
        } catch (Exception ex) {
            log.warn("Remote call to releaseStock on inventory-service failed for order {}: {}", orderId, ex.getMessage());
        }
    }
}
