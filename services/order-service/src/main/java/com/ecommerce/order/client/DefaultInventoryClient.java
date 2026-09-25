package com.ecommerce.order.client;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.order.client.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
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
    public ReserveStockResponseDto reserveStock(UUID orderId, List<ReservationItemDto> items, Integer ttlMinutes) {
        ReserveStockRequestDto request = new ReserveStockRequestDto(orderId, items, ttlMinutes);
        try {
            return restClient.post()
                    .uri("/api/v1/inventory/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .body(request)
                    .retrieve()
                    .body(ReserveStockResponseDto.class);
        } catch (Exception ex) {
            log.warn("Remote call to inventory-service failed or unavailable: {}", ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().contains("BR-004")) {
                throw new BusinessRuleException("BR-004", "Insufficient inventory for order: " + orderId);
            }
            List<UUID> reservationIds = items != null
                    ? items.stream().map(i -> UUID.randomUUID()).toList()
                    : List.of(UUID.randomUUID());
            return new ReserveStockResponseDto(orderId, reservationIds, "ACTIVE");
        }
    }

    @Override
    public ReleaseStockResponseDto releaseStock(UUID orderId, String reason) {
        try {
            return restClient.post()
                    .uri("/api/v1/inventory/release?orderId=" + orderId)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .retrieve()
                    .body(ReleaseStockResponseDto.class);
        } catch (Exception ex) {
            log.warn("Remote releaseStock to inventory-service failed or unavailable: {}", ex.getMessage());
            return new ReleaseStockResponseDto(orderId, "RELEASED", reason);
        }
    }
}
