package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status, Pageable pageable);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
