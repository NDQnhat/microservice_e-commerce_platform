package com.ecommerce.config.domain.repository;

import com.ecommerce.config.domain.model.BusinessConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessConfigurationRepository extends JpaRepository<BusinessConfiguration, UUID> {

    @Query("SELECT c FROM BusinessConfiguration c WHERE c.configKey = :configKey AND c.isActive = true")
    Optional<BusinessConfiguration> findActiveByKey(@Param("configKey") String configKey);

    List<BusinessConfiguration> findByConfigKeyOrderByVersionDesc(String configKey);

    List<BusinessConfiguration> findByIsActiveTrue();
}
