package com.ecommerce.catalog.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.catalog.api.dto.ProductDto;
import com.ecommerce.catalog.api.dto.SkuDto;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductStatus;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.ecommerce.catalog.domain.repository.SkuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;

    public ProductController(ProductRepository productRepository, SkuRepository skuRepository) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            Pageable pageable) {

        Page<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable);
        } else if (query != null && !query.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndStatus(query, ProductStatus.ACTIVE, pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }

        Page<ProductDto> dtos = products.map(p -> new ProductDto(
                p.getId(),
                p.getCategoryId(),
                p.getName(),
                p.getDescription(),
                p.getStatus().name()
        ));

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProductDetail(@PathVariable UUID productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        return ResponseEntity.ok(new ProductDto(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getStatus().name()
        ));
    }

    @GetMapping("/{productId}/skus")
    public ResponseEntity<List<SkuDto>> getProductSkus(@PathVariable UUID productId) {
        List<SkuDto> skus = skuRepository.findByProductId(productId).stream()
                .map(s -> new SkuDto(s.getId(), s.getProductId(), s.getSkuCode(), s.getStatus().name()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(skus);
    }
}
