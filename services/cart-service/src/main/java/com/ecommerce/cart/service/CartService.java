package com.ecommerce.cart.service;

import com.ecommerce.cart.api.dto.AddToCartRequest;
import com.ecommerce.cart.api.dto.CartDto;
import com.ecommerce.cart.api.dto.CartItemDto;
import com.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.ecommerce.cart.domain.model.Cart;
import com.ecommerce.cart.domain.model.CartItem;
import com.ecommerce.cart.domain.model.CartStatus;
import com.ecommerce.cart.domain.repository.CartItemRepository;
import com.ecommerce.cart.domain.repository.CartRepository;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockValidator stockValidator;
    private final CartRedisCacheService cartRedisCacheService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       StockValidator stockValidator,
                       CartRedisCacheService cartRedisCacheService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.stockValidator = stockValidator;
        this.cartRedisCacheService = cartRedisCacheService;
    }

    // ==========================================
    // View Cart (API-CRT-001 / FR-008)
    // ==========================================

    @Transactional(readOnly = true)
    public CartDto getCart(UUID customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        Optional<CartDto> cached = cartRedisCacheService.getCachedCart(customerId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Cart cart = cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(new Cart(customerId)));

        CartDto dto = toCartDto(cart);
        cartRedisCacheService.cacheCart(dto);
        return dto;
    }

    // ==========================================
    // Add Item to Cart (API-CRT-002 / FR-006 / BR-004)
    // ==========================================

    public CartDto addItem(UUID customerId, AddToCartRequest request) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (request.getQuantity() <= 0) {
            throw new BusinessRuleException("BR-004", "Quantity must be greater than zero");
        }

        Cart cart = getOrCreateActiveCart(customerId);
        Optional<CartItem> existingItem = cart.findItemBySkuId(request.getSkuId());

        int targetQuantity = existingItem
                .map(i -> i.getQuantity() + request.getQuantity())
                .orElse(request.getQuantity());

        // Enforce BR-004: requested quantity must not exceed available stock
        stockValidator.validateStock(request.getSkuId(), targetQuantity);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(targetQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(cart, request.getSkuId(), request.getQuantity());
            cartItemRepository.save(newItem);
            cart.addItem(newItem);
        }

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);

        cartRedisCacheService.evictCart(customerId);
        CartDto dto = toCartDto(saved);
        cartRedisCacheService.cacheCart(dto);
        return dto;
    }

    // ==========================================
    // Update Item Quantity (API-CRT-003 / FR-007 / BR-004)
    // ==========================================

    public CartDto updateItem(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        Cart cart = getOrCreateActiveCart(customerId);
        CartItem item = cart.findItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));

        if (request.getQuantity() == 0) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        } else {
            // Enforce BR-004 on quantity change
            stockValidator.validateStock(item.getSkuId(), request.getQuantity());
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);

        cartRedisCacheService.evictCart(customerId);
        CartDto dto = toCartDto(saved);
        cartRedisCacheService.cacheCart(dto);
        return dto;
    }

    // ==========================================
    // Remove Item (API-CRT-004 / FR-007)
    // ==========================================

    public CartDto removeItem(UUID customerId, UUID itemId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        Cart cart = getOrCreateActiveCart(customerId);
        CartItem item = cart.findItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);

        cartRedisCacheService.evictCart(customerId);
        CartDto dto = toCartDto(saved);
        cartRedisCacheService.cacheCart(dto);
        return dto;
    }

    // ==========================================
    // Clear Cart (API-CRT-005 / FR-008)
    // ==========================================

    public CartDto clearCart(UUID customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        Cart cart = getOrCreateActiveCart(customerId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.clearItems();

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);

        cartRedisCacheService.evictCart(customerId);
        CartDto dto = toCartDto(saved);
        cartRedisCacheService.cacheCart(dto);
        return dto;
    }

    // ==========================================
    // Helpers
    // ==========================================

    public Cart getOrCreateActiveCart(UUID customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(new Cart(customerId)));
    }

    public CartDto toCartDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(i -> new CartItemDto(i.getId(), i.getSkuId(), i.getQuantity()))
                .collect(Collectors.toList());

        return new CartDto(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus().name(),
                items
        );
    }
}
