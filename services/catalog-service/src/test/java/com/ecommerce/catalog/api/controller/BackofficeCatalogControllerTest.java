package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.*;
import com.ecommerce.catalog.service.CatalogService;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficeCatalogControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private BackofficeCatalogController backofficeCatalogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backofficeCatalogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==========================================
    // Category Tests (API-CAT-005)
    // ==========================================

    @Test
    @DisplayName("API-CAT-005: POST /api/v1/backoffice/categories returns 201 Created")
    void createCategory_Success() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(null, "Sports", "sports");
        CategoryDto dto = new CategoryDto(UUID.randomUUID(), null, "Sports", "sports", "ACTIVE");

        when(catalogService.createCategory(any(CreateCategoryRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sports"))
                .andExpect(jsonPath("$.slug").value("sports"));
    }

    @Test
    @DisplayName("API-CAT-005: POST /api/v1/backoffice/categories returns 400 on blank name")
    void createCategory_ValidationError() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(null, "", "slug");

        mockMvc.perform(post("/api/v1/backoffice/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("API-CAT-005: POST /api/v1/backoffice/categories returns 409 on duplicate name")
    void createCategory_ConflictError() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(null, "Sports", "sports");

        when(catalogService.createCategory(any(CreateCategoryRequest.class)))
                .thenThrow(new ConflictException("Category name already exists: Sports"));

        mockMvc.perform(post("/api/v1/backoffice/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("API-CAT-005: POST /api/v1/backoffice/categories returns 422 on CQ-015 depth violation")
    void createCategory_DepthViolation_Returns422() throws Exception {
        UUID parentId = UUID.randomUUID();
        CreateCategoryRequest request = new CreateCategoryRequest(parentId, "Level 3", "level-3");

        when(catalogService.createCategory(any(CreateCategoryRequest.class)))
                .thenThrow(new BusinessRuleException("BR-015", "Category nesting depth cannot exceed 2 levels"));

        mockMvc.perform(post("/api/v1/backoffice/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-015"))
                .andExpect(jsonPath("$.detail").value("[BR-015] Category nesting depth cannot exceed 2 levels"));
    }

    @Test
    @DisplayName("API-CAT-005: PUT /api/v1/backoffice/categories/{id} returns 200 OK")
    void updateCategory_Success() throws Exception {
        UUID catId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest(null, "Sports & Fitness", "sports-fitness", "ACTIVE");
        CategoryDto dto = new CategoryDto(catId, null, "Sports & Fitness", "sports-fitness", "ACTIVE");

        when(catalogService.updateCategory(eq(catId), any(UpdateCategoryRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/backoffice/categories/{id}", catId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sports & Fitness"));
    }

    // ==========================================
    // Product Lifecycle Tests (API-CAT-004)
    // ==========================================

    @Test
    @DisplayName("API-CAT-004: POST /api/v1/backoffice/products returns 201 Created")
    void createProduct_Success() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(categoryId, "Running Shoes", "Air cushioned");
        ProductDto dto = new ProductDto(UUID.randomUUID(), categoryId, "Running Shoes", "Air cushioned", "ACTIVE");

        when(catalogService.createProduct(any(CreateProductRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Running Shoes"));
    }

    @Test
    @DisplayName("API-CAT-004: PUT /api/v1/backoffice/products/{id} returns 200 OK")
    void updateProduct_Success() throws Exception {
        UUID prodId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(null, "Updated Name", "Updated Desc", "ACTIVE");
        ProductDto dto = new ProductDto(prodId, UUID.randomUUID(), "Updated Name", "Updated Desc", "ACTIVE");

        when(catalogService.updateProduct(eq(prodId), any(UpdateProductRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/backoffice/products/{id}", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @DisplayName("API-CAT-004: DELETE /api/v1/backoffice/products/{id} returns 204 No Content (soft delete)")
    void discontinueProduct_Success() throws Exception {
        UUID prodId = UUID.randomUUID();
        doNothing().when(catalogService).discontinueProduct(prodId);

        mockMvc.perform(delete("/api/v1/backoffice/products/{id}", prodId))
                .andExpect(status().isNoContent());

        verify(catalogService).discontinueProduct(prodId);
    }

    // ==========================================
    // SKU / Variant Tests (API-CAT-006)
    // ==========================================

    @Test
    @DisplayName("API-CAT-006: POST /api/v1/backoffice/products/{id}/skus returns 201 Created")
    void createSku_Success() throws Exception {
        UUID prodId = UUID.randomUUID();
        CreateSkuRequest request = new CreateSkuRequest(prodId, "SKU-RED-M");
        SkuDto dto = new SkuDto(UUID.randomUUID(), prodId, "SKU-RED-M", "ACTIVE");

        when(catalogService.createSku(eq(prodId), any(CreateSkuRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/products/{id}/skus", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skuCode").value("SKU-RED-M"));
    }

    @Test
    @DisplayName("API-CAT-006: POST /api/v1/backoffice/products/{id}/skus duplicate attribute returns 422")
    void createSku_DuplicateAttribute_Returns422() throws Exception {
        UUID prodId = UUID.randomUUID();
        CreateSkuRequest request = new CreateSkuRequest(prodId, "SKU-DUP");

        when(catalogService.createSku(eq(prodId), any(CreateSkuRequest.class)))
                .thenThrow(new BusinessRuleException("BR-014", "Duplicate attribute combination for product"));

        mockMvc.perform(post("/api/v1/backoffice/products/{id}/skus", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("API-CAT-006: PUT /api/v1/backoffice/skus/{id} returns 200 OK")
    void updateSku_Success() throws Exception {
        UUID skuId = UUID.randomUUID();
        UpdateSkuRequest request = new UpdateSkuRequest("SKU-UPDATED", "ACTIVE", null);
        SkuDto dto = new SkuDto(skuId, UUID.randomUUID(), "SKU-UPDATED", "ACTIVE");

        when(catalogService.updateSku(eq(skuId), any(UpdateSkuRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/backoffice/skus/{id}", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("SKU-UPDATED"));
    }

    // ==========================================
    // Attribute Tests (API-CAT-007)
    // ==========================================

    @Test
    @DisplayName("API-CAT-007: POST /api/v1/backoffice/attributes returns 201 Created")
    void createAttribute_Success() throws Exception {
        CreateAttributeRequest request = new CreateAttributeRequest("Material");
        ProductAttributeDto dto = new ProductAttributeDto(UUID.randomUUID(), "Material", Collections.emptyList());

        when(catalogService.createAttribute(any(CreateAttributeRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Material"));
    }

    @Test
    @DisplayName("API-CAT-007: GET /api/v1/backoffice/attributes returns 200 OK")
    void getAllAttributes_Success() throws Exception {
        ProductAttributeDto dto = new ProductAttributeDto(UUID.randomUUID(), "Size", List.of());
        when(catalogService.getAllAttributes()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/backoffice/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Size"));
    }

    @Test
    @DisplayName("API-CAT-007: POST /api/v1/backoffice/attributes/{id}/values returns 201 Created")
    void addAttributeValue_Success() throws Exception {
        UUID attrId = UUID.randomUUID();
        CreateAttributeValueRequest request = new CreateAttributeValueRequest("Cotton");
        ProductAttributeValueDto dto = new ProductAttributeValueDto(UUID.randomUUID(), attrId, "Cotton");

        when(catalogService.addAttributeValue(eq(attrId), any(CreateAttributeValueRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/attributes/{id}/values", attrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("Cotton"));
    }

    // ==========================================
    // Media Gallery Tests (API-CAT-008)
    // ==========================================

    @Test
    @DisplayName("API-CAT-008: POST /api/v1/backoffice/products/{id}/media returns 201 Created")
    void addMedia_Success() throws Exception {
        UUID prodId = UUID.randomUUID();
        CreateMediaRequest request = new CreateMediaRequest(null, "https://example.com/img.jpg", "IMAGE", 0);
        ProductMediaDto dto = new ProductMediaDto(UUID.randomUUID(), prodId, null, "https://example.com/img.jpg", "IMAGE", 0);

        when(catalogService.addMedia(eq(prodId), any(CreateMediaRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/products/{id}/media", prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://example.com/img.jpg"));
    }

    @Test
    @DisplayName("API-CAT-008: DELETE /api/v1/backoffice/media/{id} returns 204 No Content")
    void deleteMedia_Success() throws Exception {
        UUID mediaId = UUID.randomUUID();
        doNothing().when(catalogService).deleteMedia(mediaId);

        mockMvc.perform(delete("/api/v1/backoffice/media/{id}", mediaId))
                .andExpect(status().isNoContent());

        verify(catalogService).deleteMedia(mediaId);
    }
}
