-- V11: Add active flag to cost_centers for soft-deactivation
-- Cost centers removed from Conta Azul are marked inactive rather than deleted,
-- preserving referential integrity with financial_events that reference them.
ALTER TABLE cost_centers
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_cc_company_active
    ON cost_centers(company_id, active);
