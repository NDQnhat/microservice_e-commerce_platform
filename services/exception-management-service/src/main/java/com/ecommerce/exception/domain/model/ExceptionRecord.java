package com.ecommerce.exception.domain.model;

import com.ecommerce.common.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "exception_records")
public class ExceptionRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 100)
    private ExceptionType exceptionType;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    @Column(name = "reference_type", nullable = false, length = 100)
    private String referenceType;

    @Column(name = "error_code", nullable = false, length = 100)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExceptionRecordStatus status;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_action", length = 100)
    private String resolutionAction;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ExceptionRecord() {
    }

    public ExceptionRecord(UUID id, ExceptionType exceptionType, String sourceService,
                           String referenceId, String referenceType, String errorCode,
                           String errorMessage, String payload, ExceptionRecordStatus status,
                           String assignedTo, String resolvedBy, Instant resolvedAt,
                           String resolutionAction, String resolutionNotes,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.exceptionType = exceptionType;
        this.sourceService = sourceService;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.payload = payload;
        this.status = status;
        this.assignedTo = assignedTo;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.resolutionAction = resolutionAction;
        this.resolutionNotes = resolutionNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ExceptionRecord create(ExceptionType exceptionType, String sourceService,
                                         String referenceId, String referenceType,
                                         String errorCode, String errorMessage, String payload) {
        Instant now = Instant.now();
        return new ExceptionRecord(
                UUID.randomUUID(),
                exceptionType,
                sourceService,
                referenceId,
                referenceType,
                errorCode,
                errorMessage,
                payload,
                ExceptionRecordStatus.OPEN,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public void assign(String operatorId) {
        this.status.validateTransitionTo(ExceptionRecordStatus.INVESTIGATING);
        this.status = ExceptionRecordStatus.INVESTIGATING;
        this.assignedTo = operatorId;
        this.updatedAt = Instant.now();
    }

    public void resolve(String operatorId, String action, String notes) {
        if (operatorId == null || operatorId.isBlank()) {
            throw new BusinessRuleException("BR-017", "Operator ID is required to resolve an exception record (BR-017)");
        }
        if (notes == null || notes.isBlank()) {
            throw new BusinessRuleException("BR-018", "Resolution notes are required when resolving an exception record (BR-018)");
        }
        this.status.validateTransitionTo(ExceptionRecordStatus.RESOLVED);
        this.status = ExceptionRecordStatus.RESOLVED;
        this.resolvedBy = operatorId;
        this.resolutionAction = action != null ? action : "MANUAL_INTERVENTION";
        this.resolutionNotes = notes;
        this.resolvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void ignore(String operatorId, String notes) {
        if (operatorId == null || operatorId.isBlank()) {
            throw new BusinessRuleException("BR-017", "Operator ID is required to ignore an exception record");
        }
        if (notes == null || notes.isBlank()) {
            throw new BusinessRuleException("BR-018", "Justification notes are required when ignoring an exception record");
        }
        this.status.validateTransitionTo(ExceptionRecordStatus.IGNORED);
        this.status = ExceptionRecordStatus.IGNORED;
        this.resolvedBy = operatorId;
        this.resolutionAction = "IGNORED";
        this.resolutionNotes = notes;
        this.resolvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ExceptionRecordStatus getStatus() {
        return status;
    }

    public void setStatus(ExceptionRecordStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public void setResolutionAction(String resolutionAction) {
        this.resolutionAction = resolutionAction;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExceptionRecord that = (ExceptionRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
