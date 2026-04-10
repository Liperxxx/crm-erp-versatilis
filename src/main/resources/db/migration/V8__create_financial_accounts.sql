-- File: V8__create_financial_accounts.sql
CREATE TABLE financial_accounts (
    id          BIGSERIAL     PRIMARY KEY,
    company_id  BIGINT        NOT NULL REFERENCES companies(id),
    external_id VARCHAR(100),
    name        VARCHAR(150)  NOT NULL,
    balance     NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_facc_company_id ON financial_accounts(company_id);
CREATE UNIQUE INDEX idx_facc_company_external
    ON financial_accounts(company_id, external_id)
    WHERE external_id IS NOT NULL;