-- V13: Tabela de rateio por centro de custo dentro de um rateio de categoria
-- Cada linha de financial_event_allocations pode ter N centros de custo,
-- representando o rateio_centro_custo[] da API Conta Azul.
CREATE TABLE financial_event_allocation_cost_centers (
    id                      BIGSERIAL       PRIMARY KEY,
    company_id              BIGINT          NOT NULL REFERENCES companies(id),
    allocation_id           BIGINT          NOT NULL
                                REFERENCES financial_event_allocations(id) ON DELETE CASCADE,
    -- FK para a entidade local (pode ser NULL se o CC não foi sincronizado ainda)
    cost_center_id          BIGINT          REFERENCES cost_centers(id),
    -- snapshot do ID externo (Conta Azul) no momento do sync
    external_cost_center_id VARCHAR(100),
    -- snapshot do nome no momento do sync
    cost_center_name        VARCHAR(150),
    -- valor desta fatia de rateio por CC (soma = allocated_amount da allocation pai)
    allocated_amount        NUMERIC(15,2)   NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Índices para queries analíticas por empresa, allocation e centro de custo
CREATE INDEX idx_feacc_company_id       ON financial_event_allocation_cost_centers(company_id);
CREATE INDEX idx_feacc_allocation_id    ON financial_event_allocation_cost_centers(allocation_id);
CREATE INDEX idx_feacc_cost_center_id   ON financial_event_allocation_cost_centers(cost_center_id)
    WHERE cost_center_id IS NOT NULL;
CREATE INDEX idx_feacc_company_cc       ON financial_event_allocation_cost_centers(company_id, cost_center_id)
    WHERE cost_center_id IS NOT NULL;
CREATE INDEX idx_feacc_ext_cc           ON financial_event_allocation_cost_centers(company_id, external_cost_center_id)
    WHERE external_cost_center_id IS NOT NULL;

-- Unicidade: uma allocation não pode ter duas linhas para o mesmo external_cost_center_id
CREATE UNIQUE INDEX idx_feacc_alloc_ext_cc
    ON financial_event_allocation_cost_centers(allocation_id, external_cost_center_id)
    WHERE external_cost_center_id IS NOT NULL;
