-- File: V9__create_financial_events.sql
CREATE TABLE financial_events (
    id                   BIGSERIAL     PRIMARY KEY,
    company_id           BIGINT        NOT NULL REFERENCES companies(id),
    external_id          VARCHAR(100),
    direction            VARCHAR(10)   NOT NULL,
    amount               NUMERIC(15,2) NOT NULL,
    description          VARCHAR(255),
    issue_date           DATE,
    due_date             DATE,
    paid_date            DATE,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    category_id          BIGINT        REFERENCES financial_categories(id),
    cost_center_id       BIGINT        REFERENCES cost_centers(id),
    financial_account_id BIGINT        REFERENCES financial_accounts(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fe_company_id   ON financial_events(company_id);
CREATE INDEX idx_fe_due_date     ON financial_events(company_id, due_date);
CREATE INDEX idx_fe_paid_date    ON financial_events(company_id, paid_date);
CREATE INDEX idx_fe_direction    ON financial_events(company_id, direction);
CREATE INDEX idx_fe_status       ON financial_events(company_id, status);
CREATE UNIQUE INDEX idx_fe_company_external
    ON financial_events(company_id, external_id)
    WHERE external_id IS NOT NULL;