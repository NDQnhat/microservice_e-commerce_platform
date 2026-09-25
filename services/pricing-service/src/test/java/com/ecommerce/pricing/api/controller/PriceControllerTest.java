package com.ecommerce.pricing.api.controller;

import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PriceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private PriceController priceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(priceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API-PRC-003: GET /api/v1/prices/{skuId}/effective returns 200 with PriceResponse")
    void getPrice_Effective_Success() throws Exception {
        UUID skuId = UUID.randomUUID();
        PriceResponse response = new PriceResponse(
                skuId,
                new BigDecimal("500000.00"),
                new BigDecimal("450000.00"),
                new BigDecimal("450000.00"),
                "VND"
        );

        when(pricingService.getEffectivePrice(skuId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/prices/{skuId}/effective", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.base_price").value(500000.00))
                .andExpect(jsonPath("$.promotional_price").value(450000.00))
                .andExpect(jsonPath("$.effective_price").value(450000.00))
                .andExpect(jsonPath("$.currency").value("VND"));
    }

    @Test
    @DisplayName("API-PRC-003: GET /api/v1/prices/skus/{skuId} returns 200 with PriceResponse")
    void getPrice_SkusPath_Success() throws Exception {
        UUID skuId = UUID.randomUUID();
        PriceResponse response = new PriceResponse(
                skuId,
                new BigDecimal("200000.00"),
                null,
                new BigDecimal("200000.00"),
                "VND"
        );

        when(pricingService.getEffectivePrice(skuId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/prices/skus/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.effective_price").value(200000.00));
    }

    @Test
    @DisplayName("API-PRC-003: GET /api/v1/prices/{skuId}/effective returns 404 when SKU price not found")
    void getPrice_NotFound() throws Exception {
        UUID skuId = UUID.randomUUID();
        when(pricingService.getEffectivePrice(skuId))
                .thenThrow(new NotFoundException("Price not found for SKU: " + skuId));

        mockMvc.perform(get("/api/v1/prices/{skuId}/effective", skuId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Price not found for SKU: " + skuId));
    }
}
