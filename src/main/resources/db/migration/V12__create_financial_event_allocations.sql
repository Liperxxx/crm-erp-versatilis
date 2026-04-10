-- V12: Tabela de rateio por categoria de um evento financeiro
-- Suporta múltiplas categorias por evento (rateio completo do Conta Azul).
-- Os campos external_* e *_name são cópias defensivas dos valores snapshot no
-- momento do sync, garantindo histórico mesmo se category_id for desativado.
CREATE TABLE financial_event_allocations (
    id                    BIGSERIAL       PRIMARY KEY,
    company_id            BIGINT          NOT NULL REFERENCES companies(id),
    financial_event_id    BIGINT          NOT NULL REFERENCES financial_events(id) ON DELETE CASCADE,
    -- FK para a entidade local (pode ser NULL se a categoria não foi sincronizada ainda)
    category_id           BIGINT          REFERENCES financial_categories(id),
    -- snapshot do ID externo (Conta Azul) no momento do sync
    external_category_id  VARCHAR(100),
    -- snapshot do nome no momento do sync
    category_name         VARCHAR(150),
    -- valor desta fatia de rateio (soma de todas as linhas = valor total do evento)
    allocated_amount      NUMERIC(15,2)   NOT NULL,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Índices para queries analíticas por empresa, evento e categoria
CREATE INDEX idx_fea_company_id         ON financial_event_allocations(company_id);
CREATE INDEX idx_fea_event_id           ON financial_event_allocations(financial_event_id);
CREATE INDEX idx_fea_category_id        ON financial_event_allocations(category_id)
    WHERE category_id IS NOT NULL;
CREATE INDEX idx_fea_company_category   ON financial_event_allocations(company_id, category_id)
    WHERE category_id IS NOT NULL;
CREATE INDEX idx_fea_ext_category       ON financial_event_allocations(company_id, external_category_id)
    WHERE external_category_id IS NOT NULL;

-- Unicidade: um evento não pode ter duas linhas para o mesmo external_category_id
CREATE UNIQUE INDEX idx_fea_event_ext_cat
    ON financial_event_allocations(financial_event_id, external_category_id)
    WHERE external_category_id IS NOT NULL;
