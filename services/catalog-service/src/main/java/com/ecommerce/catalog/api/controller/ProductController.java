package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.ProductDetailDto;
import com.ecommerce.catalog.api.dto.ProductDto;
import com.ecommerce.catalog.api.dto.SkuDto;
import com.ecommerce.catalog.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            Pageable pageable) {
        return ResponseEntity.ok(catalogService.searchProducts(query, categoryId, pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailDto> getProductDetail(@PathVariable UUID productId) {
        return ResponseEntity.ok(catalogService.getProductDetail(productId));
    }

    @GetMapping("/{productId}/skus")
    public ResponseEntity<List<SkuDto>> getProductSkus(@PathVariable UUID productId) {
        return ResponseEntity.ok(catalogService.getProductSkus(productId));
    }
}
