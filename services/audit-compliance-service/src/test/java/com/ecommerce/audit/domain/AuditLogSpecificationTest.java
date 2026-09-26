package com.ecommerce.audit.domain;

import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.domain.model.AuditLog;
import com.ecommerce.audit.domain.repository.AuditLogSpecification;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogSpecificationTest {

    @Mock
    private Root<AuditLog> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Expression<String> expression;

    @Mock
    private Predicate predicate;

    @Test
    @DisplayName("AuditLogSpecification: returns empty predicate when all filter fields are null")
    void shouldCreateSpecificationWithNoFilters() {
        Specification<AuditLog> spec = AuditLogSpecification.withFilters(
                null, null, null, null, null, null, null
        );

        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();
        verify(cb).and(new Predicate[0]);
    }

    @Test
    @DisplayName("AuditLogSpecification: combines all filter predicates when provided")
    void shouldCreateSpecificationWithAllFilters() {
        UUID actorId = UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        doReturn(path).when(root).get(anyString());
        doReturn(expression).when(cb).upper(any());
        doReturn(predicate).when(cb).equal(any(), any());
        doReturn(predicate).when(cb).greaterThanOrEqualTo(any(), any(Instant.class));
        doReturn(predicate).when(cb).lessThanOrEqualTo(any(), any(Instant.class));
        doReturn(predicate).when(cb).and(any(Predicate[].class));

        Specification<AuditLog> spec = AuditLogSpecification.withFilters(
                actorId,
                "SUPER_ADMIN",
                AuditActionType.CONFIG_CHANGE,
                "BUSINESS_CONFIGURATION",
                "KEY_01",
                from,
                to
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        verify(cb).equal(any(), eq(actorId));
        verify(cb).equal(any(), eq("SUPER_ADMIN"));
        verify(cb).equal(any(), eq(AuditActionType.CONFIG_CHANGE));
        verify(cb).equal(any(), eq("BUSINESS_CONFIGURATION"));
        verify(cb).equal(any(), eq("KEY_01"));
        verify(cb).greaterThanOrEqualTo(any(), eq(from));
        verify(cb).lessThanOrEqualTo(any(), eq(to));
    }
}
