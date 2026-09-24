package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    Optional<ProductAttribute> findByName(String name);
}
