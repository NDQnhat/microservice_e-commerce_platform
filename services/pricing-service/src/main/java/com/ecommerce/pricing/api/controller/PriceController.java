package com.ecommerce.pricing.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.domain.model.Price;
import com.ecommerce.pricing.domain.model.PricePromotion;
import com.ecommerce.pricing.domain.model.PromotionStatus;
import com.ecommerce.pricing.domain.repository.PricePromotionRepository;
import com.ecommerce.pricing.domain.repository.PriceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceRepository priceRepository;
    private final PricePromotionRepository promotionRepository;

    public PriceController(PriceRepository priceRepository,
                           PricePromotionRepository promotionRepository) {
        this.priceRepository = priceRepository;
        this.promotionRepository = promotionRepository;
    }

    @GetMapping("/skus/{skuId}")
    public ResponseEntity<PriceResponse> getPrice(@PathVariable UUID skuId) {
        Price basePrice = priceRepository.findCurrentPriceBySkuId(skuId)
                .orElseThrow(() -> new NotFoundException("Price not found for SKU: " + skuId));

        Optional<PricePromotion> activePromotion = promotionRepository.findActivePromotion(
                skuId, PromotionStatus.ACTIVE, Instant.now());

        BigDecimal promotionalPrice = activePromotion.map(PricePromotion::getSalePrice).orElse(null);
        BigDecimal effectivePrice = (promotionalPrice != null && promotionalPrice.compareTo(basePrice.getBasePrice()) < 0)
                ? promotionalPrice
                : basePrice.getBasePrice();

        return ResponseEntity.ok(new PriceResponse(
                skuId,
                basePrice.getBasePrice(),
                promotionalPrice,
                effectivePrice,
                basePrice.getCurrency()
        ));
    }
}
