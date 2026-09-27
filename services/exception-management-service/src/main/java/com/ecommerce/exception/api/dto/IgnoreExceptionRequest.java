package com.ecommerce.exception.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IgnoreExceptionRequest(
        @NotBlank(message = "Justification notes are required (BR-018)")
        @Size(max = 2000, message = "Justification notes cannot exceed 2000 characters")
        String notes
) {
}
