package com.ecommerce.pricing.api.controller;

import com.ecommerce.pricing.api.dto.PriceResponse;
import com.ecommerce.pricing.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PricingService pricingService;

    public PriceController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping({"/{skuId}/effective", "/skus/{skuId}"})
    public ResponseEntity<PriceResponse> getPrice(@PathVariable UUID skuId) {
        return ResponseEntity.ok(pricingService.getEffectivePrice(skuId));
    }
}
