package com.ecommerce.inventory.domain.repository;

import com.ecommerce.inventory.domain.model.InventoryReservation;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findByOrderId(UUID orderId);
    List<InventoryReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
    Optional<InventoryReservation> findByOrderIdAndSkuId(UUID orderId, UUID skuId);
    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
    boolean existsByOrderId(UUID orderId);
    boolean existsByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
