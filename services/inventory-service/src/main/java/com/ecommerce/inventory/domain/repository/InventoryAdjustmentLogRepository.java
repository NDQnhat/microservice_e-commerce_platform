package com.ecommerce.inventory.domain.repository;

import com.ecommerce.inventory.domain.model.InventoryAdjustmentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryAdjustmentLogRepository extends JpaRepository<InventoryAdjustmentLog, UUID> {
    Page<InventoryAdjustmentLog> findBySkuIdOrderByCreatedAtDesc(UUID skuId, Pageable pageable);
}
