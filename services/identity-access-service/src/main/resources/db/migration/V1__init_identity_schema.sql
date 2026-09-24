-- V1: Identity and Access Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (USER_ACCOUNT, CUSTOMER_ADDRESS, ROLE, PERMISSION, USER_ROLE, ROLE_PERMISSION)

CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    full_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_account_email ON user_account(email);
CREATE INDEX idx_user_account_status ON user_account(status);

CREATE TABLE customer_address (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    ward VARCHAR(128) NOT NULL,
    district VARCHAR(128) NOT NULL,
    city VARCHAR(128) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_address_customer_id ON customer_address(customer_id);

CREATE TABLE role (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE permission (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

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

CREATE INDEX idx_identity_outbox_status ON outbox_events(status, created_at);

-- Seed confirmed Roles per SRS Section 2.3.1
INSERT INTO role (id, code) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN'),
    ('00000000-0000-0000-0000-000000000002', 'CATALOG_ADMIN'),
    ('00000000-0000-0000-0000-000000000003', 'INVENTORY_ADMIN'),
    ('00000000-0000-0000-0000-000000000004', 'ORDER_OPS_ADMIN'),
    ('00000000-0000-0000-0000-000000000005', 'CUSTOMER_SUPPORT');
