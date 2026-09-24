package com.ecommerce.exception.domain.model;

public enum ExceptionType {
    PAYMENT_FAILED,
    STUCK_ORDER,
    NOTIFICATION_FAILED,
    DUPLICATE_PAYMENT,
    LATE_OR_STALE_PAYMENT_CALLBACK,
    INVENTORY_DISCREPANCY,
    UNKNOWN_PROCESSING_ERROR
}
