package com.ecommerce.audit.service;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.service.dto.AuditLogFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    /**
     * Query immutable audit logs with multi-attribute filtering (FR-034, API-AUDIT-001).
     *
     * @param filter   Dynamic search criteria
     * @param pageable Pagination and sorting
     * @return Paginated audit log records
     */
    Page<AuditLogDto> queryAuditLogs(AuditLogFilter filter, Pageable pageable);

    /**
     * Record a new immutable audit log entry (Append-Only, NFR-AUDIT-002, BR-015).
     * Enforces mandatory reason guardrail for sensitive actions (BR-012).
     *
     * @param request Ingestion request
     * @return Created audit log entry
     */
    AuditLogDto recordAuditLog(RecordAuditRequest request);
}
