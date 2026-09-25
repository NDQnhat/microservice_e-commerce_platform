package com.ecommerce.cart.domain;

import com.ecommerce.cart.domain.model.Cart;
import com.ecommerce.cart.domain.model.CartItem;
import com.ecommerce.cart.domain.model.CartStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartDomainTest {

    @Test
    @DisplayName("New cart is created with ACTIVE status, null updatedAt, and empty items")
    void newCartShouldBeActive() {
        UUID customerId = UUID.randomUUID();
        Cart cart = new Cart(customerId);

        assertThat(cart.getId()).isNotNull();
        assertThat(cart.getCustomerId()).isEqualTo(customerId);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("addItem sets parent cart and appends to items collection")
    void addItem_SetsRelationship() {
        Cart cart = new Cart(UUID.randomUUID());
        UUID skuId = UUID.randomUUID();
        CartItem item = new CartItem(cart, skuId, 3);

        cart.addItem(item);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0)).isEqualTo(item);
        assertThat(item.getCart()).isEqualTo(cart);
    }

    @Test
    @DisplayName("removeItem clears relationship and removes from items collection")
    void removeItem_ClearsRelationship() {
        Cart cart = new Cart(UUID.randomUUID());
        CartItem item = new CartItem(cart, UUID.randomUUID(), 2);
        cart.addItem(item);

        cart.removeItem(item);

        assertThat(cart.getItems()).isEmpty();
        assertThat(item.getCart()).isNull();
    }

    @Test
    @DisplayName("clearItems empties all items and removes references")
    void clearItems_ClearsAll() {
        Cart cart = new Cart(UUID.randomUUID());
        CartItem item1 = new CartItem(cart, UUID.randomUUID(), 1);
        CartItem item2 = new CartItem(cart, UUID.randomUUID(), 4);
        cart.addItem(item1);
        cart.addItem(item2);

        cart.clearItems();

        assertThat(cart.getItems()).isEmpty();
        assertThat(item1.getCart()).isNull();
        assertThat(item2.getCart()).isNull();
    }

    @Test
    @DisplayName("findItemBySkuId finds item by SKU ID")
    void findItemBySkuId_ReturnsItem() {
        Cart cart = new Cart(UUID.randomUUID());
        UUID skuId1 = UUID.randomUUID();
        UUID skuId2 = UUID.randomUUID();
        CartItem item1 = new CartItem(cart, skuId1, 2);
        cart.addItem(item1);

        Optional<CartItem> found = cart.findItemBySkuId(skuId1);
        Optional<CartItem> notFound = cart.findItemBySkuId(skuId2);

        assertThat(found).isPresent().contains(item1);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("findItemById finds item by its CartItem ID")
    void findItemById_ReturnsItem() {
        Cart cart = new Cart(UUID.randomUUID());
        CartItem item = new CartItem(cart, UUID.randomUUID(), 5);
        cart.addItem(item);

        Optional<CartItem> found = cart.findItemById(item.getId());
        Optional<CartItem> notFound = cart.findItemById(UUID.randomUUID());

        assertThat(found).isPresent().contains(item);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("markAsCheckedOut transitions status to CHECKED_OUT and updates updatedAt")
    void markAsCheckedOut_TransitionsStatus() {
        Cart cart = new Cart(UUID.randomUUID());

        cart.markAsCheckedOut();

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(cart.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("setUpdatedAt updates timestamp")
    void setUpdatedAt_UpdatesTimestamp() {
        Cart cart = new Cart(UUID.randomUUID());
        Instant now = Instant.now();

        cart.setUpdatedAt(now);

        assertThat(cart.getUpdatedAt()).isEqualTo(now);
    }
}
