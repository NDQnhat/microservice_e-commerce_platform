package com.ecommerce.exception.api.controller;

import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.api.dto.ResolveExceptionRequest;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.security.UserPrincipal;
import com.ecommerce.exception.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/backoffice/exceptions", "/api/v1/exceptions"})
@Tag(name = "Exception Management", description = "Operational exception tracking, triage, resolution, and dashboard metrics (API-EXC-001, API-DASH-001)")
public class ExceptionBoardController {

    private final ExceptionService exceptionService;

    public ExceptionBoardController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @GetMapping
    @Operation(summary = "List exception records with optional status, type, and reference filtering (API-EXC-001)")
    public ResponseEntity<Page<ExceptionRecordDto>> listExceptions(
            @RequestParam(required = false) ExceptionRecordStatus status,
            @RequestParam(required = false) ExceptionType exceptionType,
            @RequestParam(required = false) String referenceType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ExceptionRecordDto> records = exceptionService.listExceptions(status, exceptionType, referenceType, pageable);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exception record details by ID")
    public ResponseEntity<ExceptionRecordDto> getExceptionById(@PathVariable UUID id) {
        ExceptionRecordDto dto = exceptionService.getExceptionById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(summary = "Record a new system or business exception")
    public ResponseEntity<ExceptionRecordDto> recordException(@Valid @RequestBody CreateExceptionRecordRequest request) {
        ExceptionRecordDto created = exceptionService.recordException(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign exception record to operator for investigation")
    public ResponseEntity<ExceptionRecordDto> assignException(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String headerOperatorId,
            Authentication authentication) {

        String operatorId = resolveOperatorId(headerOperatorId, authentication);
        ExceptionRecordDto updated = exceptionService.assignException(id, operatorId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve exception record with operator audit and resolution notes (BR-017, BR-018)")
    public ResponseEntity<ExceptionRecordDto> resolveException(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveExceptionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerOperatorId,
            Authentication authentication) {

        String operatorId = resolveOperatorId(headerOperatorId, authentication);
        ExceptionRecordDto updated = exceptionService.resolveException(id, request, operatorId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/ignore")
    @Operation(summary = "Mark exception record as ignored with justification (BR-017, BR-018)")
    public ResponseEntity<ExceptionRecordDto> ignoreException(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) String headerOperatorId,
            Authentication authentication) {

        String operatorId = resolveOperatorId(headerOperatorId, authentication);
        String notes = extractNotes(body);
        ExceptionRecordDto updated = exceptionService.ignoreException(id, notes, operatorId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Operational dashboard summary metrics (API-DASH-001, FR-032)")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        DashboardSummaryDto summary = exceptionService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    private String resolveOperatorId(String headerOperatorId, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserPrincipal principal) {
                if (principal.getEmail() != null && !principal.getEmail().isBlank()) {
                    return principal.getEmail();
                }
                if (principal.getId() != null) {
                    return principal.getId().toString();
                }
            }
            if (authentication.getName() != null && !authentication.getName().isBlank() && !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        }
        if (headerOperatorId != null && !headerOperatorId.isBlank()) {
            return headerOperatorId;
        }
        return "SYSTEM";
    }

    private String extractNotes(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object notesObj = body.get("notes");
        if (notesObj != null) {
            return notesObj.toString();
        }
        Object justificationObj = body.get("justification");
        if (justificationObj != null) {
            return justificationObj.toString();
        }
        Object reasonObj = body.get("reason");
        if (reasonObj != null) {
            return reasonObj.toString();
        }
        return null;
    }
}
