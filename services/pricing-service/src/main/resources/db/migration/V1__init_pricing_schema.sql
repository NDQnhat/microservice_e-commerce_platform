-- V1: Pricing Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (PRICE, PRICE_PROMOTION)

CREATE TABLE price (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL,
    base_price NUMERIC(14, 2) NOT NULL CHECK (base_price > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_price_sku_id ON price(sku_id);
CREATE INDEX idx_price_effective ON price(sku_id, effective_from, effective_to);

CREATE TABLE price_promotion (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL,
    sale_price NUMERIC(14, 2) NOT NULL CHECK (sale_price > 0),
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_promotion_dates CHECK (start_at < end_at)
);

CREATE INDEX idx_price_promotion_sku ON price_promotion(sku_id, status);
