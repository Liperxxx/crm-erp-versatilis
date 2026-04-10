# bustech-erp

Spring Boot 3.5 · Java 25 · PostgreSQL · Flyway · JWT

---

## Requisitos locais

| Ferramenta | Versão mínima |
|-----------|--------------|
| JDK (Eclipse Adoptium) | 25 |
| Maven | 3.9+ |
| PostgreSQL | 14+ |

---

## Executar localmente (VS Code / terminal)

### 1. Crie o banco de dados

```sql
CREATE DATABASE bustech_erp;
```

### 2. Configure variáveis (opcional no perfil `dev`)

O perfil `dev` usa valores padrão — nenhuma variável de ambiente é obrigatória para rodar localmente:

| Variável | Padrão dev |
|---------|-----------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/erp_dev` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | `postgres` |
| `JWT_SECRET` | valor inseguro embutido (`dev-secret-key-...`) |
| `CONTAAZUL_*` | vazio / placeholder |

Para sobrescrever, exporte as variáveis antes de iniciar:

```powershell
$env:DATABASE_URL      = "jdbc:postgresql://localhost:5432/bustech_erp"
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "suasenha"
```

### 3. Inicie a aplicação

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
mvn spring-boot:run
```

O perfil `dev` é ativado automaticamente. As migrações Flyway (V1–V13) rodam na primeira vez.

---

## Credenciais iniciais (perfil `dev` apenas)

> **Atenção**: criadas pelo `DevDataInitializer`. **Nunca use em produção.**

| E-mail | Senha | Empresa | Role |
|--------|-------|---------|------|
| `admin@bustech.com.br` | `admin@dev123!` | Bustech Tecnologia | ADMIN |
| `admin@versatilis.com.br` | `admin@dev123!` | Versatilis | ADMIN |

### Autenticação

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@bustech.com.br", "password": "admin@dev123!" }
```

Resposta: `{ "token": "<JWT>" }`

Use em todas as requisições: `Authorization: Bearer <JWT>`

---

## Segurança em produção

- Defina `SPRING_PROFILES_ACTIVE=prod`
- Forneça `JWT_SECRET` com no mínimo 32 caracteres aleatórios
- O `DevDataInitializer` **não é instanciado** fora do perfil `dev`

---

## Esquema de banco — Flyway migrations

| Versão | Tabela / alteração |
|--------|--------------------|
| V1 | `companies` |
| V2 | `users` |
| V3 | `financial_transactions` (legado) |
| V4 | `conta_azul_connections` |
| V5 | seed companies (dev) |
| V6 | `financial_categories` |
| V7 | `cost_centers` |
| V8 | `financial_accounts` |
| V9 | `financial_events` |
| V10 | `financial_categories.active` |
| V11 | `cost_centers.active` |
| V12 | `financial_event_allocations` — rateio por categoria |
| V13 | `financial_event_allocation_cost_centers` — rateio por centro de custo |

---

## Integração Conta Azul — sincronização

### O que é sincronizado

| Entidade | Tabela destino | Serviço |
|----------|---------------|---------|
| Categorias financeiras | `financial_categories` | `CategorySyncService` |
| Centros de custo | `cost_centers` | `CostCenterSyncService` |
| Eventos financeiros | `financial_events` | `FinancialEventSyncService` |
| Rateio por categoria (parcelas) | `financial_event_allocations` | `AllocationSyncService` |
| Rateio por centro de custo (parcelas) | `financial_event_allocation_cost_centers` | `AllocationSyncService` |

### Fluxo de sincronização

```
POST /api/integrations/conta-azul/sync/{companyId}
  │
  ├── sync categorias  → financial_categories
  ├── sync centros     → cost_centers
  ├── sync eventos     → financial_events
  └── sync rateio      → financial_event_allocations
                          financial_event_allocation_cost_centers
```

O rateio representa a divisão proporcional de um evento entre múltiplas categorias ou centros de custo. Cada parcela (`allocation`) pode ter N linhas de centro de custo (`allocation_cost_centers`).

### Endpoints de integração

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/integrations/conta-azul/authorize/{companyId}` | Inicia fluxo OAuth2 |
| GET | `/api/integrations/conta-azul/callback` | Callback OAuth2 |
| GET | `/api/integrations/conta-azul/status/{companyId}` | Status da conexão |
| POST | `/api/integrations/conta-azul/sync/{companyId}` | Sincroniza tudo |
| POST | `/api/integrations/conta-azul/sync-all` | Sincroniza todas as empresas |
| DELETE | `/api/integrations/conta-azul/connection/{companyId}` | Remove conexão |

