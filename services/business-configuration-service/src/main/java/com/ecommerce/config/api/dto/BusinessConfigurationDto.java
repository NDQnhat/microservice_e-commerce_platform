package com.ecommerce.config.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BusinessConfigurationDto(
        UUID id,
        String configKey,
        String configValue,
        String description,
        Integer version,
        Boolean isActive,
        Instant effectiveFrom,
        Instant effectiveTo,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
