package com.ecommerce.catalog.service;

import com.ecommerce.catalog.api.dto.*;
import com.ecommerce.catalog.domain.model.*;
import com.ecommerce.catalog.domain.repository.*;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final ProductMediaRepository mediaRepository;

    public CatalogService(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          SkuRepository skuRepository,
                          ProductAttributeRepository attributeRepository,
                          ProductAttributeValueRepository attributeValueRepository,
                          ProductMediaRepository mediaRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.attributeRepository = attributeRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.mediaRepository = mediaRepository;
    }

    // ==========================================
    // Public Catalog Browsing (API-CAT-001, 002, 003)
    // ==========================================

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(String query, UUID categoryId, Pageable pageable) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasCategory = categoryId != null;

        Page<Product> products;
        if (hasCategory && hasQuery) {
            products = productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndStatus(
                    categoryId, query.trim(), ProductStatus.ACTIVE, pageable);
        } else if (hasCategory) {
            products = productRepository.findByCategoryIdAndStatus(
                    categoryId, ProductStatus.ACTIVE, pageable);
        } else if (hasQuery) {
            products = productRepository.findByNameContainingIgnoreCaseAndStatus(
                    query.trim(), ProductStatus.ACTIVE, pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }

        return products.map(this::toProductDto);
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProductDetail(UUID productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        List<Sku> skus = skuRepository.findByProductId(productId).stream()
                .filter(s -> s.getStatus() == SkuStatus.ACTIVE)
                .collect(Collectors.toList());

        List<SkuDetailDto> skuDetailDtos = skus.stream()
                .map(this::toSkuDetailDto)
                .collect(Collectors.toList());

        List<ProductMediaDto> mediaDtos = mediaRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(this::toMediaDto)
                .collect(Collectors.toList());

        return new ProductDetailDto(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                skuDetailDtos,
                mediaDtos
        );
    }

    @Transactional(readOnly = true)
    public List<SkuDto> getProductSkus(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found: " + productId);
        }

        return skuRepository.findByProductId(productId).stream()
                .map(this::toSkuDto)
                .collect(Collectors.toList());
    }

    // ==========================================
    // Backoffice Product Lifecycle (API-CAT-004)
    // ==========================================

    public ProductDto createProduct(CreateProductRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new NotFoundException("Category not found: " + request.getCategoryId());
        }

        Product product = new Product(request.getCategoryId(), request.getName(), request.getDescription());
        Product saved = productRepository.save(product);

        // Auto-create default SKU per ASM-007 & CQ-011
        String defaultSkuCode = "SKU-" + saved.getId().toString().substring(0, 8).toUpperCase();
        Sku defaultSku = new Sku(saved.getId(), defaultSkuCode);
        skuRepository.save(defaultSku);

        return toProductDto(saved);
    }

    public ProductDto updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new NotFoundException("Category not found: " + request.getCategoryId());
            }
            product.setCategoryId(request.getCategoryId());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            product.setStatus(ProductStatus.valueOf(request.getStatus()));
        }

        product.setUpdatedAt(Instant.now());
        Product saved = productRepository.save(product);
        return toProductDto(saved);
    }

    public void discontinueProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        // Enforce BR-005: hard delete disallowed; transition to DISCONTINUED
        product.setStatus(ProductStatus.DISCONTINUED);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);

        // Discontinue associated SKUs
        List<Sku> skus = skuRepository.findByProductId(productId);
        for (Sku sku : skus) {
            sku.setStatus(SkuStatus.DISCONTINUED);
            skuRepository.save(sku);
        }
    }

    // ==========================================
    // Backoffice Category Management (API-CAT-005)
    // ==========================================

    public CategoryDto createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category name already exists: " + request.getName());
        }
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Category slug already exists: " + request.getSlug());
        }

        if (request.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Parent category not found"));

            // CQ-015: Maximum category nesting depth is 2 levels
            if (parent.getParentCategoryId() != null) {
                throw new BusinessRuleException("BR-015", "Category nesting depth cannot exceed 2 levels");
            }
        }

        Category category = new Category(request.getParentCategoryId(), request.getName(), request.getSlug());
        Category saved = categoryRepository.save(category);
        return toCategoryDto(saved);
    }

    public CategoryDto updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));

        if (request.getName() != null && !request.getName().isBlank()) {
            if (categoryRepository.existsByNameAndIdNot(request.getName(), categoryId)) {
                throw new ConflictException("Category name already exists: " + request.getName());
            }
            category.setName(request.getName().trim());
        }

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), categoryId)) {
                throw new ConflictException("Category slug already exists: " + request.getSlug());
            }
            category.setSlug(request.getSlug().trim());
        }

        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(categoryId)) {
                throw new BusinessRuleException("BR-015", "Category cannot be its own parent");
            }

            Category parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Parent category not found: " + request.getParentCategoryId()));

            if (parent.getParentCategoryId() != null) {
                throw new BusinessRuleException("BR-015", "Category nesting depth cannot exceed 2 levels");
            }

            if (categoryRepository.existsByParentCategoryId(categoryId)) {
                throw new BusinessRuleException("BR-015", "Category with subcategories cannot become a child category");
            }

            category.setParentCategoryId(request.getParentCategoryId());
        }

        if (request.getStatus() != null) {
            category.setStatus(CategoryStatus.valueOf(request.getStatus()));
        }

        Category saved = categoryRepository.save(category);
        return toCategoryDto(saved);
    }

    // ==========================================
    // Backoffice SKU / Variant Management (API-CAT-006)
    // ==========================================

    public SkuDto createSku(UUID productId, CreateSkuRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found: " + productId);
        }
        if (skuRepository.existsBySkuCode(request.getSkuCode())) {
            throw new ConflictException("SKU code already exists: " + request.getSkuCode());
        }

        Set<ProductAttributeValue> attributeValues = resolveAttributeValues(request.getAttributeValueIds());

        // Enforce BR-014: Duplicate attribute combinations for the same product are strictly prohibited
        if (!attributeValues.isEmpty()) {
            validateAttributeCombinationUniqueness(productId, null, attributeValues);
        }

        Sku sku = new Sku(productId, request.getSkuCode());
        sku.setAttributeValues(attributeValues);
        Sku saved = skuRepository.save(sku);

        return toSkuDto(saved);
    }

    public SkuDto updateSku(UUID skuId, UpdateSkuRequest request) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new NotFoundException("SKU not found: " + skuId));

        if (request.getSkuCode() != null && !request.getSkuCode().equals(sku.getSkuCode())) {
            if (skuRepository.existsBySkuCode(request.getSkuCode())) {
                throw new ConflictException("SKU code already exists: " + request.getSkuCode());
            }
            sku.setSkuCode(request.getSkuCode());
        }

        if (request.getStatus() != null) {
            sku.setStatus(SkuStatus.valueOf(request.getStatus()));
        }

        if (request.getAttributeValueIds() != null) {
            Set<ProductAttributeValue> attributeValues = resolveAttributeValues(request.getAttributeValueIds());
            if (!attributeValues.isEmpty()) {
                validateAttributeCombinationUniqueness(sku.getProductId(), skuId, attributeValues);
            }
            sku.setAttributeValues(attributeValues);
        }

        Sku saved = skuRepository.save(sku);
        return toSkuDto(saved);
    }

    // ==========================================
    // Backoffice Attribute Management (API-CAT-007)
    // ==========================================

    public ProductAttributeDto createAttribute(CreateAttributeRequest request) {
        if (attributeRepository.existsByName(request.getName())) {
            throw new ConflictException("Attribute name already exists: " + request.getName());
        }

        ProductAttribute attribute = new ProductAttribute(request.getName());
        ProductAttribute saved = attributeRepository.save(attribute);
        return new ProductAttributeDto(saved.getId(), saved.getName(), Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public List<ProductAttributeDto> getAllAttributes() {
        return attributeRepository.findAll().stream()
                .map(attr -> {
                    List<ProductAttributeValueDto> values = attributeValueRepository.findByAttributeId(attr.getId()).stream()
                            .map(v -> new ProductAttributeValueDto(v.getId(), v.getAttributeId(), v.getValue()))
                            .collect(Collectors.toList());
                    return new ProductAttributeDto(attr.getId(), attr.getName(), values);
                })
                .collect(Collectors.toList());
    }

    public ProductAttributeValueDto addAttributeValue(UUID attributeId, CreateAttributeValueRequest request) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new NotFoundException("Attribute not found: " + attributeId);
        }
        if (attributeValueRepository.existsByAttributeIdAndValue(attributeId, request.getValue())) {
            throw new ConflictException("Attribute value already exists: " + request.getValue());
        }

        ProductAttributeValue value = new ProductAttributeValue(attributeId, request.getValue());
        ProductAttributeValue saved = attributeValueRepository.save(value);
        return new ProductAttributeValueDto(saved.getId(), saved.getAttributeId(), saved.getValue());
    }

    // ==========================================
    // Backoffice Media Gallery Management (API-CAT-008)
    // ==========================================

    public ProductMediaDto addMedia(UUID productId, CreateMediaRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found: " + productId);
        }

        if (request.getSkuId() != null) {
            Sku sku = skuRepository.findById(request.getSkuId())
                    .orElseThrow(() -> new NotFoundException("SKU not found: " + request.getSkuId()));
            if (!sku.getProductId().equals(productId)) {
                throw new BusinessRuleException("BR-016", "SKU does not belong to specified product");
            }
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.valueOf(request.getMediaType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("BR-017", "Invalid media type: " + request.getMediaType());
        }

        ProductMedia media = new ProductMedia(
                productId,
                request.getSkuId(),
                request.getUrl(),
                mediaType,
                request.getSortOrder()
        );
        ProductMedia saved = mediaRepository.save(media);
        return toMediaDto(saved);
    }

    public void deleteMedia(UUID mediaId) {
        ProductMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found: " + mediaId));
        mediaRepository.delete(media);
    }

    // ==========================================
    // Helpers & Mappers
    // ==========================================

    private Set<ProductAttributeValue> resolveAttributeValues(Set<UUID> valueIds) {
        if (valueIds == null || valueIds.isEmpty()) {
            return new HashSet<>();
        }
        List<ProductAttributeValue> values = attributeValueRepository.findByIdIn(valueIds);
        if (values.size() != valueIds.size()) {
            throw new NotFoundException("One or more attribute values could not be found");
        }
        return new HashSet<>(values);
    }

    private void validateAttributeCombinationUniqueness(UUID productId, UUID excludeSkuId, Set<ProductAttributeValue> targetValues) {
        Set<UUID> targetValueIds = targetValues.stream()
                .map(ProductAttributeValue::getId)
                .collect(Collectors.toSet());

        List<Sku> existingSkus = skuRepository.findByProductId(productId);
        for (Sku existingSku : existingSkus) {
            if (excludeSkuId != null && existingSku.getId().equals(excludeSkuId)) {
                continue;
            }
            Set<UUID> existingIds = existingSku.getAttributeValues().stream()
                    .map(ProductAttributeValue::getId)
                    .collect(Collectors.toSet());

            if (!existingIds.isEmpty() && existingIds.equals(targetValueIds)) {
                throw new BusinessRuleException("BR-014", "Duplicate attribute combination for product");
            }
        }
    }

    private CategoryDto toCategoryDto(Category c) {
        return new CategoryDto(
                c.getId(),
                c.getParentCategoryId(),
                c.getName(),
                c.getSlug(),
                c.getStatus().name()
        );
    }

    private ProductDto toProductDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getCategoryId(),
                p.getName(),
                p.getDescription(),
                p.getStatus().name()
        );
    }

    private SkuDto toSkuDto(Sku s) {
        return new SkuDto(
                s.getId(),
                s.getProductId(),
                s.getSkuCode(),
                s.getStatus().name()
        );
    }

    private SkuDetailDto toSkuDetailDto(Sku s) {
        Set<ProductAttributeValueDto> attrDtos = s.getAttributeValues().stream()
                .map(v -> new ProductAttributeValueDto(v.getId(), v.getAttributeId(), v.getValue()))
                .collect(Collectors.toSet());

        return new SkuDetailDto(
                s.getId(),
                s.getProductId(),
                s.getSkuCode(),
                s.getStatus().name(),
                attrDtos
        );
    }

    private ProductMediaDto toMediaDto(ProductMedia m) {
        return new ProductMediaDto(
                m.getId(),
                m.getProductId(),
                m.getSkuId(),
                m.getUrl(),
                m.getMediaType().name(),
                m.getSortOrder()
        );
    }
}
