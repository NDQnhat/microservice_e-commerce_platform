package com.ecommerce.pricing.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.pricing.api.dto.CreatePricePromotionRequest;
import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.api.dto.SetPriceRequest;
import com.ecommerce.pricing.service.PricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficePricingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private BackofficePricingController backofficePricingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backofficePricingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API-PRC-001: POST /api/v1/backoffice/prices returns 201 Created with PriceResponse")
    void setPrice_Success() throws Exception {
        UUID skuId = UUID.randomUUID();
        SetPriceRequest request = new SetPriceRequest(skuId, new BigDecimal("350000.00"), "VND");
        PriceResponse response = new PriceResponse(skuId, new BigDecimal("350000.00"), null, new BigDecimal("350000.00"), "VND");

        when(pricingService.setBasePrice(any(SetPriceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.base_price").value(350000.00))
                .andExpect(jsonPath("$.effective_price").value(350000.00));
    }

    @Test
    @DisplayName("API-PRC-001: POST /api/v1/backoffice/pricing/prices (legacy path) returns 201 Created")
    void setPrice_LegacyPath_Success() throws Exception {
        UUID skuId = UUID.randomUUID();
        SetPriceRequest request = new SetPriceRequest(skuId, new BigDecimal("350000.00"), "VND");
        PriceResponse response = new PriceResponse(skuId, new BigDecimal("350000.00"), null, new BigDecimal("350000.00"), "VND");

        when(pricingService.setBasePrice(any(SetPriceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/pricing/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()));
    }

    @Test
    @DisplayName("API-PRC-001: POST /api/v1/backoffice/prices returns 400 on zero or negative price")
    void setPrice_ValidationError() throws Exception {
        UUID skuId = UUID.randomUUID();
        SetPriceRequest request = new SetPriceRequest(skuId, BigDecimal.ZERO, "VND");

        mockMvc.perform(post("/api/v1/backoffice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("API-PRC-002: POST /api/v1/backoffice/prices/promotions returns 201 Created")
    void createPromotion_Success() throws Exception {
        UUID skuId = UUID.randomUUID();
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(7, ChronoUnit.DAYS);

        CreatePricePromotionRequest request = new CreatePricePromotionRequest(
                skuId, new BigDecimal("250000.00"), start, end);

        doNothing().when(pricingService).createPromotion(any(CreatePricePromotionRequest.class));

        mockMvc.perform(post("/api/v1/backoffice/prices/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(pricingService).createPromotion(any(CreatePricePromotionRequest.class));
    }

    @Test
    @DisplayName("API-PRC-002: POST /api/v1/backoffice/prices/promotions returns 422 on invalid dates per BR-013")
    void createPromotion_InvalidDates_Returns422() throws Exception {
        UUID skuId = UUID.randomUUID();
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);

        CreatePricePromotionRequest request = new CreatePricePromotionRequest(
                skuId, new BigDecimal("250000.00"), start, end);

        doThrow(new BusinessRuleException("BR-013", "Promotion start time must be before end time"))
                .when(pricingService).createPromotion(any(CreatePricePromotionRequest.class));

        mockMvc.perform(post("/api/v1/backoffice/prices/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-013"))
                .andExpect(jsonPath("$.detail").value("[BR-013] Promotion start time must be before end time"));
    }
}