### Endpoints DEV — injeção manual de token (`@Profile("dev")`)

Para desenvolvimento local sem URL pública de callback:

| Método | Path | Role |
|--------|------|------|
| POST | `/api/dev/conta-azul/token/{companyId}` | ADMIN |
| DELETE | `/api/dev/conta-azul/connection/{companyId}` | ADMIN |
| GET | `/api/dev/conta-azul/status/{companyId}` | ADMIN/MANAGER |

```http
POST /api/dev/conta-azul/token/1
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "access_token": "seu_access_token",
  "refresh_token": "seu_refresh_token",
  "expires_in": 3600
}
```

Depois: `POST /api/integrations/conta-azul/sync/1`

### Variáveis de ambiente — Conta Azul

| Variável | Obrigatória | Descrição |
|---------|-------------|-----------|
| `CONTAAZUL_CLIENT_ID` | Sim (OAuth2 real) | Client ID do app |
| `CONTAAZUL_CLIENT_SECRET` | Sim (OAuth2 real) | Client secret |
| `CONTAAZUL_REDIRECT_URI` | Sim (OAuth2 real) | Callback registrada. Dev default: `http://localhost:8081/api/integrations/conta-azul/callback` |
| `CONTAAZUL_FRONTEND_URL` | Não | Redirect pós-callback. Dev default: `http://localhost:3000/dashboard.html` |

---

## Dashboards analíticos

### Prioridade: rateio vs. evento direto

Todos os endpoints de categoria e centro de custo usam **lógica híbrida**:

1. **Eventos COM rateio** → valores lidos de `financial_event_allocations` / `financial_event_allocation_cost_centers`, preservando a divisão proporcional exata.
2. **Eventos SEM rateio** → valor lido diretamente de `financial_events`, usando o campo `category_id` ou `cost_center_id` do próprio evento.

Os dois conjuntos são somados por nome (merge aditivo). Nunca há dupla contagem — a presença de rateio exclui o evento da contagem direta.

---

### Dashboard por empresa — `/api/dashboard/company/{companyId}`

Parâmetros obrigatórios em todos os endpoints: `start` e `end` (formato `YYYY-MM-DD`).

#### Resumo e séries mensais

| GET | Path | Filtros opcionais |
|-----|------|-------------------|
| | `/summary` | `categoryId`, `costCenterId` |
| | `/monthly-revenue` | `categoryId`, `costCenterId` |
| | `/monthly-expenses` | `categoryId`, `costCenterId` |
| | `/monthly-profit` | `categoryId`, `costCenterId` |
| | `/cash-flow` | `categoryId`, `costCenterId` |

Os filtros opcionais são **backward-compatible**: omitir equivale ao comportamento anterior. Quando um filtro é passado, as queries usam `(:param IS NULL OR e.xxx.id = :param)` — nulls são no-ops no banco.

#### Breakdown simples (sem rateio)

| GET | Path |
|-----|------|
| | `/categories` |
| | `/cost-centers` |

#### Analytics por categoria (híbrido rateio-aware)

| GET | Path | Filtro opcional | Ordenação |
|-----|------|-----------------|-----------|
| | `/categories/summary` | `costCenterId` | totalRevenue desc |
| | `/categories/revenue` | `costCenterId` | totalRevenue desc |
| | `/categories/expenses` | `costCenterId` | totalExpenses desc |
| | `/categories/profit` | `costCenterId` | totalProfit desc |

Cada item retorna: `categoryName`, `totalRevenue`, `totalExpenses`, `totalProfit`, `revenueShare%`, `expenseShare%`.

#### Analytics por centro de custo (híbrido rateio-aware)

| GET | Path | Filtro opcional | Ordenação |
|-----|------|-----------------|-----------|
| | `/cost-centers/summary` | `categoryId` | totalRevenue desc |
| | `/cost-centers/revenue` | `categoryId` | totalRevenue desc |
| | `/cost-centers/expenses` | `categoryId` | totalExpenses desc |

Cada item retorna: `costCenterName`, `totalRevenue`, `totalExpenses`, `revenueShare%`, `expenseShare%`.

---

### Dashboard comparativo — `/api/dashboard/comparison`

Parâmetros obrigatórios: `companyA`, `companyB`, `start`, `end`.

#### Comparativos temporais

