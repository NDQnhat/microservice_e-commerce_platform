package com.ecommerce.payment.client;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.payment.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DefaultOrderClient implements OrderClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderClient.class);

    private final RestClient restClient;
    private final JwtTokenProvider jwtTokenProvider;

    public DefaultOrderClient(@Value("${services.order.url:http://localhost:8086}") String orderUrl,
                              JwtTokenProvider jwtTokenProvider) {
        this.restClient = RestClient.builder()
                .baseUrl(orderUrl)
                .build();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String getOrderStatus(UUID orderId) {
        try {
            String token = jwtTokenProvider.generateSystemToken();
            Map<String, Object> response = restClient.get()
                    .uri("/api/v1/orders/{orderId}", orderId)
                    .header("Authorization", "Bearer " + token)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.get("status") != null) {
                return response.get("status").toString();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch order status from order-service for order {}: {}", orderId, ex.getMessage());
        }
        return null;
    }

    @Override
    public void updateOrderStatus(UUID orderId, String targetStatus, String reason) {
        try {
            String token = jwtTokenProvider.generateSystemToken();
            Map<String, Object> body = new HashMap<>();
            body.put("target_status", targetStatus);
            body.put("note", reason);
            body.put("actor_id", "00000000-0000-0000-0000-000000000000");

            restClient.put()
                    .uri("/api/v1/backoffice/orders/{orderId}/status", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully requested order status update for order {} to {}", orderId, targetStatus);
        } catch (Exception ex) {
            log.warn("Remote call to update order status failed for order {} to {}: {}", orderId, targetStatus, ex.getMessage());
        }
    }
}
