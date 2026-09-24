package com.ecommerce.catalog.domain;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryStatus;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogDomainTest {

    @Test
    @DisplayName("Category defaults to ACTIVE status upon creation")
    void categoryShouldDefaultToActive() {
        Category category = new Category(null, "Electronics", "electronics");
        assertThat(category.getId()).isNotNull();
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(category.getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Product defaults to ACTIVE and can be discontinued")
    void productShouldDefaultToActiveAndDiscontinue() {
        UUID categoryId = UUID.randomUUID();
        Product product = new Product(categoryId, "Smartphone", "High performance phone");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        product.setStatus(ProductStatus.DISCONTINUED);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
    }
}
