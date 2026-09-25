package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.ProductDetailDto;
import com.ecommerce.catalog.api.dto.ProductDto;
import com.ecommerce.catalog.api.dto.SkuDetailDto;
import com.ecommerce.catalog.api.dto.SkuDto;
import com.ecommerce.catalog.service.CatalogService;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API-CAT-002: GET /api/v1/products returns 200 with page of products")
    void searchProducts_Returns200() throws Exception {
        UUID prodId = UUID.randomUUID();
        ProductDto p = new ProductDto(prodId, UUID.randomUUID(), "Smartphone", "Phone", "ACTIVE");
        when(catalogService.searchProducts(eq("phone"), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/products")
                        .param("query", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Smartphone"));
    }

    @Test
    @DisplayName("API-CAT-003: GET /api/v1/products/{id} returns 200 with product detail")
    void getProductDetail_Returns200() throws Exception {
        UUID prodId = UUID.randomUUID();
        SkuDetailDto sku = new SkuDetailDto(UUID.randomUUID(), prodId, "SKU-001", "ACTIVE", Collections.emptySet());
        ProductDetailDto detail = new ProductDetailDto(
                prodId, UUID.randomUUID(), "Laptop", "High performance", "ACTIVE",
                Instant.now(), Instant.now(), List.of(sku), Collections.emptyList()
        );

        when(catalogService.getProductDetail(prodId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/products/{id}", prodId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prodId.toString()))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.skus[0].skuCode").value("SKU-001"));
    }

    @Test
    @DisplayName("API-CAT-003: GET /api/v1/products/{id} returns 404 when product not found")
    void getProductDetail_NotFound_Returns404() throws Exception {
        UUID prodId = UUID.randomUUID();
        when(catalogService.getProductDetail(prodId))
                .thenThrow(new NotFoundException("Product not found: " + prodId));

        mockMvc.perform(get("/api/v1/products/{id}", prodId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("API-CAT-003: GET /api/v1/products/{id}/skus returns 200 with SKU list")
    void getProductSkus_Returns200() throws Exception {
        UUID prodId = UUID.randomUUID();
        SkuDto sku = new SkuDto(UUID.randomUUID(), prodId, "SKU-ABC", "ACTIVE");
        when(catalogService.getProductSkus(prodId)).thenReturn(List.of(sku));

        mockMvc.perform(get("/api/v1/products/{id}/skus", prodId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuCode").value("SKU-ABC"));
    }
}
