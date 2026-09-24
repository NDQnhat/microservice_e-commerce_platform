CREATE TABLE IF NOT EXISTS exception_records (
    id UUID PRIMARY KEY,
    exception_type VARCHAR(100) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    reference_type VARCHAR(100) NOT NULL,
    error_code VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    payload TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_to VARCHAR(100),
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_action VARCHAR(100),
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_exception_records_status ON exception_records (status);
CREATE INDEX idx_exception_records_type ON exception_records (exception_type);
CREATE INDEX idx_exception_records_ref ON exception_records (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_exception_outbox_status ON outbox_events (status, created_at);
