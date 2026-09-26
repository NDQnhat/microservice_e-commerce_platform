package com.ecommerce.config.domain;

import com.ecommerce.config.domain.model.BusinessConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

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

    @Test
    @DisplayName("Should create next version with incremented version number and deprecate current (BR-019)")
    void testCreateNextVersion() {
        BusinessConfiguration v1 = BusinessConfiguration.createNew(
                "RESERVATION_TTL_MINUTES",
                "15",
                "Initial reservation TTL",
                "SUPER_ADMIN_1"
        );

        BusinessConfiguration v2 = v1.createNextVersion(
                "30",
                "Extended TTL for holiday rush",
                "SUPER_ADMIN_2"
        );

        // Verify v1 is deprecated
        assertFalse(v1.getIsActive());
        assertNotNull(v1.getEffectiveTo());
        assertEquals(1, v1.getVersion());
        assertEquals("15", v1.getConfigValue());

        // Verify v2 has new attributes and increments version
        assertNotNull(v2.getId());
        assertNotEquals(v1.getId(), v2.getId());
        assertEquals("RESERVATION_TTL_MINUTES", v2.getConfigKey());
        assertEquals("30", v2.getConfigValue());
        assertEquals("Extended TTL for holiday rush", v2.getDescription());
        assertEquals(2, v2.getVersion());
        assertTrue(v2.getIsActive());
        assertNotNull(v2.getEffectiveFrom());
        assertNull(v2.getEffectiveTo());
        assertEquals("SUPER_ADMIN_2", v2.getCreatedBy());
    }

    @Test
    @DisplayName("Should support equality and hash code based on identity UUID")
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        BusinessConfiguration cfg1 = new BusinessConfiguration(
                id, "KEY1", "VAL1", "DESC1", 1, true, now, null, "ADMIN", now, now
        );
        BusinessConfiguration cfg2 = new BusinessConfiguration(
                id, "KEY1", "VAL2", "DESC2", 2, false, now, now, "ADMIN2", now, now
        );
        BusinessConfiguration cfg3 = new BusinessConfiguration(
                UUID.randomUUID(), "KEY1", "VAL1", "DESC1", 1, true, now, null, "ADMIN", now, now
        );

        assertEquals(cfg1, cfg2);
        assertEquals(cfg1.hashCode(), cfg2.hashCode());
        assertNotEquals(cfg1, cfg3);
        assertNotEquals(cfg1, null);
        assertNotEquals(cfg1, new Object());
    }
}
