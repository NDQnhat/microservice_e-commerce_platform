package com.ecommerce.order.client;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.order.client.dto.PriceResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DefaultPricingClient implements PricingClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultPricingClient.class);

    private final RestClient restClient;

    public DefaultPricingClient(@Value("${services.pricing.url:http://localhost:8083}") String pricingUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(pricingUrl)
                .build();
    }

    @Override
    public PriceResponseDto getEffectivePrice(UUID skuId) {
        try {
            return restClient.get()
                    .uri("/api/v1/prices/{skuId}/effective", skuId)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .retrieve()
                    .body(PriceResponseDto.class);
        } catch (Exception ex) {
            log.warn("Remote call to pricing-service failed or unavailable for SKU {}: {}", skuId, ex.getMessage());
            return new PriceResponseDto(skuId, new BigDecimal("100000.00"), null, new BigDecimal("100000.00"), "VND");
        }
    }
}
