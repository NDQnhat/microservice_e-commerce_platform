package com.ecommerce.cart.domain;

import com.ecommerce.cart.domain.model.Cart;
import com.ecommerce.cart.domain.model.CartItem;
import com.ecommerce.cart.domain.model.CartStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartDomainTest {

    @Test
    @DisplayName("New cart is created with ACTIVE status and empty items")
    void newCartShouldBeActive() {
        UUID customerId = UUID.randomUUID();
        Cart cart = new Cart(customerId);

        assertThat(cart.getId()).isNotNull();
        assertThat(cart.getCustomerId()).isEqualTo(customerId);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Cart item can be added to cart with valid quantity")
    void cartItemCanBeAdded() {
        Cart cart = new Cart(UUID.randomUUID());
        UUID skuId = UUID.randomUUID();
        CartItem item = new CartItem(cart, skuId, 3);

        cart.getItems().add(item);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }
}
