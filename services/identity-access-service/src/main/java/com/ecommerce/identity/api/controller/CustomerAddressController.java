package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AddressRequest;
import com.ecommerce.identity.api.dto.AddressResponse;
import com.ecommerce.identity.domain.model.CustomerAddress;
import com.ecommerce.identity.domain.repository.CustomerAddressRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/addresses")
public class CustomerAddressController {

    private final CustomerAddressRepository addressRepository;
    private final UserAccountRepository userAccountRepository;

    public CustomerAddressController(CustomerAddressRepository addressRepository,
                                     UserAccountRepository userAccountRepository) {
        this.addressRepository = addressRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable UUID customerId) {
        List<AddressResponse> responses = addressRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AddressResponse> createAddress(@PathVariable UUID customerId,
                                                         @Valid @RequestBody AddressRequest request) {
        if (!userAccountRepository.existsById(customerId)) {
            throw new NotFoundException("Customer not found: " + customerId);
        }

        if (request.isDefault()) {
            addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        addressRepository.save(existingDefault);
                    });
        }

        CustomerAddress address = new CustomerAddress(
                customerId,
                request.getRecipientName(),
                request.getPhone(),
                request.getLine1(),
                request.getLine2(),
                request.getWard(),
                request.getDistrict(),
                request.getCity(),
                request.isDefault()
        );

        CustomerAddress saved = addressRepository.save(address);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    @PutMapping("/{addressId}")
    @Transactional
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable UUID customerId,
                                                         @PathVariable UUID addressId,
                                                         @Valid @RequestBody AddressRequest request) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        addressRepository.save(existingDefault);
                    });
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setDefault(request.isDefault());

        CustomerAddress saved = addressRepository.save(address);
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID customerId,
                                              @PathVariable UUID addressId) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));

        addressRepository.delete(address);
        return ResponseEntity.noContent().build();
    }

    private AddressResponse mapToResponse(CustomerAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getCustomerId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.isDefault()
        );
    }
}
