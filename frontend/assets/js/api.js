/**
 * api.js — Authenticated HTTP client + endpoint catalog
 * Integrates with the Spring Boot backend at BASE_URL.
 */

const API = {
  BASE: 'http://localhost:8081',

  /** Always-fresh headers including Authorization when logged in. */
  headers() {
    const token = localStorage.getItem('erp_token');
    const h = { 'Content-Type': 'application/json' };
    if (token) h['Authorization'] = 'Bearer ' + token;
    return h;
  },

  /** Generic GET — returns parsed `data` field from ApiResponse<T>. */
  async get(path) {
    let res;
    try {
      res = await fetch(this.BASE + path, { headers: this.headers() });
    } catch (_) {
      throw new Error('Sem conexão com o servidor. Verifique se o backend está rodando em ' + this.BASE);
    }
    if (res.status === 401) {
      API.logout();
      return null;
    }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Erro HTTP ' + res.status);
    }
    const json = await res.json();
    return json.data;
  },

  /** Generic POST with JSON body — returns parsed `data` field. */
  async post(path, body) {
    let res;
    try {
      res = await fetch(this.BASE + path, {
        method: 'POST',
        headers: this.headers(),
        body: JSON.stringify(body),
      });
    } catch (_) {
      throw new Error('Sem conexão com o servidor.');
    }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Erro HTTP ' + res.status);
    }
    const json = await res.json();
    return json.data;
  },

  /** Clear session and redirect to login. */
  logout() {
    localStorage.removeItem('erp_token');
    localStorage.removeItem('erp_user');
    window.location.href = '/login.html';
  },

  // ── Endpoint factories ──────────────────────────────────────────────────

  // Auth
  LOGIN: '/api/auth/login',

  // ── Dashboard v1 (real backend paths) ─────────────────────────────────
  // GET /api/v1/dashboard?year=Y  → DashboardResponse for the authenticated user's company
  //   { companySummary: {companyId,companyName,totalIncome,totalExpense,netResult,overdueAmount,pendingCount,overdueCount},
  //     monthlyIncome: [{year,month,monthLabel,amount}],
  //     monthlyExpense: [{...}] }
  dashboardCompany:    (year) => `/api/v1/dashboard?year=${year}`,

  // GET /api/v1/dashboard/comparative?year=Y  → ComparativeDashboardResponse (ADMIN only)
  //   { companies: [CompanySummaryDto], consolidated: {totalIncome,totalExpense,netResult,totalOverdue} }
  dashboardComparative:(year) => `/api/v1/dashboard/comparative?year=${year}`,

  // Per-company dashboard
  companySummary:        (id, s, e) => `/api/dashboard/company/${id}/summary?start=${s}&end=${e}`,
  companyMonthlyRevenue: (id, s, e) => `/api/dashboard/company/${id}/monthly-revenue?start=${s}&end=${e}`,
  companyMonthlyExpenses:(id, s, e) => `/api/dashboard/company/${id}/monthly-expenses?start=${s}&end=${e}`,
  companyMonthlyProfit:  (id, s, e) => `/api/dashboard/company/${id}/monthly-profit?start=${s}&end=${e}`,
  companyCategories:     (id, s, e) => `/api/dashboard/company/${id}/categories?start=${s}&end=${e}`,
  companyCashFlow:       (id, s, e) => `/api/dashboard/company/${id}/cash-flow?start=${s}&end=${e}`,

  // Per-company category analytics
  companyCategoryAnalytics: (id, s, e) => `/api/dashboard/company/${id}/categories/summary?start=${s}&end=${e}`,
  companyCategoryRevenue:   (id, s, e) => `/api/dashboard/company/${id}/categories/revenue?start=${s}&end=${e}`,
  companyCategoryExpenses:  (id, s, e) => `/api/dashboard/company/${id}/categories/expenses?start=${s}&end=${e}`,
  companyCategoryProfit:    (id, s, e) => `/api/dashboard/company/${id}/categories/profit?start=${s}&end=${e}`,

  // Per-company cost-center analytics
  companyCostCenterAnalytics: (id, s, e) => `/api/dashboard/company/${id}/cost-centers/summary?start=${s}&end=${e}`,
  companyCostCenterRevenue:   (id, s, e) => `/api/dashboard/company/${id}/cost-centers/revenue?start=${s}&end=${e}`,
  companyCostCenterExpenses:  (id, s, e) => `/api/dashboard/company/${id}/cost-centers/expenses?start=${s}&end=${e}`,

  // Comparison cost-center analytics
  comparisonCostCenterSummary:  (a, b, s, e) => `/api/dashboard/comparison/cost-centers/summary?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonCostCenterRevenue:  (a, b, s, e) => `/api/dashboard/comparison/cost-centers/revenue?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonCostCenterExpenses: (a, b, s, e) => `/api/dashboard/comparison/cost-centers/expenses?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,

  // Comparison category analytics
  comparisonCategorySummary:  (a, b, s, e) => `/api/dashboard/comparison/categories/summary?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonCategoryRevenue:  (a, b, s, e) => `/api/dashboard/comparison/categories/revenue?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonCategoryExpenses: (a, b, s, e) => `/api/dashboard/comparison/categories/expenses?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,

  // Comparison dashboard
  comparisonSummary:       (a, b, s, e) => `/api/dashboard/comparison/summary?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonRevenue:       (a, b, s, e) => `/api/dashboard/comparison/revenue?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonExpenses:      (a, b, s, e) => `/api/dashboard/comparison/expenses?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonMargins:       (a, b, s, e) => `/api/dashboard/comparison/margins?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,
  comparisonMonthlyResults:(a, b, s, e) => `/api/dashboard/comparison/monthly-results?companyA=${a}&companyB=${b}&start=${s}&end=${e}`,

  // Comparison categories & cost-centers (correct paths, with direction param)
  comparisonCategories:    (a, b, dir, s, e) => `/api/dashboard/comparison/categories?companyA=${a}&companyB=${b}&direction=${dir}&start=${s}&end=${e}`,
  comparisonCostCenters:   (a, b, dir, s, e) => `/api/dashboard/comparison/cost-centers?companyA=${a}&companyB=${b}&direction=${dir}&start=${s}&end=${e}`,

  // Consolidated dashboard
  consolidatedSummary:       (s, e) => `/api/dashboard/consolidated/summary?start=${s}&end=${e}`,
  consolidatedMonthlyResults:(s, e) => `/api/dashboard/consolidated/monthly-results?start=${s}&end=${e}`,

  // Financial transactions
  transactions:      (page, size) => `/api/v1/financial/transactions?page=${page || 0}&size=${size || 20}&sort=dueDate,desc`,
  transaction:       (id) => `/api/v1/financial/transactions/${id}`,

  // Financial categories
  categories:        (type, activeOnly) => {
    let q = '/api/v1/financial/categories?activeOnly=' + (activeOnly !== false);
    if (type) q += '&type=' + type;
    return q;
  },

  // Cost centers
  costCenters:       (activeOnly) => `/api/v1/financial/cost-centers?activeOnly=${activeOnly !== false}`,

  // Companies
  companies:         () => '/api/companies',
  company:           (id) => `/api/companies/${id}`,

  // Users
  users:             (page, size) => `/api/users?page=${page || 0}&size=${size || 20}`,
  userMe:            () => '/api/users/me',

  // Conta Azul integration
  contaAzulStatus:   (companyId) => `/api/integrations/conta-azul/status/${companyId}`,
  contaAzulSync:     (companyId) => `/api/integrations/conta-azul/sync/${companyId}`,
  contaAzulSyncAll:  () => '/api/integrations/conta-azul/sync-all',
  contaAzulAuthorize:(companyId) => `/api/integrations/conta-azul/authorize/${companyId}`,

  /** Generic PATCH — returns parsed `data` field from ApiResponse<T>. */
  async patch(path, body) {
    let res;
    try {
      res = await fetch(this.BASE + path, {
        method: 'PATCH',
        headers: this.headers(),
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch (_) {
      throw new Error('Sem conexão com o servidor.');
    }
    if (res.status === 401) { API.logout(); return null; }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Erro HTTP ' + res.status);
    }
    const json = await res.json();
    return json.data;
  },

  /** Generic PUT — returns parsed `data` field from ApiResponse<T>. */
  async put(path, body) {
    let res;
    try {
      res = await fetch(this.BASE + path, {
        method: 'PUT',
        headers: this.headers(),
        body: JSON.stringify(body),
      });
    } catch (_) {
      throw new Error('Sem conexão com o servidor.');
    }
    if (res.status === 401) { API.logout(); return null; }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Erro HTTP ' + res.status);
    }
    const json = await res.json();
    return json.data;
  },
};
