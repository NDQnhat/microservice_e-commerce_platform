package com.ecommerce.identity.service;

import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AddressRequest;
import com.ecommerce.identity.api.dto.AddressResponse;
import com.ecommerce.identity.domain.model.CustomerAddress;
import com.ecommerce.identity.domain.repository.CustomerAddressRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private CustomerAddressService addressService;

    private final UUID customerId = UUID.randomUUID();
    private final UUID otherCustomerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        addressService = new CustomerAddressService(addressRepository, userAccountRepository);
    }

    @Test
    @DisplayName("FR-022 Authorization: Cross-customer access rejected with 403 AUTHORIZATION_FAILED")
    void crossCustomerAccess_ThrowsAuthorizationFailed() {
        assertThatThrownBy(() -> addressService.getAddresses(customerId, otherCustomerId))
                .isInstanceOf(AuthorizationFailedException.class)
                .hasMessageContaining("cannot access another customer's addresses");
    }

    @Test
    @DisplayName("FR-022 Happy Path: Create address with is_default=true unsets previous default (BR-017)")
    void createAddress_UnsetsPreviousDefault() {
        AddressRequest request = new AddressRequest(
                "John Doe", "0901234567", "123 Main St", "Apt 4B", "Ward 1", "District 1", "HCMC", true
        );

        when(userAccountRepository.existsById(customerId)).thenReturn(true);

        CustomerAddress existingDefault = new CustomerAddress(
                customerId, "Jane Doe", "0909999999", "456 Old St", null, "Ward 2", "District 2", "HCMC", true
        );
        when(addressRepository.findByCustomerIdAndIsDefaultTrue(customerId))
                .thenReturn(Optional.of(existingDefault));

        CustomerAddress savedAddress = new CustomerAddress(
                customerId, request.getRecipientName(), request.getPhone(),
                request.getLine1(), request.getLine2(), request.getWard(),
                request.getDistrict(), request.getCity(), true
        );
        when(addressRepository.save(any(CustomerAddress.class))).thenReturn(savedAddress);

        AddressResponse response = addressService.createAddress(customerId, customerId, request);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository, times(2)).save(any(CustomerAddress.class));
    }

    @Test
    @DisplayName("FR-022 Happy Path: Update address fields successfully")
    void updateAddress_HappyPath() {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest(
                "Updated Name", "0908888888", "999 New St", null, "Ward 5", "District 3", "Danang", false
        );

        CustomerAddress existing = new CustomerAddress(
                customerId, "Old Name", "0901111111", "123 Old St", null, "Ward 1", "District 1", "HCMC", false
        );
        when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.of(existing));
        when(addressRepository.save(existing)).thenReturn(existing);

        AddressResponse response = addressService.updateAddress(customerId, addressId, customerId, request);

        assertThat(response.getRecipientName()).isEqualTo("Updated Name");
        assertThat(response.getCity()).isEqualTo("Danang");
    }

    @Test
    @DisplayName("FR-022 Not Found: Update non-existent address throws NotFoundException (404)")
    void updateAddress_NotFound() {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest(
                "Name", "090", "Line", null, "W", "D", "C", false
        );
        when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(customerId, addressId, customerId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    @DisplayName("FR-022 Happy Path: Delete address removes record")
    void deleteAddress_HappyPath() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress existing = new CustomerAddress(
                customerId, "Name", "090", "Line", null, "W", "D", "C", false
        );
        when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.of(existing));

        addressService.deleteAddress(customerId, addressId, customerId);

        verify(addressRepository).delete(existing);
    }
}
