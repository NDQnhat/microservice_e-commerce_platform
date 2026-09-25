package com.ecommerce.cart.domain.repository;

import com.ecommerce.cart.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndSkuId(UUID cartId, UUID skuId);
    java.util.List<CartItem> findByCartId(UUID cartId);
    void deleteByCartId(UUID cartId);
}
