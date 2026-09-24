package com.ecommerce.exception.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveExceptionRequest(
        @NotBlank(message = "Resolution action is required")
        @Size(max = 100, message = "Resolution action cannot exceed 100 characters")
        String action,

        @NotBlank(message = "Resolution notes are required (BR-018)")
        @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
        String notes
) {
}
