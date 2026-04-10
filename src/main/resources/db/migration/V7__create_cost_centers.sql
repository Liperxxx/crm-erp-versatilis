-- File: V7__create_cost_centers.sql
CREATE TABLE cost_centers (
    id          BIGSERIAL    PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES companies(id),
    external_id VARCHAR(100),
    name        VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cc_company_id ON cost_centers(company_id);
CREATE UNIQUE INDEX idx_cc_company_external
    ON cost_centers(company_id, external_id)
    WHERE external_id IS NOT NULL;