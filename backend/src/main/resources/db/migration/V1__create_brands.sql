CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE brand_status AS ENUM (
    'DRAFT',
    'GENERATING',
    'READY',
    'FAILED'
);

CREATE TABLE brands (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status      brand_status NOT NULL DEFAULT 'DRAFT',
    brand_dna   JSONB,
    error       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_brands_status ON brands (status);
