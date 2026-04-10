-- V14: Dados demonstrativos de categorias financeiras, centros de custo e eventos financeiros.
-- Cobre Jan/2025 – Jun/2026 para Bustech (company_id=1) e Versatilis (company_id=2).
-- execute apenas em ambiente de desenvolvimento/homologação.

-- ═══════════════════════════════════════════════════════════════
-- CATEGORIAS FINANCEIRAS
-- ═══════════════════════════════════════════════════════════════

INSERT INTO financial_categories (company_id, name, type, active) VALUES
  -- Bustech
  (1, 'Desenvolvimento de Software', 'INCOME',  TRUE),
  (1, 'Consultoria',                 'INCOME',  TRUE),
  (1, 'Suporte e Manutencao',        'INCOME',  TRUE),
  (1, 'Folha de Pagamento',          'EXPENSE', TRUE),
  (1, 'Infraestrutura e Cloud',      'EXPENSE', TRUE),
  (1, 'Marketing',                   'EXPENSE', TRUE),
  -- Versatilis
  (2, 'Vendas de Produtos',          'INCOME',  TRUE),
  (2, 'Prestacao de Servicos',       'INCOME',  TRUE),
  (2, 'Estoque e Compras',           'EXPENSE', TRUE),
  (2, 'Pessoal',                     'EXPENSE', TRUE),
  (2, 'Operacional',                 'EXPENSE', TRUE);

-- ═══════════════════════════════════════════════════════════════
-- CENTROS DE CUSTO
-- ═══════════════════════════════════════════════════════════════

INSERT INTO cost_centers (company_id, name, active) VALUES
  -- Bustech
  (1, 'Desenvolvimento', TRUE),
  (1, 'Comercial',       TRUE),
  (1, 'Administrativo',  TRUE),
  -- Versatilis
  (2, 'Vendas',          TRUE),
  (2, 'Operacoes',       TRUE),
  (2, 'Administrativo',  TRUE);

-- ═══════════════════════════════════════════════════════════════
-- EVENTOS FINANCEIROS — BUSTECH (company_id = 1)
-- Período: Jan/2025 – Jun/2026  (18 meses)
-- Variação por MOD(mês, N) para gerar sazonalidade realista.
-- paid_date = issue_date (recebimento no mesmo dia = cash recebido)
-- ═══════════════════════════════════════════════════════════════

