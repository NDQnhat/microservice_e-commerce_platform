package com.ecommerce.exception.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.api.dto.ResolveExceptionRequest;
import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.domain.model.OutboxEventRecord;
import com.ecommerce.exception.domain.repository.ExceptionRecordRepository;
import com.ecommerce.exception.domain.repository.OutboxEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Exception Management", description = "Operational exception tracking, resolution, and dashboard metrics (API-EXC-001, API-DASH-001)")
public class ExceptionBoardController {

    private final ExceptionRecordRepository exceptionRecordRepository;
    private final OutboxEventRepository outboxEventRepository;

    public ExceptionBoardController(ExceptionRecordRepository exceptionRecordRepository,
                                    OutboxEventRepository outboxEventRepository) {
        this.exceptionRecordRepository = exceptionRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @GetMapping
    @Operation(summary = "List exception records with optional status filtering")
    public ResponseEntity<Page<ExceptionRecordDto>> listExceptions(
            @RequestParam(required = false) ExceptionRecordStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ExceptionRecord> records = (status != null)
                ? exceptionRecordRepository.findByStatus(status, pageable)
                : exceptionRecordRepository.findAll(pageable);

        return ResponseEntity.ok(records.map(this::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exception record details by ID")
    public ResponseEntity<ExceptionRecordDto> getExceptionById(@PathVariable UUID id) {
        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));
        return ResponseEntity.ok(toDto(record));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Record a new system or business exception")
    public ResponseEntity<ExceptionRecordDto> recordException(@Valid @RequestBody CreateExceptionRecordRequest request) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PostMapping("/{id}/assign")
    @Transactional
    @Operation(summary = "Assign exception record to operator for investigation")
    public ResponseEntity<ExceptionRecordDto> assignException(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String operatorId) {

        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));
        record.assign(operatorId);
        ExceptionRecord updated = exceptionRecordRepository.save(record);
        return ResponseEntity.ok(toDto(updated));
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    @Operation(summary = "Resolve exception record with operator audit and resolution notes (BR-017, BR-018)")
    public ResponseEntity<ExceptionRecordDto> resolveException(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveExceptionRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String operatorId) {

        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));

        record.resolve(operatorId, request.action(), request.notes());
        ExceptionRecord updated = exceptionRecordRepository.save(record);

        // Outbox event record for resolved exception
        String payload = String.format("{\"exceptionId\":\"%s\",\"referenceId\":\"%s\",\"referenceType\":\"%s\",\"resolvedBy\":\"%s\",\"action\":\"%s\"}",
                updated.getId(), updated.getReferenceId(), updated.getReferenceType(), operatorId, request.action());
        OutboxEventRecord outbox = OutboxEventRecord.createPending("EXCEPTION", updated.getId().toString(), "EXCEPTION_RESOLVED", payload);
        outboxEventRepository.save(outbox);

        return ResponseEntity.ok(toDto(updated));
    }

    @PostMapping("/{id}/ignore")
    @Transactional
    @Operation(summary = "Mark exception record as ignored with justification")
    public ResponseEntity<ExceptionRecordDto> ignoreException(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String operatorId) {

        ExceptionRecord record = exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exception record not found: " + id));

        String notes = body.getOrDefault("notes", "Ignored by operator");
        record.ignore(operatorId, notes);
        ExceptionRecord updated = exceptionRecordRepository.save(record);
        return ResponseEntity.ok(toDto(updated));
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Operational dashboard summary metrics (API-DASH-001)")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        long openCount = exceptionRecordRepository.countByStatus(ExceptionRecordStatus.OPEN);
        long investigatingCount = exceptionRecordRepository.countByStatus(ExceptionRecordStatus.INVESTIGATING);
        long resolvedCount = exceptionRecordRepository.countByStatus(ExceptionRecordStatus.RESOLVED);

        List<Object[]> rawCounts = exceptionRecordRepository.countOpenByType();
        Map<String, Long> openByType = new HashMap<>();
        for (Object[] row : rawCounts) {
            ExceptionType type = (ExceptionType) row[0];
            Long count = (Long) row[1];
            openByType.put(type.name(), count);
        }

        DashboardSummaryDto summary = new DashboardSummaryDto(
                openCount,
                investigatingCount,
                resolvedCount,
                openByType
        );
        return ResponseEntity.ok(summary);
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
