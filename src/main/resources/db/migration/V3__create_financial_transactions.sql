CREATE TABLE financial_transactions (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT         NOT NULL REFERENCES companies(id),
    type             VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    description      VARCHAR(255)   NOT NULL,
    amount           NUMERIC(18,2)  NOT NULL,
    paid_amount      NUMERIC(18,2),
    due_date         DATE           NOT NULL,
    payment_date     DATE,
    category         VARCHAR(100),
    cost_center      VARCHAR(100),
    notes            VARCHAR(255),
    external_id      VARCHAR(100),
    external_source  VARCHAR(50),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ft_company_id          ON financial_transactions(company_id);
CREATE INDEX idx_ft_company_type        ON financial_transactions(company_id, type);
CREATE INDEX idx_ft_company_status      ON financial_transactions(company_id, status);
CREATE INDEX idx_ft_due_date            ON financial_transactions(due_date);
CREATE INDEX idx_ft_external            ON financial_transactions(external_id, external_source);
