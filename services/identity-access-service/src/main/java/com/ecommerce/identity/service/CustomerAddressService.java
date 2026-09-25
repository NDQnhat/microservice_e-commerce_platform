package com.ecommerce.identity.service;

import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AddressRequest;
import com.ecommerce.identity.api.dto.AddressResponse;
import com.ecommerce.identity.domain.model.CustomerAddress;
import com.ecommerce.identity.domain.repository.CustomerAddressRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final UserAccountRepository userAccountRepository;

    public CustomerAddressService(CustomerAddressRepository addressRepository,
                                  UserAccountRepository userAccountRepository) {
        this.addressRepository = addressRepository;
        this.userAccountRepository = userAccountRepository;
    }

    private void verifyOwnership(UUID customerId, UUID authenticatedUserId) {
        if (authenticatedUserId == null || !authenticatedUserId.equals(customerId)) {
            throw new AuthorizationFailedException("Access denied: cannot access another customer's addresses");
        }
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID customerId, UUID authenticatedUserId) {
        verifyOwnership(customerId, authenticatedUserId);
        return addressRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(UUID customerId, UUID authenticatedUserId, AddressRequest request) {
        verifyOwnership(customerId, authenticatedUserId);

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
        return mapToResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(UUID customerId, UUID addressId, UUID authenticatedUserId, AddressRequest request) {
        verifyOwnership(customerId, authenticatedUserId);

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
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId, UUID authenticatedUserId) {
        verifyOwnership(customerId, authenticatedUserId);

        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));

        addressRepository.delete(address);
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
