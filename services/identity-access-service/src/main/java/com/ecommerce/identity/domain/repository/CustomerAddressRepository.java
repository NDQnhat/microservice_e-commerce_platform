package com.ecommerce.identity.domain.repository;

import com.ecommerce.identity.domain.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerId(UUID customerId);
    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(UUID customerId);
}
