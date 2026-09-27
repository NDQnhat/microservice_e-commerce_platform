package com.ecommerce.exception.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.InvalidStateException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.api.dto.ResolveExceptionRequest;
import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.domain.repository.ExceptionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionServiceTest {

    @Mock
    private ExceptionRecordRepository exceptionRecordRepository;

    @Mock
    private ExceptionOutboxService exceptionOutboxService;

    @InjectMocks
    private ExceptionServiceImpl exceptionService;

    private ExceptionRecord sampleRecord;

    @BeforeEach
    void setUp() {
        sampleRecord = ExceptionRecord.create(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-12345",
                "ORDER",
                "PAY_ERR_INSUFFICIENT_FUNDS",
                "Payment declined due to insufficient funds",
                "{\"amount\":100}"
        );
    }

    @Test
    @DisplayName("listExceptions: queries repository with specification and default sort")
    void listExceptions_returnsPagedDtos() {
        Page<ExceptionRecord> pagedRecords = new PageImpl<>(List.of(sampleRecord));
        when(exceptionRecordRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagedRecords);

        Page<ExceptionRecordDto> result = exceptionService.listExceptions(
                ExceptionRecordStatus.OPEN,
                ExceptionType.PAYMENT_FAILED,
                "ORDER",
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).referenceId()).isEqualTo("ORD-12345");
        verify(exceptionRecordRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getExceptionById: returns DTO when found")
    void getExceptionById_found_returnsDto() {
        UUID id = sampleRecord.getId();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));

        ExceptionRecordDto result = exceptionService.getExceptionById(id);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.exceptionType()).isEqualTo(ExceptionType.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("getExceptionById: throws NotFoundException when missing")
    void getExceptionById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exceptionService.getExceptionById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Exception record not found");
    }

    @Test
    @DisplayName("recordException: creates and persists new exception record")
    void recordException_success() {
        CreateExceptionRecordRequest request = new CreateExceptionRecordRequest(
                ExceptionType.STUCK_ORDER,
                "order-service",
                "ORD-99999",
                "ORDER",
                "STUCK_IN_PAYING",
                "Order stuck in processing",
                "{}"
        );

        when(exceptionRecordRepository.save(any(ExceptionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExceptionRecordDto result = exceptionService.recordException(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ExceptionRecordStatus.OPEN);
        assertThat(result.exceptionType()).isEqualTo(ExceptionType.STUCK_ORDER);
        assertThat(result.referenceId()).isEqualTo("ORD-99999");
        verify(exceptionRecordRepository).save(any(ExceptionRecord.class));
    }

    @Test
    @DisplayName("assignException: transitions to INVESTIGATING with operatorId")
    void assignException_success() {
        UUID id = sampleRecord.getId();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));
        when(exceptionRecordRepository.save(any(ExceptionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExceptionRecordDto result = exceptionService.assignException(id, "OPERATOR_ALICE");

        assertThat(result.status()).isEqualTo(ExceptionRecordStatus.INVESTIGATING);
        assertThat(result.assignedTo()).isEqualTo("OPERATOR_ALICE");
        verify(exceptionRecordRepository).save(sampleRecord);
    }

    @Test
    @DisplayName("resolveException: transitions to RESOLVED, triggers outbox event (BR-017, BR-018)")
    void resolveException_success() {
        UUID id = sampleRecord.getId();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));
        when(exceptionRecordRepository.save(any(ExceptionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolveExceptionRequest request = new ResolveExceptionRequest("RETRY_MANUALLY", "Bank transaction verified");
        ExceptionRecordDto result = exceptionService.resolveException(id, request, "ADMIN_BOB");

        assertThat(result.status()).isEqualTo(ExceptionRecordStatus.RESOLVED);
        assertThat(result.resolvedBy()).isEqualTo("ADMIN_BOB");
        assertThat(result.resolutionAction()).isEqualTo("RETRY_MANUALLY");
        assertThat(result.resolutionNotes()).isEqualTo("Bank transaction verified");
        assertThat(result.resolvedAt()).isNotNull();

        verify(exceptionRecordRepository).save(sampleRecord);
        verify(exceptionOutboxService).recordExceptionResolved(sampleRecord);
    }

    @Test
    @DisplayName("resolveException: fails without operatorId (BR-017)")
    void resolveException_failsWithoutOperatorId() {
        UUID id = sampleRecord.getId();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));

        ResolveExceptionRequest request = new ResolveExceptionRequest("RETRY_MANUALLY", "Bank transaction verified");

        assertThatThrownBy(() -> exceptionService.resolveException(id, request, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Operator ID is required");

        verify(exceptionOutboxService, never()).recordExceptionResolved(any());
    }

    @Test
    @DisplayName("resolveException: fails without resolution notes (BR-018)")
    void resolveException_failsWithoutNotes() {
        UUID id = sampleRecord.getId();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));

        ResolveExceptionRequest request = new ResolveExceptionRequest("RETRY_MANUALLY", "   ");

        assertThatThrownBy(() -> exceptionService.resolveException(id, request, "ADMIN_BOB"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Resolution notes are required");

        verify(exceptionOutboxService, never()).recordExceptionResolved(any());
    }

    @Test
    @DisplayName("ignoreException: transitions to IGNORED, triggers outbox event (BR-017, BR-018)")
    void ignoreException_success() {
        UUID id = sampleRecord.getId();
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));
        when(exceptionRecordRepository.save(any(ExceptionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExceptionRecordDto result = exceptionService.ignoreException(id, "False alarm from monitoring tool", "OPERATOR_ALICE");

        assertThat(result.status()).isEqualTo(ExceptionRecordStatus.IGNORED);
        assertThat(result.resolvedBy()).isEqualTo("OPERATOR_ALICE");
        assertThat(result.resolutionAction()).isEqualTo("IGNORED");
        assertThat(result.resolutionNotes()).isEqualTo("False alarm from monitoring tool");

        verify(exceptionRecordRepository).save(sampleRecord);
        verify(exceptionOutboxService).recordExceptionIgnored(sampleRecord);
    }

    @Test
    @DisplayName("terminal states: RESOLVED and IGNORED reject further transitions (InvalidStateException)")
    void terminalStates_rejectTransitions() {
        UUID id = sampleRecord.getId();
        sampleRecord.resolve("ADMIN_BOB", "CANCEL_ORDER", "Order cancelled manually");
        when(exceptionRecordRepository.findById(id)).thenReturn(Optional.of(sampleRecord));

        assertThatThrownBy(() -> exceptionService.assignException(id, "OPERATOR_ALICE"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Terminal exception status");
    }

    @Test
    @DisplayName("getDashboardSummary: aggregates telemetry metrics and open by type breakdown")
    void getDashboardSummary_aggregatesCorrectly() {
        when(exceptionRecordRepository.countByStatus(ExceptionRecordStatus.OPEN)).thenReturn(10L);
        when(exceptionRecordRepository.countByStatus(ExceptionRecordStatus.INVESTIGATING)).thenReturn(3L);
        when(exceptionRecordRepository.countByStatus(ExceptionRecordStatus.RESOLVED)).thenReturn(25L);

        List<Object[]> rawCounts = List.of(
                new Object[]{ExceptionType.PAYMENT_FAILED, 6L},
                new Object[]{ExceptionType.DUPLICATE_PAYMENT, 4L}
        );
        when(exceptionRecordRepository.countOpenByType()).thenReturn(rawCounts);

        DashboardSummaryDto summary = exceptionService.getDashboardSummary();

        assertThat(summary.totalOpenExceptions()).isEqualTo(10L);
        assertThat(summary.totalInvestigatingExceptions()).isEqualTo(3L);
        assertThat(summary.totalResolvedExceptions()).isEqualTo(25L);
        assertThat(summary.openExceptionsByType().get("PAYMENT_FAILED")).isEqualTo(6L);
        assertThat(summary.openExceptionsByType().get("DUPLICATE_PAYMENT")).isEqualTo(4L);
        assertThat(summary.openExceptionsByType().get("STUCK_ORDER")).isEqualTo(0L);
    }
}
