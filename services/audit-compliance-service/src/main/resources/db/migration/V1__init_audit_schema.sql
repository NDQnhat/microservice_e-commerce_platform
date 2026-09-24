-- V1: Audit & Compliance Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (AUDIT_LOG)
-- Append-only; no update or delete capability exists (BR-015, NFR-AUDIT-002)

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    before_value TEXT,
    after_value TEXT,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_log_action ON audit_log(action_type);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
