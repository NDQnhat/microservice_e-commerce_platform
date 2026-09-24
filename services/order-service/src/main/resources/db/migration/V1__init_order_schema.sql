-- V1: Order Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (ORDERS, ORDER_ITEM, ORDER_TIMELINE_EVENT)

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    shipping_recipient_name VARCHAR(255) NOT NULL,
    shipping_phone VARCHAR(32) NOT NULL,
    shipping_line1 VARCHAR(255) NOT NULL,
    shipping_line2 VARCHAR(255),
    shipping_ward VARCHAR(128) NOT NULL,
    shipping_district VARCHAR(128) NOT NULL,
    shipping_city VARCHAR(128) NOT NULL,
    subtotal_amount NUMERIC(14, 2) NOT NULL CHECK (subtotal_amount >= 0),
    shipping_fee_amount NUMERIC(14, 2) NOT NULL CHECK (shipping_fee_amount >= 0),
    discount_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    grand_total_amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_placed_at ON orders(placed_at);

CREATE TABLE order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku_id UUID,
    product_name_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(128) NOT NULL,
    attribute_snapshot TEXT NOT NULL,
    unit_price_snapshot NUMERIC(14, 2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_order_item_order ON order_item(order_id);

-- Immutable timeline table per BR-008 and NFR-AUDIT-002
CREATE TABLE order_timeline_event (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    actor_id UUID,
    actor_type VARCHAR(32) NOT NULL,
    note TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_timeline_order ON order_timeline_event(order_id, occurred_at);

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

CREATE INDEX idx_order_outbox_status ON outbox_events(status, created_at);
