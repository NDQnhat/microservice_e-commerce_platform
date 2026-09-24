package com.ecommerce.pricing.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.pricing.api.dto.CreatePricePromotionRequest;
import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.api.dto.SetPriceRequest;
import com.ecommerce.pricing.domain.model.Price;
import com.ecommerce.pricing.domain.model.PricePromotion;
import com.ecommerce.pricing.domain.repository.PricePromotionRepository;
import com.ecommerce.pricing.domain.repository.PriceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/backoffice/pricing")
public class BackofficePricingController {

    private final PriceRepository priceRepository;
    private final PricePromotionRepository promotionRepository;

    public BackofficePricingController(PriceRepository priceRepository,
                                       PricePromotionRepository promotionRepository) {
        this.priceRepository = priceRepository;
        this.promotionRepository = promotionRepository;
    }

    @PostMapping("/prices")
    @Transactional
    public ResponseEntity<PriceResponse> setPrice(@Valid @RequestBody SetPriceRequest request) {
        Instant now = Instant.now();

        // FR-026: creating a new price closes prior active record's effective_to
        Optional<Price> currentPrice = priceRepository.findCurrentPriceBySkuId(request.getSkuId());
        currentPrice.ifPresent(p -> {
            p.setEffectiveTo(now);
            priceRepository.save(p);
        });

        Price newPrice = new Price(request.getSkuId(), request.getBasePrice(), request.getCurrency());
        newPrice.setEffectiveFrom(now);
        Price saved = priceRepository.save(newPrice);

        return ResponseEntity.status(HttpStatus.CREATED).body(new PriceResponse(
                saved.getSkuId(),
                saved.getBasePrice(),
                null,
                saved.getBasePrice(),
                saved.getCurrency()
        ));
    }

    @PostMapping("/promotions")
    @Transactional
    public ResponseEntity<Void> createPromotion(@Valid @RequestBody CreatePricePromotionRequest request) {
        if (!request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessRuleException("BR-013", "Promotion start time must be before end time");
        }

        PricePromotion promotion = new PricePromotion(
                request.getSkuId(),
                request.getSalePrice(),
                request.getStartAt(),
                request.getEndAt()
        );
        promotionRepository.save(promotion);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
