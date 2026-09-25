package com.ecommerce.cart.service;

import com.ecommerce.cart.api.dto.AddToCartRequest;
import com.ecommerce.cart.api.dto.CartDto;
import com.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.ecommerce.cart.domain.model.Cart;
import com.ecommerce.cart.domain.model.CartItem;
import com.ecommerce.cart.domain.model.CartStatus;
import com.ecommerce.cart.domain.repository.CartItemRepository;
import com.ecommerce.cart.domain.repository.CartRepository;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StockValidator stockValidator;

    @Mock
    private CartRedisCacheService cartRedisCacheService;

    @InjectMocks
    private CartService cartService;

    private UUID customerId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        cart = new Cart(customerId);
    }

    // ==========================================
    // View Cart (API-CRT-001 / FR-008)
    // ==========================================

    @Test
    @DisplayName("getCart returns cached cart if present in Redis")
    void getCart_CacheHit() {
        CartDto cachedDto = new CartDto(cart.getId(), customerId, "ACTIVE", Collections.emptyList());
        when(cartRedisCacheService.getCachedCart(customerId)).thenReturn(Optional.of(cachedDto));

        CartDto result = cartService.getCart(customerId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cart.getId());
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        verify(cartRepository, never()).findByCustomerIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("getCart queries DB on cache miss, caches result, and returns DTO")
    void getCart_CacheMiss_ExistingCart() {
        when(cartRedisCacheService.getCachedCart(customerId)).thenReturn(Optional.empty());
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartDto result = cartService.getCart(customerId);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        verify(cartRedisCacheService).cacheCart(any(CartDto.class));
    }

    @Test
    @DisplayName("getCart creates a new active cart if customer has no active cart")
    void getCart_CreatesNewCart_WhenNotFound() {
        when(cartRedisCacheService.getCachedCart(customerId)).thenReturn(Optional.empty());
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.getCart(customerId);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(cartRepository).save(any(Cart.class));
        verify(cartRedisCacheService).cacheCart(any(CartDto.class));
    }

    @Test
    @DisplayName("getCart throws IllegalArgumentException if customerId is null")
    void getCart_NullCustomerId_ThrowsException() {
        assertThatThrownBy(() -> cartService.getCart(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID cannot be null");
    }

    // ==========================================
    // Add Item (API-CRT-002 / FR-006 / BR-004)
    // ==========================================

    @Test
    @DisplayName("addItem adds new item to cart when SKU not present and validates stock")
    void addItem_NewItem_Success() {
        UUID skuId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(skuId, 2);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.addItem(customerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSkuId()).isEqualTo(skuId);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);

        verify(stockValidator).validateStock(skuId, 2);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRedisCacheService).evictCart(customerId);
        verify(cartRedisCacheService).cacheCart(any(CartDto.class));
    }

    @Test
    @DisplayName("addItem merges quantity when SKU already exists in cart (FR-006)")
    void addItem_ExistingItem_MergesQuantity() {
        UUID skuId = UUID.randomUUID();
        CartItem existingItem = new CartItem(cart, skuId, 3);
        cart.addItem(existingItem);

        AddToCartRequest request = new AddToCartRequest(skuId, 4);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.addItem(customerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(7); // 3 + 4 = 7

        verify(stockValidator).validateStock(skuId, 7);
        verify(cartItemRepository).save(existingItem);
        verify(cartRedisCacheService).evictCart(customerId);
    }

    @Test
    @DisplayName("addItem throws BusinessRuleException when requested quantity exceeds stock (BR-004)")
    void addItem_StockExceeded_ThrowsBusinessRuleException() {
        UUID skuId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(skuId, 100);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        doThrow(new BusinessRuleException("BR-004", "Requested quantity 100 exceeds available stock"))
                .when(stockValidator).validateStock(skuId, 100);

        assertThatThrownBy(() -> cartService.addItem(customerId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Requested quantity 100 exceeds available stock")
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addItem throws BusinessRuleException when quantity is zero or negative")
    void addItem_ZeroOrNegativeQuantity_ThrowsBusinessRuleException() {
        UUID skuId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(skuId, 0);

        assertThatThrownBy(() -> cartService.addItem(customerId, request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));
    }

    // ==========================================
    // Update Item (API-CRT-003 / FR-007 / BR-004)
    // ==========================================

    @Test
    @DisplayName("updateItem updates quantity successfully with stock check")
    void updateItem_Success() {
        UUID skuId = UUID.randomUUID();
        CartItem item = new CartItem(cart, skuId, 2);
        cart.addItem(item);

        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.updateItem(customerId, item.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);

        verify(stockValidator).validateStock(skuId, 5);
        verify(cartItemRepository).save(item);
        verify(cartRedisCacheService).evictCart(customerId);
    }

    @Test
    @DisplayName("updateItem removes item from cart when quantity is set to 0 (FR-007)")
    void updateItem_QuantityZero_RemovesItem() {
        UUID skuId = UUID.randomUUID();
        CartItem item = new CartItem(cart, skuId, 2);
        cart.addItem(item);

        UpdateCartItemRequest request = new UpdateCartItemRequest(0);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.updateItem(customerId, item.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();

        verify(stockValidator, never()).validateStock(any(), anyInt());
        verify(cartItemRepository).delete(item);
        verify(cartRedisCacheService).evictCart(customerId);
    }

    @Test
    @DisplayName("updateItem throws NotFoundException when item is not in cart")
    void updateItem_ItemNotFound_ThrowsNotFoundException() {
        UUID nonExistentItemId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(customerId, nonExistentItemId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    @DisplayName("updateItem throws BusinessRuleException when new quantity exceeds stock (BR-004)")
    void updateItem_StockExceeded_ThrowsBusinessRuleException() {
        UUID skuId = UUID.randomUUID();
        CartItem item = new CartItem(cart, skuId, 2);
        cart.addItem(item);

        UpdateCartItemRequest request = new UpdateCartItemRequest(50);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        doThrow(new BusinessRuleException("BR-004", "Requested quantity exceeds available stock"))
                .when(stockValidator).validateStock(skuId, 50);

        assertThatThrownBy(() -> cartService.updateItem(customerId, item.getId(), request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));
    }

    // ==========================================
    // Remove Item (API-CRT-004 / FR-007)
    // ==========================================

    @Test
    @DisplayName("removeItem removes item from cart and deletes from repository")
    void removeItem_Success() {
        UUID skuId = UUID.randomUUID();
        CartItem item = new CartItem(cart, skuId, 3);
        cart.addItem(item);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.removeItem(customerId, item.getId());

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        verify(cartItemRepository).delete(item);
        verify(cartRedisCacheService).evictCart(customerId);
    }

    @Test
    @DisplayName("removeItem throws NotFoundException when item does not exist")
    void removeItem_ItemNotFound_ThrowsNotFoundException() {
        UUID nonExistentItemId = UUID.randomUUID();

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(customerId, nonExistentItemId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    // ==========================================
    // Clear Cart (API-CRT-005 / FR-008)
    // ==========================================

    @Test
    @DisplayName("clearCart deletes all items for the cart and empties collection")
    void clearCart_Success() {
        CartItem item1 = new CartItem(cart, UUID.randomUUID(), 2);
        CartItem item2 = new CartItem(cart, UUID.randomUUID(), 5);
        cart.addItem(item1);
        cart.addItem(item2);

        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.clearCart(customerId);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        verify(cartItemRepository).deleteByCartId(cart.getId());
        verify(cartRedisCacheService).evictCart(customerId);
    }
}
