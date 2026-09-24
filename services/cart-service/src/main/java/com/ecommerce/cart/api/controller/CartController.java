package com.ecommerce.cart.api.controller;

import com.ecommerce.cart.api.dto.*;
import com.ecommerce.cart.domain.model.Cart;
import com.ecommerce.cart.domain.model.CartItem;
import com.ecommerce.cart.domain.model.CartStatus;
import com.ecommerce.cart.domain.repository.CartItemRepository;
import com.ecommerce.cart.domain.repository.CartRepository;
import com.ecommerce.common.error.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartController(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(@RequestParam UUID customerId) {
        Cart cart = getOrCreateActiveCart(customerId);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @PostMapping("/items")
    @Transactional
    public ResponseEntity<CartDto> addItem(@RequestParam UUID customerId,
                                           @Valid @RequestBody AddToCartRequest request) {
        Cart cart = getOrCreateActiveCart(customerId);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndSkuId(cart.getId(), request.getSkuId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(cart, request.getSkuId(), request.getQuantity());
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(cart));
    }

    @PutMapping("/items/{itemId}")
    @Transactional
    public ResponseEntity<CartDto> updateItem(@RequestParam UUID customerId,
                                              @PathVariable UUID itemId,
                                              @Valid @RequestBody UpdateCartItemRequest request) {
        Cart cart = getOrCreateActiveCart(customerId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));

        if (request.getQuantity() == 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return ResponseEntity.ok(mapToDto(cart));
    }

    @DeleteMapping("/items/{itemId}")
    @Transactional
    public ResponseEntity<CartDto> removeItem(@RequestParam UUID customerId,
                                              @PathVariable UUID itemId) {
        Cart cart = getOrCreateActiveCart(customerId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return ResponseEntity.ok(mapToDto(cart));
    }

    private Cart getOrCreateActiveCart(UUID customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(new Cart(customerId)));
    }

    private CartDto mapToDto(Cart cart) {
        return new CartDto(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus().name(),
                cart.getItems().stream()
                        .map(i -> new CartItemDto(i.getId(), i.getSkuId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );
    }
}
