package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AddressRequest;
import com.ecommerce.identity.api.dto.AddressResponse;
import com.ecommerce.identity.security.UserPrincipal;
import com.ecommerce.identity.service.CustomerAddressService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CustomerAddressService addressService;

    @InjectMocks
    private CustomerAddressController addressController;

    private final UUID customerId = UUID.randomUUID();
    private final UUID addressId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserPrincipal principal = new UserPrincipal(customerId, "customer@example.com", List.of("CUSTOMER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-IAM-003 Happy Path: GET addresses returns 200 OK with address list")
    void getAddresses_HappyPath() throws Exception {
        AddressResponse address = new AddressResponse(
                addressId, customerId, "John Doe", "0901234567",
                "123 Main St", null, "Ward 1", "District 1", "HCMC", true
        );
        when(addressService.getAddresses(customerId, customerId)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/v1/customers/{customerId}/addresses", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipient_name").value("John Doe"))
                .andExpect(jsonPath("$[0].is_default").value(true));
    }

    @Test
    @DisplayName("API-IAM-003 Happy Path: POST address creates address and returns 201 Created")
    void createAddress_HappyPath() throws Exception {
        AddressRequest request = new AddressRequest(
                "John Doe", "0901234567", "123 Main St", null, "Ward 1", "District 1", "HCMC", true
        );
        AddressResponse response = new AddressResponse(
                addressId, customerId, "John Doe", "0901234567",
                "123 Main St", null, "Ward 1", "District 1", "HCMC", true
        );
        when(addressService.createAddress(eq(customerId), eq(customerId), any(AddressRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.recipient_name").value("John Doe"));
    }

    @Test
    @DisplayName("API-IAM-003 Validation: POST address with missing recipient_name returns 400 VALIDATION_ERROR")
    void createAddress_MissingRecipientName_Returns400() throws Exception {
        AddressRequest request = new AddressRequest(
                "", "0901234567", "123 Main St", null, "Ward 1", "District 1", "HCMC", false
        );

        mockMvc.perform(post("/api/v1/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("API-IAM-003 Authorization: Cross-customer access returns 403 AUTHORIZATION_FAILED")
    void getAddresses_CrossCustomer_Returns403() throws Exception {
        UUID otherCustomerId = UUID.randomUUID();
        when(addressService.getAddresses(otherCustomerId, customerId))
                .thenThrow(new AuthorizationFailedException("Access denied: cannot access another customer's addresses"));

        mockMvc.perform(get("/api/v1/customers/{customerId}/addresses", otherCustomerId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Access denied: cannot access another customer's addresses"));
    }

    @Test
    @DisplayName("API-IAM-003 Happy Path: PUT address updates and returns 200 OK")
    void updateAddress_HappyPath() throws Exception {
        AddressRequest request = new AddressRequest(
                "Jane Doe", "0909999999", "456 Updated St", null, "Ward 2", "District 2", "HCMC", false
        );
        AddressResponse response = new AddressResponse(
                addressId, customerId, "Jane Doe", "0909999999",
                "456 Updated St", null, "Ward 2", "District 2", "HCMC", false
        );
        when(addressService.updateAddress(eq(customerId), eq(addressId), eq(customerId), any(AddressRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/customers/{customerId}/addresses/{addressId}", customerId, addressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipient_name").value("Jane Doe"));
    }

    @Test
    @DisplayName("API-IAM-003 Happy Path: DELETE address returns 204 No Content")
    void deleteAddress_HappyPath() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/{customerId}/addresses/{addressId}", customerId, addressId))
                .andExpect(status().isNoContent());
    }
}
