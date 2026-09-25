package com.ecommerce.pricing.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.pricing.api.dto.CreatePricePromotionRequest;
import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.api.dto.SetPriceRequest;
import com.ecommerce.pricing.domain.model.Price;
import com.ecommerce.pricing.domain.model.PricePromotion;
import com.ecommerce.pricing.domain.model.PromotionStatus;
import com.ecommerce.pricing.domain.repository.PricePromotionRepository;
import com.ecommerce.pricing.domain.repository.PriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private PricePromotionRepository promotionRepository;

    @InjectMocks
    private PricingService pricingService;

    // ==========================================
    // Base Price Management (API-PRC-001)
    // ==========================================

    @Test
    @DisplayName("API-PRC-001: setBasePrice closes previous active price and creates new active price per BR-013 / FR-026")
    void setBasePrice_Success_ClosesPriorActivePrice() {
        UUID skuId = UUID.randomUUID();
        Price oldPrice = new Price(skuId, new BigDecimal("100000.00"), "VND");
        oldPrice.setEffectiveFrom(Instant.now().minus(10, ChronoUnit.DAYS));

        when(priceRepository.findCurrentPriceBySkuId(skuId)).thenReturn(Optional.of(oldPrice));
        when(priceRepository.save(any(Price.class))).thenAnswer(inv -> inv.getArgument(0));

        SetPriceRequest request = new SetPriceRequest(skuId, new BigDecimal("120000.00"), "VND");
        PriceResponse response = pricingService.setBasePrice(request);

        assertThat(response.getSkuId()).isEqualTo(skuId);
        assertThat(response.getBasePrice()).isEqualTo(new BigDecimal("120000.00"));
        assertThat(response.getEffectivePrice()).isEqualTo(new BigDecimal("120000.00"));

        // Verify old price was closed
        assertThat(oldPrice.getEffectiveTo()).isNotNull();

        // Verify two saves: one to close old price, one to persist new price
        verify(priceRepository, times(2)).save(any(Price.class));
    }

    @Test
    @DisplayName("API-PRC-001: setBasePrice throws BusinessRuleException if price <= 0")
    void setBasePrice_NonPositivePrice_ThrowsBusinessRuleException() {
        UUID skuId = UUID.randomUUID();
        SetPriceRequest request = new SetPriceRequest(skuId, BigDecimal.ZERO, "VND");

        assertThatThrownBy(() -> pricingService.setBasePrice(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must be greater than zero");
    }

    // ==========================================
    // Promotion Management (API-PRC-002)
    // ==========================================

    @Test
    @DisplayName("API-PRC-002: createPromotion saves active promotional price rule")
    void createPromotion_Success() {
        UUID skuId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plus(7, ChronoUnit.DAYS);

        CreatePricePromotionRequest request = new CreatePricePromotionRequest(
                skuId, new BigDecimal("80000.00"), start, end);

        pricingService.createPromotion(request);

        ArgumentCaptor<PricePromotion> captor = ArgumentCaptor.forClass(PricePromotion.class);
        verify(promotionRepository).save(captor.capture());

        PricePromotion saved = captor.getValue();
        assertThat(saved.getSkuId()).isEqualTo(skuId);
        assertThat(saved.getSalePrice()).isEqualTo(new BigDecimal("80000.00"));
        assertThat(saved.getStatus()).isEqualTo(PromotionStatus.ACTIVE);
    }

    @Test
    @DisplayName("API-PRC-002: createPromotion throws BusinessRuleException if startAt >= endAt per BR-013")
    void createPromotion_InvalidDates_ThrowsBusinessRuleException() {
        UUID skuId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.minus(1, ChronoUnit.DAYS);

        CreatePricePromotionRequest request = new CreatePricePromotionRequest(
                skuId, new BigDecimal("80000.00"), start, end);

        assertThatThrownBy(() -> pricingService.createPromotion(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("start time must be before end time");
    }

    @Test
    @DisplayName("API-PRC-002: createPromotion throws BusinessRuleException if salePrice <= 0")
    void createPromotion_NonPositiveSalePrice_ThrowsBusinessRuleException() {
        UUID skuId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.DAYS);

        CreatePricePromotionRequest request = new CreatePricePromotionRequest(
                skuId, BigDecimal.ZERO, start, end);

        assertThatThrownBy(() -> pricingService.createPromotion(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must be greater than zero");
    }

    // ==========================================
    // Effective Price Resolution (API-PRC-003)
    // ==========================================

    @Test
    @DisplayName("API-PRC-003: getEffectivePrice returns salePrice when active promotion is lower than basePrice")
    void getEffectivePrice_ReturnsPromotion_WhenSalePriceLower() {
        UUID skuId = UUID.randomUUID();
        Price base = new Price(skuId, new BigDecimal("200000.00"), "VND");
        PricePromotion promo = new PricePromotion(
                skuId, new BigDecimal("150000.00"),
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        when(priceRepository.findCurrentPriceBySkuId(skuId)).thenReturn(Optional.of(base));
        when(promotionRepository.findActivePromotion(eq(skuId), eq(PromotionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(Optional.of(promo));

        PriceResponse response = pricingService.getEffectivePrice(skuId);

        assertThat(response.getBasePrice()).isEqualTo(new BigDecimal("200000.00"));
        assertThat(response.getPromotionalPrice()).isEqualTo(new BigDecimal("150000.00"));
        assertThat(response.getEffectivePrice()).isEqualTo(new BigDecimal("150000.00"));
    }

    @Test
    @DisplayName("API-PRC-003: getEffectivePrice returns basePrice when active promotion is higher than basePrice")
    void getEffectivePrice_ReturnsBasePrice_WhenPromoHigher() {
        UUID skuId = UUID.randomUUID();
        Price base = new Price(skuId, new BigDecimal("200000.00"), "VND");
        PricePromotion promo = new PricePromotion(
                skuId, new BigDecimal("250000.00"),
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        when(priceRepository.findCurrentPriceBySkuId(skuId)).thenReturn(Optional.of(base));
        when(promotionRepository.findActivePromotion(eq(skuId), eq(PromotionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(Optional.of(promo));

        PriceResponse response = pricingService.getEffectivePrice(skuId);

        assertThat(response.getBasePrice()).isEqualTo(new BigDecimal("200000.00"));
        assertThat(response.getPromotionalPrice()).isEqualTo(new BigDecimal("250000.00"));
        assertThat(response.getEffectivePrice()).isEqualTo(new BigDecimal("200000.00"));
    }

    @Test
    @DisplayName("API-PRC-003: getEffectivePrice returns basePrice when no active promotion exists")
    void getEffectivePrice_ReturnsBasePrice_WhenNoPromotion() {
        UUID skuId = UUID.randomUUID();
        Price base = new Price(skuId, new BigDecimal("200000.00"), "VND");

        when(priceRepository.findCurrentPriceBySkuId(skuId)).thenReturn(Optional.of(base));
        when(promotionRepository.findActivePromotion(eq(skuId), eq(PromotionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(Optional.empty());

        PriceResponse response = pricingService.getEffectivePrice(skuId);

        assertThat(response.getBasePrice()).isEqualTo(new BigDecimal("200000.00"));
        assertThat(response.getPromotionalPrice()).isNull();
        assertThat(response.getEffectivePrice()).isEqualTo(new BigDecimal("200000.00"));
    }

    @Test
    @DisplayName("API-PRC-003: getEffectivePrice throws NotFoundException when no price exists for SKU")
    void getEffectivePrice_PriceNotFound_ThrowsNotFoundException() {
        UUID skuId = UUID.randomUUID();
        when(priceRepository.findCurrentPriceBySkuId(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingService.getEffectivePrice(skuId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Price not found for SKU");
    }
}
