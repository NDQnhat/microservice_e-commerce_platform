package com.ecommerce.exception.api.dto;

import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import java.time.Instant;
import java.util.UUID;

public record ExceptionRecordDto(
        UUID id,
        ExceptionType exceptionType,
        String sourceService,
        String referenceId,
        String referenceType,
        String errorCode,
        String errorMessage,
        String payload,
        ExceptionRecordStatus status,
        String assignedTo,
        String resolvedBy,
        Instant resolvedAt,
        String resolutionAction,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
