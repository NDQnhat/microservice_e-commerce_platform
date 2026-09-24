package com.ecommerce.config.domain;

import com.ecommerce.config.domain.model.BusinessConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BusinessConfigurationTest {

    @Test
    @DisplayName("Should create new configuration with active state and version 1")
    void testCreateNew() {
        BusinessConfiguration config = BusinessConfiguration.createNew(
                "ORDER_CANCELLATION_TIMEOUT_MINUTES",
                "15",
                "Cutoff window for order cancellation in minutes",
                "ADMIN_USER"
        );

        assertNotNull(config.getId());
        assertEquals("ORDER_CANCELLATION_TIMEOUT_MINUTES", config.getConfigKey());
        assertEquals("15", config.getConfigValue());
        assertTrue(config.getIsActive());
        assertEquals(1, config.getVersion());
        assertNotNull(config.getEffectiveFrom());
        assertNull(config.getEffectiveTo());
        assertEquals("ADMIN_USER", config.getCreatedBy());
    }

    @Test
    @DisplayName("Should deprecate configuration by setting inactive and effectiveTo")
    void testDeprecate() {
        BusinessConfiguration config = BusinessConfiguration.createNew(
                "ORDER_CANCELLATION_TIMEOUT_MINUTES",
                "15",
                "Cutoff window for order cancellation",
                "ADMIN_USER"
        );

        Instant expiration = Instant.now();
        config.deprecate(expiration);

        assertFalse(config.getIsActive());
        assertEquals(expiration, config.getEffectiveTo());
    }
}
