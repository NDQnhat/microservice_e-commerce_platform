package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.*;
import com.ecommerce.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/backoffice", "/api/v1/backoffice/catalog"})
public class BackofficeCatalogController {

    private final CatalogService catalogService;

    public BackofficeCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ==========================================
    // Category Management (API-CAT-005)
    // ==========================================

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto created = catalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable UUID categoryId,
                                                      @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryDto updated = catalogService.updateCategory(categoryId, request);
        return ResponseEntity.ok(updated);
    }

    // ==========================================
    // Product Lifecycle Management (API-CAT-004)
    // ==========================================

    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto created = catalogService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable UUID productId,
                                                    @Valid @RequestBody UpdateProductRequest request) {
        ProductDto updated = catalogService.updateProduct(productId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> discontinueProduct(@PathVariable UUID productId) {
        catalogService.discontinueProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // SKU / Variant Management (API-CAT-006)
    // ==========================================

    @PostMapping("/products/{productId}/skus")
    public ResponseEntity<SkuDto> createSkuForProduct(@PathVariable UUID productId,
                                                      @Valid @RequestBody CreateSkuRequest request) {
        request.setProductId(productId);
        SkuDto created = catalogService.createSku(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/skus")
    public ResponseEntity<SkuDto> createSku(@Valid @RequestBody CreateSkuRequest request) {
        SkuDto created = catalogService.createSku(request.getProductId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/skus/{skuId}")
    public ResponseEntity<SkuDto> updateSku(@PathVariable UUID skuId,
                                            @Valid @RequestBody UpdateSkuRequest request) {
        SkuDto updated = catalogService.updateSku(skuId, request);
        return ResponseEntity.ok(updated);
    }

    // ==========================================
    // Attribute Management (API-CAT-007)
    // ==========================================

    @PostMapping("/attributes")
    public ResponseEntity<ProductAttributeDto> createAttribute(@Valid @RequestBody CreateAttributeRequest request) {
        ProductAttributeDto created = catalogService.createAttribute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/attributes")
    public ResponseEntity<List<ProductAttributeDto>> getAllAttributes() {
        return ResponseEntity.ok(catalogService.getAllAttributes());
    }

    @PostMapping("/attributes/{attributeId}/values")
    public ResponseEntity<ProductAttributeValueDto> addAttributeValue(@PathVariable UUID attributeId,
                                                                      @Valid @RequestBody CreateAttributeValueRequest request) {
        ProductAttributeValueDto created = catalogService.addAttributeValue(attributeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==========================================
    // Media Gallery Management (API-CAT-008)
    // ==========================================

    @PostMapping("/products/{productId}/media")
    public ResponseEntity<ProductMediaDto> addProductMedia(@PathVariable UUID productId,
                                                           @Valid @RequestBody CreateMediaRequest request) {
        ProductMediaDto created = catalogService.addMedia(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
        catalogService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }
}
