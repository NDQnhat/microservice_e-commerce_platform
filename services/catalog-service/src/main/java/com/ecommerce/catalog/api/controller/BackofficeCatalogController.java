package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.*;
import com.ecommerce.catalog.domain.model.*;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import com.ecommerce.catalog.domain.repository.ProductMediaRepository;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.ecommerce.catalog.domain.repository.SkuRepository;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/catalog")
public class BackofficeCatalogController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final ProductMediaRepository mediaRepository;

    public BackofficeCatalogController(CategoryRepository categoryRepository,
                                       ProductRepository productRepository,
                                       SkuRepository skuRepository,
                                       ProductMediaRepository mediaRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.mediaRepository = mediaRepository;
    }

    @PostMapping("/categories")
    @Transactional
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category name already exists: " + request.getName());
        }
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Category slug already exists: " + request.getSlug());
        }

        if (request.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Parent category not found"));

            if (parent.getParentCategoryId() != null) {
                throw new BusinessRuleException("BR-015", "Category nesting depth cannot exceed 2 levels");
            }
        }

        Category category = new Category(request.getParentCategoryId(), request.getName(), request.getSlug());
        Category saved = categoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CategoryDto(
                saved.getId(),
                saved.getParentCategoryId(),
                saved.getName(),
                saved.getSlug(),
                saved.getStatus().name()
        ));
    }

    @PostMapping("/products")
    @Transactional
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new NotFoundException("Category not found: " + request.getCategoryId());
        }

        Product product = new Product(request.getCategoryId(), request.getName(), request.getDescription());
        Product saved = productRepository.save(product);

        // Auto-create default SKU per ASM-007
        String defaultSkuCode = "SKU-" + saved.getId().toString().substring(0, 8).toUpperCase();
        Sku defaultSku = new Sku(saved.getId(), defaultSkuCode);
        skuRepository.save(defaultSku);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ProductDto(
                saved.getId(),
                saved.getCategoryId(),
                saved.getName(),
                saved.getDescription(),
                saved.getStatus().name()
        ));
    }

    @PostMapping("/skus")
    @Transactional
    public ResponseEntity<SkuDto> createSku(@Valid @RequestBody CreateSkuRequest request) {
        if (!productRepository.existsById(request.getProductId())) {
            throw new NotFoundException("Product not found: " + request.getProductId());
        }
        if (skuRepository.existsBySkuCode(request.getSkuCode())) {
            throw new ConflictException("SKU code already exists: " + request.getSkuCode());
        }

        Sku sku = new Sku(request.getProductId(), request.getSkuCode());
        Sku saved = skuRepository.save(sku);

        return ResponseEntity.status(HttpStatus.CREATED).body(new SkuDto(
                saved.getId(),
                saved.getProductId(),
                saved.getSkuCode(),
                saved.getStatus().name()
        ));
    }

    @DeleteMapping("/products/{productId}")
    @Transactional
    public ResponseEntity<Void> discontinueProduct(@PathVariable UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        // Enforce BR-005: hard delete disallowed; transition to DISCONTINUED
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);

        return ResponseEntity.noContent().build();
    }
}
