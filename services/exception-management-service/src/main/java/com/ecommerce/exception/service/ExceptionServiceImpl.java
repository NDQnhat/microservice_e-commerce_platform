package com.ecommerce.exception.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.api.dto.ResolveExceptionRequest;
import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.domain.repository.ExceptionRecordRepository;
import com.ecommerce.exception.domain.repository.ExceptionRecordSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ExceptionServiceImpl implements ExceptionService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionServiceImpl.class);

    private final ExceptionRecordRepository exceptionRecordRepository;
    private final ExceptionOutboxService exceptionOutboxService;

    public ExceptionServiceImpl(
            ExceptionRecordRepository exceptionRecordRepository,
            ExceptionOutboxService exceptionOutboxService) {
        this.exceptionRecordRepository = exceptionRecordRepository;
        this.exceptionOutboxService = exceptionOutboxService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExceptionRecordDto> listExceptions(
            ExceptionRecordStatus status,
            ExceptionType exceptionType,
            String referenceType,
            Pageable pageable) {

        Pageable effectivePageable = ensureDefaultSort(pageable);
        Specification<ExceptionRecord> spec = ExceptionRecordSpecification.withFilters(status, exceptionType, referenceType);
        Page<ExceptionRecord> records = exceptionRecordRepository.findAll(spec, effectivePageable);
        return records.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ExceptionRecordDto getExceptionById(UUID id) {
        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));
        return toDto(record);
    }

    @Override
    public ExceptionRecordDto recordException(CreateExceptionRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateExceptionRecordRequest cannot be null");
        }

        ExceptionRecord record = ExceptionRecord.create(
                request.exceptionType(),
                request.sourceService(),
                request.referenceId(),
                request.referenceType(),
                request.errorCode(),
                request.errorMessage(),
                request.payload()
        );

        ExceptionRecord saved = exceptionRecordRepository.save(record);
        log.info("Recorded new exception: id={}, type={}, sourceService={}, refType={}, refId={}",
                saved.getId(), saved.getExceptionType(), saved.getSourceService(), saved.getReferenceType(), saved.getReferenceId());
        return toDto(saved);
    }

    @Override
    public ExceptionRecordDto assignException(UUID id, String operatorId) {
        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));

        String effectiveOperator = (operatorId != null && !operatorId.isBlank()) ? operatorId : "SYSTEM";
        record.assign(effectiveOperator);
        ExceptionRecord updated = exceptionRecordRepository.save(record);
        log.info("Assigned exception {} to operator {}", id, effectiveOperator);
        return toDto(updated);
    }

    @Override
    public ExceptionRecordDto resolveException(UUID id, ResolveExceptionRequest request, String operatorId) {
        if (request == null) {
            throw new IllegalArgumentException("ResolveExceptionRequest cannot be null");
        }

        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));

        record.resolve(operatorId, request.action(), request.notes());
        ExceptionRecord updated = exceptionRecordRepository.save(record);

        // Transactional outbox event publishing (FR-038, BR-015)
        exceptionOutboxService.recordExceptionResolved(updated);
        log.info("Resolved exception {} by operator {}: action={}", id, operatorId, request.action());
        return toDto(updated);
    }

    @Override
    public ExceptionRecordDto ignoreException(UUID id, String notes, String operatorId) {
        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));

        record.ignore(operatorId, notes);
        ExceptionRecord updated = exceptionRecordRepository.save(record);

        // Transactional outbox event publishing (FR-038, BR-015)
        exceptionOutboxService.recordExceptionIgnored(updated);
        log.info("Ignored exception {} by operator {}: notes={}", id, operatorId, notes);
        return toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        long openCount = exceptionRecordRepository.countByStatus(ExceptionRecordStatus.OPEN);
        long investigatingCount = exceptionRecordRepository.countByStatus(ExceptionRecordStatus.INVESTIGATING);
        long resolvedCount = exceptionRecordRepository.countByStatus(ExceptionRecordStatus.RESOLVED);

        Map<String, Long> openByType = new HashMap<>();
        for (ExceptionType type : ExceptionType.values()) {
            openByType.put(type.name(), 0L);
        }

        List<Object[]> rawCounts = exceptionRecordRepository.countOpenByType();
        for (Object[] row : rawCounts) {
            if (row != null && row.length >= 2) {
                ExceptionType type = (ExceptionType) row[0];
                Long count = (Long) row[1];
                if (type != null && count != null) {
                    openByType.put(type.name(), count);
                }
            }
        }

        return new DashboardSummaryDto(
                openCount,
                investigatingCount,
                resolvedCount,
                openByType
        );
    }

    private Pageable ensureDefaultSort(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        return pageable;
    }

    private ExceptionRecordDto toDto(ExceptionRecord record) {
        return new ExceptionRecordDto(
                record.getId(),
                record.getExceptionType(),
                record.getSourceService(),
                record.getReferenceId(),
                record.getReferenceType(),
                record.getErrorCode(),
                record.getErrorMessage(),
                record.getPayload(),
                record.getStatus(),
                record.getAssignedTo(),
                record.getResolvedBy(),
                record.getResolvedAt(),
                record.getResolutionAction(),
                record.getResolutionNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
