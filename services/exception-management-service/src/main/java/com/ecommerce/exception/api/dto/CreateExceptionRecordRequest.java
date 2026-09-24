package com.ecommerce.exception.api.dto;

import com.ecommerce.exception.domain.model.ExceptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExceptionRecordRequest(
        @NotNull(message = "Exception type is required")
        ExceptionType exceptionType,

        @NotBlank(message = "Source service is required")
        @Size(max = 100, message = "Source service cannot exceed 100 characters")
        String sourceService,

        @NotBlank(message = "Reference ID is required")
        @Size(max = 100, message = "Reference ID cannot exceed 100 characters")
        String referenceId,

        @NotBlank(message = "Reference type is required")
        @Size(max = 100, message = "Reference type cannot exceed 100 characters")
        String referenceType,

        @NotBlank(message = "Error code is required")
        @Size(max = 100, message = "Error code cannot exceed 100 characters")
        String errorCode,

        @NotBlank(message = "Error message is required")
        String errorMessage,

        String payload
) {
}
