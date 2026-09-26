package com.ecommerce.audit.service;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.domain.model.AuditLog;
import com.ecommerce.audit.domain.repository.AuditLogRepository;
import com.ecommerce.audit.domain.repository.AuditLogSpecification;
import com.ecommerce.audit.service.dto.AuditLogFilter;
import com.ecommerce.common.error.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> queryAuditLogs(AuditLogFilter filter, Pageable pageable) {
        Pageable effectivePageable = ensureDefaultSort(pageable);

        Specification<AuditLog> spec = (filter != null)
                ? AuditLogSpecification.withFilters(
                        filter.actorId(),
                        filter.actorRole(),
                        filter.actionType(),
                        filter.entityType(),
                        filter.entityId(),
                        filter.from(),
                        filter.to()
                )
                : (root, query, cb) -> cb.conjunction();

        Page<AuditLog> logs = auditLogRepository.findAll(spec, effectivePageable);
        return logs.map(this::toDto);
    }

    @Override
    public AuditLogDto recordAuditLog(RecordAuditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RecordAuditRequest cannot be null");
        }

        // BR-012 Guardrail: Reason is mandatory for PAYMENT_RECONCILE audit action
        if (request.getActionType() == AuditActionType.PAYMENT_RECONCILE) {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new BusinessRuleException("BR-012", "Reason is mandatory for PAYMENT_RECONCILE audit action");
            }
        }

        AuditLog auditLog = new AuditLog(
                request.getActorId(),
                request.getActorRole(),
                request.getActionType(),
                request.getEntityType(),
                request.getEntityId(),
                request.getBeforeValue(),
                request.getAfterValue(),
                request.getReason()
        );

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log recorded: id={}, actionType={}, entityType={}, entityId={}, actorId={}",
                saved.getId(), saved.getActionType(), saved.getEntityType(), saved.getEntityId(), saved.getActorId());

        return toDto(saved);
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

    private AuditLogDto toDto(AuditLog entity) {
        return new AuditLogDto(
                entity.getId(),
                entity.getActorId(),
                entity.getActorRole(),
                entity.getActionType() != null ? entity.getActionType().name() : null,
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getBeforeValue(),
                entity.getAfterValue(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
