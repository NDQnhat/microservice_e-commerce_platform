package com.ecommerce.exception.service;

import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.api.dto.ResolveExceptionRequest;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ExceptionService {

    Page<ExceptionRecordDto> listExceptions(
            ExceptionRecordStatus status,
            ExceptionType exceptionType,
            String referenceType,
            Pageable pageable
    );

    ExceptionRecordDto getExceptionById(UUID id);

    ExceptionRecordDto recordException(CreateExceptionRecordRequest request);

    ExceptionRecordDto assignException(UUID id, String operatorId);

    ExceptionRecordDto resolveException(UUID id, ResolveExceptionRequest request, String operatorId);

    ExceptionRecordDto ignoreException(UUID id, String notes, String operatorId);

    DashboardSummaryDto getDashboardSummary();
}
