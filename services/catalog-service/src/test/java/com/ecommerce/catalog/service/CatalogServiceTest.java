package com.ecommerce.catalog.service;

import com.ecommerce.catalog.api.dto.*;
import com.ecommerce.catalog.domain.model.*;
import com.ecommerce.catalog.domain.repository.*;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private ProductAttributeRepository attributeRepository;

    @Mock
    private ProductAttributeValueRepository attributeValueRepository;

    @Mock
    private ProductMediaRepository mediaRepository;

    @InjectMocks
    private CatalogService catalogService;

    // ==========================================
    // Public Catalog Browsing (API-CAT-001, 002, 003)
    // ==========================================

    @Test
    @DisplayName("API-CAT-001: getActiveCategories returns active categories only")
    void getActiveCategories_ReturnsActiveOnly() {
        Category active1 = new Category(null, "Electronics", "electronics");
        Category active2 = new Category(active1.getId(), "Smartphones", "smartphones");

        when(categoryRepository.findByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(active1, active2));

        List<CategoryDto> result = catalogService.getActiveCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
        assertThat(result.get(1).getName()).isEqualTo("Smartphones");
    }

    @Test
    @DisplayName("API-CAT-002: searchProducts with category and keyword filter")
    void searchProducts_WithCategoryAndQuery() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product(categoryId, "iPhone 15", "Latest smartphone");
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndStatus(categoryId, "iPhone", ProductStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<ProductDto> result = catalogService.searchProducts("iPhone", categoryId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("API-CAT-002: searchProducts with category filter only")
    void searchProducts_WithCategoryOnly() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product(categoryId, "Smart Watch", "Watch");
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<ProductDto> result = catalogService.searchProducts(null, categoryId, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("API-CAT-002: searchProducts with query filter only")
    void searchProducts_WithQueryOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product(UUID.randomUUID(), "MacBook Pro", "Laptop");
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByNameContainingIgnoreCaseAndStatus("MacBook", ProductStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<ProductDto> result = catalogService.searchProducts("MacBook", null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("API-CAT-003: getProductDetail returns product with active SKUs and sorted media")
    void getProductDetail_Success() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(UUID.randomUUID(), "Mechanical Keyboard", "RGB Gaming");
        product.setId(productId);

        Sku sku = new Sku(productId, "SKU-KB-001");
        ProductMedia media = new ProductMedia(productId, sku.getId(), "https://example.com/img.png", MediaType.IMAGE, 1);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProductId(productId)).thenReturn(List.of(sku));
        when(mediaRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of(media));

        ProductDetailDto detail = catalogService.getProductDetail(productId);

        assertThat(detail.getId()).isEqualTo(productId);
        assertThat(detail.getName()).isEqualTo("Mechanical Keyboard");
        assertThat(detail.getSkus()).hasSize(1);
        assertThat(detail.getSkus().get(0).getSkuCode()).isEqualTo("SKU-KB-001");
        assertThat(detail.getMedia()).hasSize(1);
    }

    @Test
    @DisplayName("API-CAT-003: getProductDetail throws NotFoundException when product is discontinued or missing")
    void getProductDetail_NotFound_ThrowsNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProductDetail(productId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("API-CAT-003: getProductSkus returns SKUs for product")
    void getProductSkus_Success() {
        UUID productId = UUID.randomUUID();
        Sku sku = new Sku(productId, "SKU-001");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(skuRepository.findByProductId(productId)).thenReturn(List.of(sku));

        List<SkuDto> skus = catalogService.getProductSkus(productId);

        assertThat(skus).hasSize(1);
        assertThat(skus.get(0).getSkuCode()).isEqualTo("SKU-001");
    }

    // ==========================================
    // Backoffice Product Lifecycle (API-CAT-004)
    // ==========================================

    @Test
    @DisplayName("API-CAT-004: createProduct auto-creates default SKU per ASM-007 / CQ-011")
    void createProduct_Success_AutoCreatesDefaultSku() {
        UUID categoryId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(categoryId, "T-Shirt", "Cotton shirt");

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = catalogService.createProduct(request);

        assertThat(dto.getName()).isEqualTo("T-Shirt");
        verify(skuRepository).save(any(Sku.class));
    }

    @Test
    @DisplayName("API-CAT-004: createProduct with non-existent category throws NotFoundException")
    void createProduct_CategoryNotFound_ThrowsNotFoundException() {
        UUID categoryId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(categoryId, "T-Shirt", "Cotton shirt");

        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> catalogService.createProduct(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("API-CAT-004: updateProduct successfully modifies product fields")
    void updateProduct_Success() {
        UUID productId = UUID.randomUUID();
        UUID newCategoryId = UUID.randomUUID();
        Product product = new Product(UUID.randomUUID(), "Old Name", "Old Desc");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.existsById(newCategoryId)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest(newCategoryId, "New Name", "New Desc", "ACTIVE");
        ProductDto updated = catalogService.updateProduct(productId, request);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New Desc");
        assertThat(updated.getCategoryId()).isEqualTo(newCategoryId);
    }

    @Test
    @DisplayName("API-CAT-004: discontinueProduct transitions product and SKUs to DISCONTINUED per BR-005")
    void discontinueProduct_Success_EnforcesBR005() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(UUID.randomUUID(), "Tablet", "Description");
        Sku sku = new Sku(productId, "SKU-TAB-1");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProductId(productId)).thenReturn(List.of(sku));

        catalogService.discontinueProduct(productId);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        assertThat(sku.getStatus()).isEqualTo(SkuStatus.DISCONTINUED);
        verify(productRepository).save(product);
        verify(skuRepository).save(sku);
    }

    // ==========================================
    // Backoffice Category Management (API-CAT-005)
    // ==========================================

    @Test
    @DisplayName("API-CAT-005: createCategory creates root category successfully")
    void createCategory_Success_Root() {
        CreateCategoryRequest request = new CreateCategoryRequest(null, "Fashion", "fashion");

        when(categoryRepository.existsByName("Fashion")).thenReturn(false);
        when(categoryRepository.existsBySlug("fashion")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDto created = catalogService.createCategory(request);

        assertThat(created.getName()).isEqualTo("Fashion");
        assertThat(created.getParentCategoryId()).isNull();
    }

    @Test
    @DisplayName("API-CAT-005: createCategory creates child category under root category")
    void createCategory_Success_Child() {
        UUID rootId = UUID.randomUUID();
        Category root = new Category(null, "Electronics", "electronics");
        root.setId(rootId);

        CreateCategoryRequest request = new CreateCategoryRequest(rootId, "Laptops", "laptops");

        when(categoryRepository.existsByName("Laptops")).thenReturn(false);
        when(categoryRepository.existsBySlug("laptops")).thenReturn(false);
        when(categoryRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDto created = catalogService.createCategory(request);

        assertThat(created.getName()).isEqualTo("Laptops");
        assertThat(created.getParentCategoryId()).isEqualTo(rootId);
    }

    @Test
    @DisplayName("API-CAT-005: createCategory enforces CQ-015 max 2 levels nesting depth")
    void createCategory_DepthExceeds2Levels_ThrowsBusinessRuleException() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Category child = new Category(rootId, "Smartphones", "smartphones");
        child.setId(childId);

        CreateCategoryRequest request = new CreateCategoryRequest(childId, "Accessories", "accessories");

        when(categoryRepository.existsByName("Accessories")).thenReturn(false);
        when(categoryRepository.existsBySlug("accessories")).thenReturn(false);
        when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> catalogService.createCategory(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("depth cannot exceed 2 levels");
    }

    @Test
    @DisplayName("API-CAT-005: createCategory throws ConflictException on duplicate name")
    void createCategory_DuplicateName_ThrowsConflictException() {
        CreateCategoryRequest request = new CreateCategoryRequest(null, "Electronics", "electronics-2");
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> catalogService.createCategory(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("name already exists");
    }

    @Test
    @DisplayName("API-CAT-005: updateCategory throws BusinessRuleException on self parent")
    void updateCategory_SelfParent_ThrowsBusinessRuleException() {
        UUID catId = UUID.randomUUID();
        Category category = new Category(null, "Books", "books");
        category.setId(catId);

        when(categoryRepository.findById(catId)).thenReturn(Optional.of(category));

        UpdateCategoryRequest request = new UpdateCategoryRequest(catId, "Books", "books", "ACTIVE");

        assertThatThrownBy(() -> catalogService.updateCategory(catId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    // ==========================================
    // Backoffice SKU / Variant Management (API-CAT-006)
    // ==========================================

    @Test
    @DisplayName("API-CAT-006: createSku enforces BR-014 preventing duplicate attribute combinations")
    void createSku_DuplicateAttributeCombination_ThrowsBusinessRuleException() {
        UUID productId = UUID.randomUUID();
        UUID attrValueId = UUID.randomUUID();
        ProductAttributeValue val = new ProductAttributeValue(UUID.randomUUID(), "Red");
        val.setId(attrValueId);

        Sku existingSku = new Sku(productId, "SKU-RED-1");
        existingSku.setAttributeValues(Set.of(val));

        CreateSkuRequest request = new CreateSkuRequest(productId, "SKU-RED-2", Set.of(attrValueId));

        when(productRepository.existsById(productId)).thenReturn(true);
        when(skuRepository.existsBySkuCode("SKU-RED-2")).thenReturn(false);
        when(attributeValueRepository.findByIdIn(Set.of(attrValueId))).thenReturn(List.of(val));
        when(skuRepository.findByProductId(productId)).thenReturn(List.of(existingSku));

        assertThatThrownBy(() -> catalogService.createSku(productId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Duplicate attribute combination");
    }

    @Test
    @DisplayName("API-CAT-006: createSku successfully creates variant with unique attribute combination")
    void createSku_Success() {
        UUID productId = UUID.randomUUID();
        UUID attrValueId = UUID.randomUUID();
        ProductAttributeValue val = new ProductAttributeValue(UUID.randomUUID(), "Blue");
        val.setId(attrValueId);

        CreateSkuRequest request = new CreateSkuRequest(productId, "SKU-BLUE-1", Set.of(attrValueId));

        when(productRepository.existsById(productId)).thenReturn(true);
        when(skuRepository.existsBySkuCode("SKU-BLUE-1")).thenReturn(false);
        when(attributeValueRepository.findByIdIn(Set.of(attrValueId))).thenReturn(List.of(val));
        when(skuRepository.findByProductId(productId)).thenReturn(Collections.emptyList());
        when(skuRepository.save(any(Sku.class))).thenAnswer(inv -> inv.getArgument(0));

        SkuDto skuDto = catalogService.createSku(productId, request);

        assertThat(skuDto.getSkuCode()).isEqualTo("SKU-BLUE-1");
        assertThat(skuDto.getProductId()).isEqualTo(productId);
    }

    // ==========================================
    // Backoffice Attributes (API-CAT-007)
    // ==========================================

    @Test
    @DisplayName("API-CAT-007: createAttribute successfully saves new attribute")
    void createAttribute_Success() {
        CreateAttributeRequest request = new CreateAttributeRequest("Color");
        when(attributeRepository.existsByName("Color")).thenReturn(false);
        when(attributeRepository.save(any(ProductAttribute.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductAttributeDto dto = catalogService.createAttribute(request);

        assertThat(dto.getName()).isEqualTo("Color");
    }

    @Test
    @DisplayName("API-CAT-007: createAttribute throws ConflictException on duplicate name")
    void createAttribute_Duplicate_ThrowsConflictException() {
        CreateAttributeRequest request = new CreateAttributeRequest("Color");
        when(attributeRepository.existsByName("Color")).thenReturn(true);

        assertThatThrownBy(() -> catalogService.createAttribute(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("API-CAT-007: addAttributeValue adds value to attribute")
    void addAttributeValue_Success() {
        UUID attrId = UUID.randomUUID();
        CreateAttributeValueRequest request = new CreateAttributeValueRequest("Red");

        when(attributeRepository.existsById(attrId)).thenReturn(true);
        when(attributeValueRepository.existsByAttributeIdAndValue(attrId, "Red")).thenReturn(false);
        when(attributeValueRepository.save(any(ProductAttributeValue.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductAttributeValueDto dto = catalogService.addAttributeValue(attrId, request);

        assertThat(dto.getValue()).isEqualTo("Red");
        assertThat(dto.getAttributeId()).isEqualTo(attrId);
    }

    // ==========================================
    // Backoffice Media Gallery (API-CAT-008)
    // ==========================================

    @Test
    @DisplayName("API-CAT-008: addMedia adds image to product gallery")
    void addMedia_Success() {
        UUID productId = UUID.randomUUID();
        CreateMediaRequest request = new CreateMediaRequest(null, "https://cdn.example.com/p1.jpg", "IMAGE", 0);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(mediaRepository.save(any(ProductMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductMediaDto dto = catalogService.addMedia(productId, request);

        assertThat(dto.getUrl()).isEqualTo("https://cdn.example.com/p1.jpg");
        assertThat(dto.getMediaType()).isEqualTo("IMAGE");
    }

    @Test
    @DisplayName("API-CAT-008: addMedia with mismatched SKU throws BusinessRuleException")
    void addMedia_MismatchedSku_ThrowsBusinessRuleException() {
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();

        Sku sku = new Sku(otherProductId, "SKU-OTHER");
        sku.setId(skuId);

        CreateMediaRequest request = new CreateMediaRequest(skuId, "https://cdn.example.com/p1.jpg", "IMAGE", 0);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> catalogService.addMedia(productId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    @DisplayName("API-CAT-008: deleteMedia deletes existing media")
    void deleteMedia_Success() {
        UUID mediaId = UUID.randomUUID();
        ProductMedia media = new ProductMedia();
        media.setId(mediaId);

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        catalogService.deleteMedia(mediaId);

        verify(mediaRepository).delete(media);
    }
}
