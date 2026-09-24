-- V1: Payment Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (PAYMENT_TRANSACTION)

CREATE TABLE payment_transaction (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    provider_reference VARCHAR(128),
    amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_order_id ON payment_transaction(order_id);
CREATE INDEX idx_payment_status ON payment_transaction(status);

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

CREATE INDEX idx_payment_outbox_status ON outbox_events(status, created_at);
