package com.ecommerce.exception.domain.repository;

import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExceptionRecordRepository extends JpaRepository<ExceptionRecord, UUID> {

    Page<ExceptionRecord> findByStatus(ExceptionRecordStatus status, Pageable pageable);

    Page<ExceptionRecord> findByExceptionType(ExceptionType exceptionType, Pageable pageable);

    Optional<ExceptionRecord> findByReferenceTypeAndReferenceIdAndStatus(
            String referenceType, String referenceId, ExceptionRecordStatus status);

    @Query("SELECT COUNT(e) FROM ExceptionRecord e WHERE e.status = :status")
    long countByStatus(@Param("status") ExceptionRecordStatus status);

    @Query("SELECT e.exceptionType, COUNT(e) FROM ExceptionRecord e WHERE e.status = 'OPEN' GROUP BY e.exceptionType")
    List<Object[]> countOpenByType();
}
