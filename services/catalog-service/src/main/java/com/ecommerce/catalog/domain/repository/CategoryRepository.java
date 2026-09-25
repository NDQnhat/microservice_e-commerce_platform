package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    boolean existsByNameAndIdNot(String name, UUID id);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    List<Category> findByStatus(CategoryStatus status);
    List<Category> findByParentCategoryId(UUID parentCategoryId);
    boolean existsByParentCategoryId(UUID parentCategoryId);
}
