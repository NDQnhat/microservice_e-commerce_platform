package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {
    List<ProductAttributeValue> findByAttributeId(UUID attributeId);
    boolean existsByAttributeIdAndValue(UUID attributeId, String value);
    Optional<ProductAttributeValue> findByAttributeIdAndValue(UUID attributeId, String value);
    List<ProductAttributeValue> findByIdIn(Collection<UUID> ids);
}
