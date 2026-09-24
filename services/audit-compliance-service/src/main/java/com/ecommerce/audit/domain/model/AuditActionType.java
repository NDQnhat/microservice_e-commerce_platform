package com.ecommerce.audit.domain.model;

public enum AuditActionType {
    ROLE_ASSIGN,
    CONFIG_CHANGE,
    INVENTORY_ADJUST,
    ORDER_STATE_TRANSITION,
    PAYMENT_RECONCILE
}
