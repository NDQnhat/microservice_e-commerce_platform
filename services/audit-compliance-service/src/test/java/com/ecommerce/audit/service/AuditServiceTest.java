package com.ecommerce.audit.service;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.domain.model.AuditLog;
import com.ecommerce.audit.domain.repository.AuditLogRepository;
import com.ecommerce.audit.service.dto.AuditLogFilter;
import com.ecommerce.common.error.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    @DisplayName("AuditService: queryAuditLogs queries repository with specification and default sort")
    void shouldQueryAuditLogsWithDefaultSort() {
        UUID actorId = UUID.randomUUID();
        AuditLogFilter filter = AuditLogFilter.of(actorId, "SUPER_ADMIN", AuditActionType.CONFIG_CHANGE, null, null, null, null);
        Pageable unpaged = PageRequest.of(0, 10);

        AuditLog log = new AuditLog(actorId, "SUPER_ADMIN", AuditActionType.CONFIG_CHANGE, "CFG", "KEY", "1", "2", "reason");
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogDto> result = auditService.queryAuditLogs(filter, unpaged);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActorId()).isEqualTo(actorId);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    @DisplayName("AuditService: recordAuditLog records valid entry")
    void shouldRecordValidAuditLog() {
        UUID actorId = UUID.randomUUID();
        RecordAuditRequest request = new RecordAuditRequest(
                actorId,
                "SUPER_ADMIN",
                AuditActionType.ROLE_ASSIGN,
                "USER",
                UUID.randomUUID().toString(),
                null,
                "ROLE_ADMIN",
                "Assigned role"
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogDto dto = auditService.recordAuditLog(request);

        assertThat(dto).isNotNull();
        assertThat(dto.getActorId()).isEqualTo(actorId);
        assertThat(dto.getActionType()).isEqualTo("ROLE_ASSIGN");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("AuditService: recordAuditLog enforces BR-012: throws BusinessRuleException when PAYMENT_RECONCILE reason is null")
    void shouldThrowExceptionWhenPaymentReconcileReasonIsNull() {
        RecordAuditRequest request = new RecordAuditRequest(
                UUID.randomUUID(),
                "ORDER_OPERATIONS_ADMIN",
                AuditActionType.PAYMENT_RECONCILE,
                "PAYMENT_TRANSACTION",
                UUID.randomUUID().toString(),
                "TIMEOUT",
                "SUCCEEDED",
                null
        );

        assertThatThrownBy(() -> auditService.recordAuditLog(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Reason is mandatory for PAYMENT_RECONCILE audit action")
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-012"));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("AuditService: recordAuditLog enforces BR-012: throws BusinessRuleException when PAYMENT_RECONCILE reason is blank")
    void shouldThrowExceptionWhenPaymentReconcileReasonIsBlank() {
        RecordAuditRequest request = new RecordAuditRequest(
                UUID.randomUUID(),
                "ORDER_OPERATIONS_ADMIN",
                AuditActionType.PAYMENT_RECONCILE,
                "PAYMENT_TRANSACTION",
                UUID.randomUUID().toString(),
                "TIMEOUT",
                "SUCCEEDED",
                "   "
        );

        assertThatThrownBy(() -> auditService.recordAuditLog(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Reason is mandatory for PAYMENT_RECONCILE audit action")
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-012"));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("AuditService: recordAuditLog succeeds for PAYMENT_RECONCILE when reason is provided")
    void shouldRecordPaymentReconcileWithReason() {
        RecordAuditRequest request = new RecordAuditRequest(
                UUID.randomUUID(),
                "ORDER_OPERATIONS_ADMIN",
                AuditActionType.PAYMENT_RECONCILE,
                "PAYMENT_TRANSACTION",
                UUID.randomUUID().toString(),
                "TIMEOUT",
                "SUCCEEDED",
                "Bank statement ref: BNK-98765 confirmed receipt"
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogDto dto = auditService.recordAuditLog(request);

        assertThat(dto.getActionType()).isEqualTo("PAYMENT_RECONCILE");
        assertThat(dto.getReason()).isEqualTo("Bank statement ref: BNK-98765 confirmed receipt");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("AuditService: throws IllegalArgumentException when request is null")
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> auditService.recordAuditLog(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
