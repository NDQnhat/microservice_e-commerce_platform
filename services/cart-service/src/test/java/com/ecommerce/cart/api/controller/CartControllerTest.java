package com.ecommerce.cart.api.controller;

import com.ecommerce.cart.api.dto.AddToCartRequest;
import com.ecommerce.cart.api.dto.CartDto;
import com.ecommerce.cart.api.dto.CartItemDto;
import com.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.ecommerce.cart.security.UserPrincipal;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private UUID customerId;
    private UUID otherCustomerId;
    private UUID cartId;
    private UUID itemId;
    private UUID skuId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        customerId = UUID.randomUUID();
        otherCustomerId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        skuId = UUID.randomUUID();

        // Default: Authenticated as customerId
        UserPrincipal principal = new UserPrincipal(customerId, "customer@example.com", List.of("CUSTOMER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // API-CRT-001: GET /api/v1/cart
    // ==========================================

    @Test
    @DisplayName("API-CRT-001 Happy Path: GET /api/v1/cart returns 200 with customer cart")
    void getCart_HappyPath() throws Exception {
        CartDto cartDto = new CartDto(cartId, customerId, "ACTIVE", List.of(
                new CartItemDto(itemId, skuId, 2)
        ));

        when(cartService.getCart(customerId)).thenReturn(cartDto);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.customer_id").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("API-CRT-001 Dual Route: GET /api/v1/customers/{customerId}/cart returns 200")
    void getCart_DualRoute_HappyPath() throws Exception {
        CartDto cartDto = new CartDto(cartId, customerId, "ACTIVE", Collections.emptyList());

        when(cartService.getCart(customerId)).thenReturn(cartDto);

        mockMvc.perform(get("/api/v1/customers/" + customerId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer_id").value(customerId.toString()));
    }

    // ==========================================
    // API-CRT-002: POST /api/v1/cart/items
    // ==========================================

    @Test
    @DisplayName("API-CRT-002 Happy Path: POST /api/v1/cart/items returns 201 Created")
    void addItem_HappyPath() throws Exception {
        AddToCartRequest request = new AddToCartRequest(skuId, 3);
        CartDto updatedCart = new CartDto(cartId, customerId, "ACTIVE", List.of(
                new CartItemDto(itemId, skuId, 3)
        ));

        when(cartService.addItem(eq(customerId), any(AddToCartRequest.class))).thenReturn(updatedCart);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    @DisplayName("API-CRT-002 Dual Route: POST /api/v1/customers/{customerId}/cart/items returns 201")
    void addItem_DualRoute_HappyPath() throws Exception {
        AddToCartRequest request = new AddToCartRequest(skuId, 1);
        CartDto updatedCart = new CartDto(cartId, customerId, "ACTIVE", List.of(
                new CartItemDto(itemId, skuId, 1)
        ));

        when(cartService.addItem(eq(customerId), any(AddToCartRequest.class))).thenReturn(updatedCart);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(1));
    }

    @Test
    @DisplayName("API-CRT-002 BR-004 Violation: POST /api/v1/cart/items returns 422 Business Rule Violation")
    void addItem_StockExceeded_Returns422() throws Exception {
        AddToCartRequest request = new AddToCartRequest(skuId, 999);

        when(cartService.addItem(eq(customerId), any(AddToCartRequest.class)))
                .thenThrow(new BusinessRuleException("BR-004", "[BR-004] Requested quantity exceeds available stock"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem-type:business-rule-violation"))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-004"));
    }

    @Test
    @DisplayName("API-CRT-002 Validation Error: POST /api/v1/cart/items with null sku_id returns 400")
    void addItem_ValidationError_Returns400() throws Exception {
        String invalidJson = "{\"quantity\": 1}";

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ==========================================
    // API-CRT-003: PUT /api/v1/cart/items/{itemId}
    // ==========================================

    @Test
    @DisplayName("API-CRT-003 Happy Path: PUT /api/v1/cart/items/{itemId} returns 200")
    void updateItem_HappyPath() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);
        CartDto updatedCart = new CartDto(cartId, customerId, "ACTIVE", List.of(
                new CartItemDto(itemId, skuId, 5)
        ));

        when(cartService.updateItem(eq(customerId), eq(itemId), any(UpdateCartItemRequest.class))).thenReturn(updatedCart);

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    @DisplayName("API-CRT-003 Not Found: PUT /api/v1/cart/items/{itemId} with unknown ID returns 404")
    void updateItem_NotFound_Returns404() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        when(cartService.updateItem(eq(customerId), eq(itemId), any(UpdateCartItemRequest.class)))
                .thenThrow(new NotFoundException("Cart item not found: " + itemId));

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ==========================================
    // API-CRT-004: DELETE /api/v1/cart/items/{itemId}
    // ==========================================

    @Test
    @DisplayName("API-CRT-004 Happy Path: DELETE /api/v1/cart/items/{itemId} returns 200")
    void removeItem_HappyPath() throws Exception {
        CartDto updatedCart = new CartDto(cartId, customerId, "ACTIVE", Collections.emptyList());

        when(cartService.removeItem(customerId, itemId)).thenReturn(updatedCart);

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // ==========================================
    // API-CRT-005: DELETE /api/v1/cart
    // ==========================================

    @Test
    @DisplayName("API-CRT-005 Happy Path: DELETE /api/v1/cart clears cart and returns 200")
    void clearCart_HappyPath() throws Exception {
        CartDto clearedCart = new CartDto(cartId, customerId, "ACTIVE", Collections.emptyList());

        when(cartService.clearCart(customerId)).thenReturn(clearedCart);

        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("API-CRT-005 Dual Route: DELETE /api/v1/customers/{customerId}/cart clears cart")
    void clearCart_DualRoute_HappyPath() throws Exception {
        CartDto clearedCart = new CartDto(cartId, customerId, "ACTIVE", Collections.emptyList());

        when(cartService.clearCart(customerId)).thenReturn(clearedCart);

        mockMvc.perform(delete("/api/v1/customers/" + customerId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // ==========================================
    // Security & Access Control
    // ==========================================

    @Test
    @DisplayName("Security: Accessing another customer's cart returns 403 Forbidden")
    void accessOtherCustomerCart_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + otherCustomerId + "/cart"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem-type:authorization-failed"))
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"));
    }

    @Test
    @DisplayName("Security: Admin can access any customer's cart")
    void adminCanAccessAnyCart() throws Exception {
        UserPrincipal adminPrincipal = new UserPrincipal(UUID.randomUUID(), "admin@example.com", List.of("ADMIN"));
        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        CartDto cartDto = new CartDto(cartId, otherCustomerId, "ACTIVE", Collections.emptyList());
        when(cartService.getCart(otherCustomerId)).thenReturn(cartDto);

        mockMvc.perform(get("/api/v1/customers/" + otherCustomerId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer_id").value(otherCustomerId.toString()));
    }

    @Test
    @DisplayName("Security: Unauthenticated request without customer ID returns 401 Unauthorized")
    void unauthenticatedWithoutId_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:authentication-failed"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }
}
