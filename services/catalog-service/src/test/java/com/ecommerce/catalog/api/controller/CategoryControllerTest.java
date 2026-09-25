package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.CategoryDto;
import com.ecommerce.catalog.service.CatalogService;
import com.ecommerce.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API-CAT-001: GET /api/v1/categories returns 200 with category list")
    void getCategories_Returns200() throws Exception {
        CategoryDto c1 = new CategoryDto(UUID.randomUUID(), null, "Electronics", "electronics", "ACTIVE");
        CategoryDto c2 = new CategoryDto(UUID.randomUUID(), c1.getId(), "Laptops", "laptops", "ACTIVE");

        when(catalogService.getActiveCategories()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[1].name").value("Laptops"));
    }
}
