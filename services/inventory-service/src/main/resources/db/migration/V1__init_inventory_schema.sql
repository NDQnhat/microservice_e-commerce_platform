-- V1: Inventory Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (INVENTORY, INVENTORY_RESERVATION, INVENTORY_ADJUSTMENT_LOG)

CREATE TABLE inventory (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL UNIQUE,
    quantity_on_hand INT NOT NULL CHECK (quantity_on_hand >= 0),
    quantity_reserved INT NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reserved_le_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_inventory_sku_id ON inventory(sku_id);

CREATE TABLE inventory_reservation (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL,
    order_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservation_order_sku ON inventory_reservation(order_id, sku_id);
CREATE INDEX idx_reservation_status_expires ON inventory_reservation(status, expires_at);

-- Immutable audit log table per BR-015 and NFR-AUDIT-002
CREATE TABLE inventory_adjustment_log (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    delta INT NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_adj_log_sku ON inventory_adjustment_log(sku_id, created_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

CREATE INDEX idx_inventory_outbox_status ON outbox_events(status, created_at);
