package com.ecommerce.exception.domain.repository;

import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ExceptionRecordSpecification {

    private ExceptionRecordSpecification() {
    }

    public static Specification<ExceptionRecord> withFilters(
            ExceptionRecordStatus status,
            ExceptionType exceptionType,
            String referenceType
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (exceptionType != null) {
                predicates.add(cb.equal(root.get("exceptionType"), exceptionType));
            }
            if (referenceType != null && !referenceType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("referenceType")), referenceType.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
