-- V1: Notification Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (NOTIFICATION_TEMPLATE, NOTIFICATION_LOG)

CREATE TABLE notification_template (
    id UUID PRIMARY KEY,
    event_code VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'EMAIL',
    subject VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_noti_template_event ON notification_template(event_code, status);

CREATE TABLE notification_log (
    id UUID PRIMARY KEY,
    order_id UUID,
    customer_id UUID,
    template_id UUID NOT NULL REFERENCES notification_template(id),
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_noti_log_order ON notification_log(order_id);
CREATE INDEX idx_noti_log_status ON notification_log(status);

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

CREATE INDEX idx_notification_outbox_status ON outbox_events(status, created_at);
