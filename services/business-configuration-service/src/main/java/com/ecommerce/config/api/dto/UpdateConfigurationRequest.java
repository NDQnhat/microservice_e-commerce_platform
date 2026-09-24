package com.ecommerce.config.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConfigurationRequest(
        @NotBlank(message = "Config key must not be blank")
        @Size(max = 100, message = "Config key must not exceed 100 characters")
        String configKey,

        @NotBlank(message = "Config value must not be blank")
        String configValue,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Reason for update must be provided")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
