-- V1: Fulfillment Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (SHIPMENT)

CREATE TABLE shipment (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    carrier_name VARCHAR(128),
    tracking_code VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'PACKING',
    packed_at TIMESTAMP WITH TIME ZONE,
    shipped_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_shipment_order_id ON shipment(order_id);
CREATE INDEX idx_shipment_status ON shipment(status);

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

CREATE INDEX idx_fulfillment_outbox_status ON outbox_events(status, created_at);
