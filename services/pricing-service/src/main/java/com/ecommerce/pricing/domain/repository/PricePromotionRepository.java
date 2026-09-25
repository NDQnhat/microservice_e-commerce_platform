package com.ecommerce.pricing.domain.repository;

import com.ecommerce.pricing.domain.model.PricePromotion;
import com.ecommerce.pricing.domain.model.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PricePromotionRepository extends JpaRepository<PricePromotion, UUID> {

    @Query("SELECT pp FROM PricePromotion pp WHERE pp.skuId = :skuId AND pp.status = :status " +
           "AND :now >= pp.startAt AND :now <= pp.endAt ORDER BY pp.salePrice ASC")
    List<PricePromotion> findActivePromotions(@Param("skuId") UUID skuId,
                                             @Param("status") PromotionStatus status,
                                             @Param("now") Instant now);

    default Optional<PricePromotion> findActivePromotion(UUID skuId, PromotionStatus status, Instant now) {
        List<PricePromotion> list = findActivePromotions(skuId, status, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
