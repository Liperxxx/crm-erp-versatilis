-- V10: Add active flag to financial_categories for soft-deactivation
-- Categories removed from Conta Azul are marked inactive rather than deleted,
-- preserving referential integrity with financial_events that reference them.
ALTER TABLE financial_categories
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_fcat_company_active
    ON financial_categories(company_id, active);
