package com.ecommerce.pricing.domain.repository;

import com.ecommerce.pricing.domain.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceRepository extends JpaRepository<Price, UUID> {

    @Query("SELECT p FROM Price p WHERE p.skuId = :skuId AND p.effectiveTo IS NULL")
    Optional<Price> findCurrentPriceBySkuId(@Param("skuId") UUID skuId);
}
