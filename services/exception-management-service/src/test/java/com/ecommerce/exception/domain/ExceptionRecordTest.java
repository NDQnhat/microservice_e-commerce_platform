package com.ecommerce.exception.domain;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.InvalidStateException;
import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionRecordTest {

    @Test
    @DisplayName("Should create exception record with OPEN status")
    void testCreateExceptionRecord() {
        ExceptionRecord record = ExceptionRecord.create(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-12345",
                "ORDER",
                "PAY_ERR_INSUFFICIENT_FUNDS",
                "Payment declined due to insufficient funds",
                "{\"amount\":100}"
        );

        assertNotNull(record.getId());
        assertEquals(ExceptionRecordStatus.OPEN, record.getStatus());
        assertEquals(ExceptionType.PAYMENT_FAILED, record.getExceptionType());
        assertEquals("payment-service", record.getSourceService());
        assertEquals("ORD-12345", record.getReferenceId());
    }

    @Test
    @DisplayName("Should assign exception record to operator")
    void testAssign() {
        ExceptionRecord record = ExceptionRecord.create(
                ExceptionType.STUCK_ORDER,
                "order-service",
                "ORD-999",
                "ORDER",
                "ORDER_TIMEOUT",
                "Order stuck in processing",
                null
        );

        record.assign("OPERATOR_1");
        assertEquals(ExceptionRecordStatus.INVESTIGATING, record.getStatus());
        assertEquals("OPERATOR_1", record.getAssignedTo());
    }

    @Test
    @DisplayName("Should resolve exception record with operator and notes (BR-017, BR-018)")
    void testResolveSuccess() {
        ExceptionRecord record = ExceptionRecord.create(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-12345",
                "ORDER",
                "GATEWAY_TIMEOUT",
                "Gateway timeout during charge",
                null
        );

        record.resolve("ADMIN_OPERATOR", "RETRY_PAYMENT", "Verified with bank, retrying transaction.");

        assertEquals(ExceptionRecordStatus.RESOLVED, record.getStatus());
        assertEquals("ADMIN_OPERATOR", record.getResolvedBy());
        assertEquals("RETRY_PAYMENT", record.getResolutionAction());
        assertEquals("Verified with bank, retrying transaction.", record.getResolutionNotes());
        assertNotNull(record.getResolvedAt());
    }

    @Test
    @DisplayName("Should fail resolve without notes (BR-018)")
    void testResolveFailsWithoutNotes() {
        ExceptionRecord record = ExceptionRecord.create(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-12345",
                "ORDER",
                "GATEWAY_TIMEOUT",
                "Gateway timeout",
                null
        );

        assertThrows(BusinessRuleException.class, () -> record.resolve("ADMIN_OPERATOR", "RETRY_PAYMENT", ""));
    }

    @Test
    @DisplayName("Should not transition from RESOLVED to any other state")
    void testTerminalState() {
        ExceptionRecord record = ExceptionRecord.create(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-12345",
                "ORDER",
                "GATEWAY_TIMEOUT",
                "Gateway timeout",
                null
        );

        record.resolve("ADMIN_OPERATOR", "RETRY_PAYMENT", "Resolved successfully");

        assertThrows(InvalidStateException.class, () -> record.assign("OTHER_OPERATOR"));
    }
}
