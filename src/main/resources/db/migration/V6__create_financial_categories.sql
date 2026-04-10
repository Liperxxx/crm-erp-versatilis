-- File: V6__create_financial_categories.sql
CREATE TABLE financial_categories (
    id          BIGSERIAL    PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES companies(id),
    external_id VARCHAR(100),
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fcat_company_id       ON financial_categories(company_id);
CREATE INDEX idx_fcat_company_type     ON financial_categories(company_id, type);
CREATE UNIQUE INDEX idx_fcat_company_external
    ON financial_categories(company_id, external_id)
    WHERE external_id IS NOT NULL;