package com.ecommerce.cart.domain.repository;

import com.ecommerce.cart.domain.model.Cart;
import com.ecommerce.cart.domain.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, CartStatus status);
}
