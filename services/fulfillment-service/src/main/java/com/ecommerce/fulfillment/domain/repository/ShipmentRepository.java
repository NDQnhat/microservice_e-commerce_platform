package com.ecommerce.fulfillment.domain.repository;

import com.ecommerce.fulfillment.domain.model.Shipment;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(UUID orderId);
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}