| GET | Path | Descrição |
|-----|------|-----------|
| | `/summary` | Totais do período + vencedor + % diferença |
| | `/monthly-results` | Receita + despesa + lucro mês a mês (ambas empresas) |
| | `/revenue` | Receita mês a mês com % diferença |
| | `/expenses` | Despesa mês a mês com % diferença |
| | `/margins` | Margem de lucro (%) mês a mês |

#### Comparativos por categoria

| GET | Path | Parâmetro extra | Descrição |
|-----|------|----------------|-----------|
| | `/categories` | `direction` (INCOME\|EXPENSE, default EXPENSE) | Breakdown simples por categoria |
| | `/categories/summary` | — | Lucro por categoria (rateio-aware) |
| | `/categories/revenue` | — | Receita por categoria (rateio-aware) |
| | `/categories/expenses` | — | Despesa por categoria (rateio-aware) |

#### Comparativos por centro de custo

| GET | Path | Parâmetro extra | Descrição |
|-----|------|----------------|-----------|
| | `/cost-centers` | `direction` (INCOME\|EXPENSE, default EXPENSE) | Breakdown simples por centro de custo |
| | `/cost-centers/summary` | — | Receita por CC (rateio-aware) |
| | `/cost-centers/revenue` | — | Receita por CC (rateio-aware) |
| | `/cost-centers/expenses` | — | Despesa por CC (rateio-aware) |

Nos endpoints rateio-aware, centros de custo/categorias presentes em apenas uma empresa aparecem com `0` na outra. Ordenação por magnitude combinada (A + B) decrescente.

---

### Dashboard consolidado — `/api/dashboard/consolidated`

Role mínima: `MANAGER`. Agrega todas as empresas do tenant.

| GET | Path |
|-----|------|
| | `/summary?start=&end=` |
| | `/monthly-results?start=&end=` |

---

## Como testar localmente

### 1. Obtenha um JWT

```powershell
$resp = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"email":"admin@bustech.com.br","password":"admin@dev123!"}'
$token = $resp.token
```

### 2. Consulte um dashboard

```powershell
# Analytics de categoria — empresa 1, ano 2025
Invoke-RestMethod `
  -Uri "http://localhost:8081/api/dashboard/company/1/categories/summary?start=2025-01-01&end=2025-12-31" `
  -Headers @{ Authorization = "Bearer $token" }

# Filtrar por centro de custo
Invoke-RestMethod `
  -Uri "http://localhost:8081/api/dashboard/company/1/categories/revenue?start=2025-01-01&end=2025-12-31&costCenterId=3" `
  -Headers @{ Authorization = "Bearer $token" }

# Comparativo de categorias entre empresas
Invoke-RestMethod `
  -Uri "http://localhost:8081/api/dashboard/comparison/categories/expenses?companyA=1&companyB=2&start=2025-01-01&end=2025-12-31" `
  -Headers @{ Authorization = "Bearer $token" }
```

### 3. Dispare uma sincronização (requer token válido de Conta Azul)

```powershell
# Injete o token manualmente (dev only)
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/dev/conta-azul/token/1" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"access_token":"<token CA>","refresh_token":"<refresh>","expires_in":3600}'

# Sincronize
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/integrations/conta-azul/sync/1" `
  -Headers @{ Authorization = "Bearer $token" }
```

---

## Limitações e cuidados

| # | Ponto | Detalhe |
|---|-------|---------|
| 1 | **Somente eventos PAID** | Todos os endpoints de analytics consideram apenas eventos com `status = 'PAID'` e `paid_date` no período. Pendentes e vencidos não entram nos totais. |
| 2 | **Rateio parcial** | Se um evento tiver rateio em apenas algumas parcelas, as parcelas sem rateio entram via FK direta. Isso é intencional e não gera dupla contagem. |
| 3 | **Nome como chave** | O merge entre rateio e eventos diretos é feito por `categoryName` / `costCenterName` (string). Renomear uma categoria na Conta Azul após sincronizações anteriores pode criar entradas duplicadas no dashboard até uma nova sync completa. |
| 4 | **Filtros nos comparativos** | Os endpoints `/comparison/*` não suportam ainda `categoryId`/`costCenterId` como filtro — apenas os endpoints `/company/{id}/*` possuem essa camada. |
| 5 | **Sincronização incremental** | A sync atual é full-replace por empresa (upsert por `external_id`). Eventos deletados na Conta Azul não são removidos automaticamente. |
| 6 | **JWT sem rotação** | O refresh de JWT não está implementado. Tokens expiram conforme `JWT_EXPIRATION_MS` (default dev: 24 h). |
