-- V1: Catalog Service Initial Schema
-- Sourced directly from FINAL_SRS Section 11 (CATEGORY, PRODUCT, PRODUCT_ATTRIBUTE, PRODUCT_ATTRIBUTE_VALUE, SKU, SKU_ATTRIBUTE_VALUE, PRODUCT_MEDIA)

CREATE TABLE category (
    id UUID PRIMARY KEY,
    parent_category_id UUID REFERENCES category(id),
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_category_slug ON category(slug);
CREATE INDEX idx_category_parent ON category(parent_category_id);

CREATE TABLE product (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES category(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_category ON product(category_id);
CREATE INDEX idx_product_status ON product(status);

CREATE TABLE product_attribute (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE product_attribute_value (
    id UUID PRIMARY KEY,
    attribute_id UUID NOT NULL REFERENCES product_attribute(id) ON DELETE CASCADE,
    value VARCHAR(128) NOT NULL,
    CONSTRAINT uq_attribute_value UNIQUE (attribute_id, value)
);

CREATE TABLE sku (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    sku_code VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sku_product_id ON sku(product_id);
CREATE INDEX idx_sku_code ON sku(sku_code);

CREATE TABLE sku_attribute_value (
    sku_id UUID NOT NULL REFERENCES sku(id) ON DELETE CASCADE,
    attribute_value_id UUID NOT NULL REFERENCES product_attribute_value(id) ON DELETE CASCADE,
    PRIMARY KEY (sku_id, attribute_value_id)
);

CREATE TABLE product_media (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    sku_id UUID REFERENCES sku(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_media_product ON product_media(product_id);
