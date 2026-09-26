package com.ecommerce.audit.api.controller;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.service.AuditService;
import com.ecommerce.audit.service.dto.AuditLogFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/backoffice/audit-log", "/api/v1/backoffice/audit-logs", "/api/v1/audit-logs"})
@Tag(name = "Audit Log", description = "Append-only immutable audit trail and compliance querying (API-AUDIT-001, FR-034, BR-015, NFR-AUDIT-002)")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * API-AUDIT-001: Query Audit Log (FR-034, BR-015).
     * Restricted strictly to Super Admin and Admin.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Query immutable audit log history with multi-attribute filtering (FR-034)")
    public ResponseEntity<Page<AuditLogDto>> queryAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {

        AuditLogFilter filter = AuditLogFilter.of(actorId, actorRole, actionType, entityType, entityId, from, to);
        Page<AuditLogDto> dtos = auditService.queryAuditLogs(filter, pageable);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Internal ingestion endpoint for sensitive actions (BR-015, Append-Only).
     */
    @PostMapping({"", "/record"})
    @Operation(summary = "Record new immutable audit log entry (Append-Only, BR-015)")
    public ResponseEntity<AuditLogDto> recordAuditLog(@Valid @RequestBody RecordAuditRequest request) {
        AuditLogDto recorded = auditService.recordAuditLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }
}
