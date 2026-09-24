package com.ecommerce.catalog.api.controller;

import com.ecommerce.catalog.api.dto.CategoryDto;
import com.ecommerce.catalog.domain.model.CategoryStatus;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories() {
        List<CategoryDto> categories = categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(c -> new CategoryDto(
                        c.getId(),
                        c.getParentCategoryId(),
                        c.getName(),
                        c.getSlug(),
                        c.getStatus().name()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }
}
