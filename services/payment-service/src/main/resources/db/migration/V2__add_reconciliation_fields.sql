-- V2: Add evidence reference and reconciliation reason to payment_transaction
-- Sourced from FINAL_SRS FR-039, BR-012, BR-015

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS evidence_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reconciliation_reason TEXT;
