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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PricingService {

    private final PriceRepository priceRepository;
    private final PricePromotionRepository promotionRepository;

    public PricingService(PriceRepository priceRepository,
                          PricePromotionRepository promotionRepository) {
        this.priceRepository = priceRepository;
        this.promotionRepository = promotionRepository;
    }

    // ==========================================
    // Backoffice Base Price Management (API-PRC-001)
    // ==========================================

    public PriceResponse setBasePrice(SetPriceRequest request) {
        if (request.getBasePrice() == null || request.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("BR-013", "Base price must be greater than zero");
        }

        Instant now = Instant.now();

        // FR-026 & BR-013: creating a new base price closes prior active record's effective_to
        Optional<Price> currentPrice = priceRepository.findCurrentPriceBySkuId(request.getSkuId());
        currentPrice.ifPresent(p -> {
            p.setEffectiveTo(now);
            priceRepository.save(p);
        });

        Price newPrice = new Price(request.getSkuId(), request.getBasePrice(), request.getCurrency());
        newPrice.setEffectiveFrom(now);
        newPrice.setEffectiveTo(null);
        Price saved = priceRepository.save(newPrice);

        return new PriceResponse(
                saved.getSkuId(),
                saved.getBasePrice(),
                null,
                saved.getBasePrice(),
                saved.getCurrency()
        );
    }

    // ==========================================
    // Backoffice Promotion Management (API-PRC-002)
    // ==========================================

    public void createPromotion(CreatePricePromotionRequest request) {
        if (request.getSalePrice() == null || request.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("BR-013", "Sale price must be greater than zero");
        }
        if (request.getStartAt() == null || request.getEndAt() == null || !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessRuleException("BR-013", "Promotion start time must be before end time");
        }

        PricePromotion promotion = new PricePromotion(
                request.getSkuId(),
                request.getSalePrice(),
                request.getStartAt(),
                request.getEndAt()
        );
        promotionRepository.save(promotion);
    }

    // ==========================================
    // Public Price Query (API-PRC-003)
    // ==========================================

    @Transactional(readOnly = true)
    public PriceResponse getEffectivePrice(UUID skuId) {
        Price basePrice = priceRepository.findCurrentPriceBySkuId(skuId)
                .orElseThrow(() -> new NotFoundException("Price not found for SKU: " + skuId));

        Optional<PricePromotion> activePromotion = promotionRepository.findActivePromotion(
                skuId, PromotionStatus.ACTIVE, Instant.now());

        BigDecimal promotionalPrice = activePromotion.map(PricePromotion::getSalePrice).orElse(null);
        BigDecimal effectivePrice = (promotionalPrice != null && promotionalPrice.compareTo(basePrice.getBasePrice()) < 0)
                ? promotionalPrice
                : basePrice.getBasePrice();

        return new PriceResponse(
                skuId,
                basePrice.getBasePrice(),
                promotionalPrice,
                effectivePrice,
                basePrice.getCurrency()
        );
    }
}
