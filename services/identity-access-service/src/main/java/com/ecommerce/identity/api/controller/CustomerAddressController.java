package com.ecommerce.identity.api.controller;

import com.ecommerce.identity.api.dto.AddressRequest;
import com.ecommerce.identity.api.dto.AddressResponse;
import com.ecommerce.identity.security.SecurityUtils;
import com.ecommerce.identity.service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(CustomerAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable UUID customerId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<AddressResponse> responses = addressService.getAddresses(customerId, currentUserId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@PathVariable UUID customerId,
                                                         @Valid @RequestBody AddressRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        AddressResponse response = addressService.createAddress(customerId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable UUID customerId,
                                                         @PathVariable UUID addressId,
                                                         @Valid @RequestBody AddressRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        AddressResponse response = addressService.updateAddress(customerId, addressId, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID customerId,
                                              @PathVariable UUID addressId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        addressService.deleteAddress(customerId, addressId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
