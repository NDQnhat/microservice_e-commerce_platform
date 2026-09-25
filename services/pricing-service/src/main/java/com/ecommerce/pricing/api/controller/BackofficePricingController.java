package com.ecommerce.pricing.api.controller;

import com.ecommerce.pricing.api.dto.CreatePricePromotionRequest;
import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.api.dto.SetPriceRequest;
import com.ecommerce.pricing.service.PricingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/backoffice", "/api/v1/backoffice/pricing"})
public class BackofficePricingController {

    private final PricingService pricingService;

    public BackofficePricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/prices")
    public ResponseEntity<PriceResponse> setPrice(@Valid @RequestBody SetPriceRequest request) {
        PriceResponse response = pricingService.setBasePrice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/prices/promotions", "/promotions"})
    public ResponseEntity<Void> createPromotion(@Valid @RequestBody CreatePricePromotionRequest request) {
        pricingService.createPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
