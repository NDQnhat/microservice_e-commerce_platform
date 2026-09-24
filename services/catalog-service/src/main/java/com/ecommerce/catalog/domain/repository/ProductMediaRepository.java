package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, UUID> {
    List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId);
    List<ProductMedia> findBySkuIdOrderBySortOrderAsc(UUID skuId);
}
