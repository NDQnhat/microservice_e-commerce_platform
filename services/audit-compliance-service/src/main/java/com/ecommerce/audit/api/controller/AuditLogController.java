package com.ecommerce.audit.api.controller;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.domain.model.AuditLog;
import com.ecommerce.audit.domain.repository.AuditLogRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/audit-log")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> queryAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {

        Page<AuditLog> page;
        if (actorId != null) {
            page = auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);
        } else if (actionType != null) {
            page = auditLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType, pageable);
        } else if (from != null && to != null) {
            page = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable);
        } else {
            page = auditLogRepository.findAll(pageable);
        }

        Page<AuditLogDto> dtos = page.map(a -> new AuditLogDto(
                a.getId(),
                a.getActorId(),
                a.getActorRole(),
                a.getActionType().name(),
                a.getEntityType(),
                a.getEntityId(),
                a.getBeforeValue(),
                a.getAfterValue(),
                a.getReason(),
                a.getCreatedAt()
        ));

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AuditLogDto> recordAuditLog(@Valid @RequestBody RecordAuditRequest request) {
        AuditLog log = new AuditLog(
                request.getActorId(),
                request.getActorRole(),
                request.getActionType(),
                request.getEntityType(),
                request.getEntityId(),
                request.getBeforeValue(),
                request.getAfterValue(),
                request.getReason()
        );
        AuditLog saved = auditLogRepository.save(log);

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuditLogDto(
                saved.getId(),
                saved.getActorId(),
                saved.getActorRole(),
                saved.getActionType().name(),
                saved.getEntityType(),
                saved.getEntityId(),
                saved.getBeforeValue(),
                saved.getAfterValue(),
                saved.getReason(),
                saved.getCreatedAt()
        ));
    }
}