-- RECEITA: Desenvolvimento de Software  (~42-56k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    1,
    'INCOME',
    (42000 + (MOD(EXTRACT(MONTH FROM d)::INT, 4) * 3500))::NUMERIC(15,2),
    'Receita Desenvolvimento - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 1 AND name = 'Desenvolvimento de Software'),
    (SELECT id FROM cost_centers           WHERE company_id = 1 AND name = 'Desenvolvimento')
FROM GENERATE_SERIES('2025-01-20'::DATE, '2026-06-20'::DATE, '1 month'::INTERVAL) AS d;

-- RECEITA: Consultoria  (~22-26k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    1,
    'INCOME',
    (22000 + (MOD(EXTRACT(MONTH FROM d)::INT, 3) * 2000))::NUMERIC(15,2),
    'Consultoria - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 1 AND name = 'Consultoria'),
    (SELECT id FROM cost_centers           WHERE company_id = 1 AND name = 'Comercial')
FROM GENERATE_SERIES('2025-01-15'::DATE, '2026-06-15'::DATE, '1 month'::INTERVAL) AS d;

-- RECEITA: Suporte e Manutencao  (~13.5-17k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    1,
    'INCOME',
    (13500 + (MOD(EXTRACT(MONTH FROM d)::INT, 5) * 800))::NUMERIC(15,2),
    'Contrato Suporte - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 1 AND name = 'Suporte e Manutencao'),
    (SELECT id FROM cost_centers           WHERE company_id = 1 AND name = 'Desenvolvimento')
FROM GENERATE_SERIES('2025-01-05'::DATE, '2026-06-05'::DATE, '1 month'::INTERVAL) AS d;

-- DESPESA: Folha de Pagamento  (~34-35.5k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    1,
    'EXPENSE',
    (34000 + (MOD(EXTRACT(MONTH FROM d)::INT, 2) * 1500))::NUMERIC(15,2),
    'Folha de Pagamento - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 1 AND name = 'Folha de Pagamento'),
    (SELECT id FROM cost_centers           WHERE company_id = 1 AND name = 'Administrativo')
FROM GENERATE_SERIES('2025-01-28'::DATE, '2026-06-28'::DATE, '1 month'::INTERVAL) AS d;

-- DESPESA: Infraestrutura e Cloud  (~13-15.4k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    1,
    'EXPENSE',
    (13000 + (MOD(EXTRACT(MONTH FROM d)::INT, 3) * 1200))::NUMERIC(15,2),
    'Infraestrutura Cloud - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 1 AND name = 'Infraestrutura e Cloud'),
    (SELECT id FROM cost_centers           WHERE company_id = 1 AND name = 'Desenvolvimento')
FROM GENERATE_SERIES('2025-01-10'::DATE, '2026-06-10'::DATE, '1 month'::INTERVAL) AS d;

-- DESPESA: Marketing  (~7-8.5k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    1,
    'EXPENSE',
    (7000 + (MOD(EXTRACT(MONTH FROM d)::INT, 4) * 500))::NUMERIC(15,2),
    'Campanhas Marketing - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 1 AND name = 'Marketing'),
    (SELECT id FROM cost_centers           WHERE company_id = 1 AND name = 'Comercial')
FROM GENERATE_SERIES('2025-01-12'::DATE, '2026-06-12'::DATE, '1 month'::INTERVAL) AS d;

-- ═══════════════════════════════════════════════════════════════
-- EVENTOS FINANCEIROS — VERSATILIS (company_id = 2)
-- ═══════════════════════════════════════════════════════════════

-- RECEITA: Vendas de Produtos  (~50-66k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    2,
    'INCOME',
    (50000 + (MOD(EXTRACT(MONTH FROM d)::INT, 5) * 4000))::NUMERIC(15,2),
    'Vendas Produtos - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 2 AND name = 'Vendas de Produtos'),
    (SELECT id FROM cost_centers           WHERE company_id = 2 AND name = 'Vendas')
FROM GENERATE_SERIES('2025-01-20'::DATE, '2026-06-20'::DATE, '1 month'::INTERVAL) AS d;

-- RECEITA: Prestacao de Servicos  (~18-23k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    2,
    'INCOME',
    (18000 + (MOD(EXTRACT(MONTH FROM d)::INT, 3) * 2500))::NUMERIC(15,2),
    'Servicos Prestados - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 2 AND name = 'Prestacao de Servicos'),
    (SELECT id FROM cost_centers           WHERE company_id = 2 AND name = 'Operacoes')
FROM GENERATE_SERIES('2025-01-15'::DATE, '2026-06-15'::DATE, '1 month'::INTERVAL) AS d;

-- DESPESA: Estoque e Compras  (~28-35.5k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    2,
    'EXPENSE',
    (28000 + (MOD(EXTRACT(MONTH FROM d)::INT, 4) * 2500))::NUMERIC(15,2),
    'Compras Estoque - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 2 AND name = 'Estoque e Compras'),
    (SELECT id FROM cost_centers           WHERE company_id = 2 AND name = 'Operacoes')
FROM GENERATE_SERIES('2025-01-25'::DATE, '2026-06-25'::DATE, '1 month'::INTERVAL) AS d;

-- DESPESA: Pessoal  (~17-18k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    2,
    'EXPENSE',
    (17000 + (MOD(EXTRACT(MONTH FROM d)::INT, 2) * 1000))::NUMERIC(15,2),
    'Pessoal - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 2 AND name = 'Pessoal'),
    (SELECT id FROM cost_centers           WHERE company_id = 2 AND name = 'Administrativo')
FROM GENERATE_SERIES('2025-01-28'::DATE, '2026-06-28'::DATE, '1 month'::INTERVAL) AS d;

-- DESPESA: Operacional  (~9-11k/mês)
INSERT INTO financial_events
    (company_id, direction, amount, description, issue_date, due_date, paid_date, status, category_id, cost_center_id)
SELECT
    2,
    'EXPENSE',
    (9000 + (MOD(EXTRACT(MONTH FROM d)::INT, 3) * 1000))::NUMERIC(15,2),
    'Custos Operacionais - ' || TO_CHAR(d, 'MM/YYYY'),
    d::DATE,
    d::DATE,
    d::DATE,
    'PAID',
    (SELECT id FROM financial_categories WHERE company_id = 2 AND name = 'Operacional'),
    (SELECT id FROM cost_centers           WHERE company_id = 2 AND name = 'Operacoes')
FROM GENERATE_SERIES('2025-01-10'::DATE, '2026-06-10'::DATE, '1 month'::INTERVAL) AS d;
