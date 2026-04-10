/**
 * financeiro.js — Lançamentos, Categorias, Centros de Custo
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('financeiro', 'Financeiro');

  // ── State ──────────────────────────────────────────────────────────────
  var currentPage = 0;
  var pageSize = 20;

  // ── Tabs ───────────────────────────────────────────────────────────────
  document.querySelectorAll('.tab-bar [role="tab"]').forEach(function (tab) {
    tab.addEventListener('click', function () {
      document.querySelectorAll('.tab-bar [role="tab"]').forEach(function (t) {
        t.classList.remove('active');
        t.setAttribute('aria-selected', 'false');
      });
      document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.remove('active'); });
      tab.classList.add('active');
      tab.setAttribute('aria-selected', 'true');
      var panelId = tab.getAttribute('aria-controls');
      var panel = document.getElementById(panelId);
      if (panel) panel.classList.add('active');
      loadActiveTab();
    });
  });

  // Arrow-key navigation within tablist (keyboard accessibility)
  document.querySelector('.tab-bar').addEventListener('keydown', function (e) {
    var tabs = Array.from(document.querySelectorAll('.tab-bar [role="tab"]'));
    var idx  = tabs.indexOf(document.activeElement);
    if (idx === -1) return;
    if (e.key === 'ArrowRight') { e.preventDefault(); tabs[(idx + 1) % tabs.length].focus(); }
    if (e.key === 'ArrowLeft')  { e.preventDefault(); tabs[(idx - 1 + tabs.length) % tabs.length].focus(); }
  });

  // ── Filters ────────────────────────────────────────────────────────────
  document.getElementById('filter-type').addEventListener('change', function () {
    currentPage = 0;
    loadActiveTab();
  });
  document.getElementById('filter-status').addEventListener('change', function () {
    currentPage = 0;
    loadActiveTab();
  });

  function loadActiveTab() {
    var active = document.querySelector('.tab-bar [role="tab"][aria-selected="true"]');
    if (!active) return;
    var panelId = active.getAttribute('aria-controls');
    if (panelId === 'panel-lancamentos')   loadTransactions();
    if (panelId === 'panel-categorias')    loadCategories();
    if (panelId === 'panel-centros-custo') loadCostCenters();
  }

  // ── Transactions ───────────────────────────────────────────────────────
  async function loadTransactions() {
    var tbody = document.getElementById('transactions-table');
    tbody.innerHTML = '<tr><td colspan="7" class="td-empty">Carregando…</td></tr>';

    try {
      var data = await API.get(API.transactions(currentPage, pageSize));
      // data is a PageResponse: { content, page, size, totalElements, totalPages }
      var rows = data.content || data || [];
      var filterType   = document.getElementById('filter-type').value;
      var filterStatus = document.getElementById('filter-status').value;

      if (filterType)   rows = rows.filter(function (r) { return r.type === filterType; });
      if (filterStatus) rows = rows.filter(function (r) { return r.status === filterStatus; });

      document.getElementById('filter-count').textContent = rows.length + ' lançamento(s)';

      if (rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="td-empty">Nenhum lançamento encontrado.</td></tr>';
        renderPagination(data);
        return;
      }

      tbody.innerHTML = rows.map(function (r) {
        var typeBadge = typeLabel(r.type);
        var statusBadge = statusLabel(r.status);
        var amountClass = r.type === 'EXPENSE' ? 'text-danger' : 'text-success';
        return '<tr>'
          + '<td>' + esc(r.description || '—') + '</td>'
          + '<td class="td-secondary">' + esc(r.category || '—') + '</td>'
          + '<td class="td-secondary">' + esc(r.costCenter || '—') + '</td>'
          + '<td>' + typeBadge + '</td>'
          + '<td>' + statusBadge + '</td>'
          + '<td class="td-num ' + amountClass + '">' + fmt(r.amount) + '</td>'
          + '<td class="td-secondary">' + formatDate(r.dueDate) + '</td>'
          + '</tr>';
      }).join('');

      renderPagination(data);
    } catch (err) {
      tbody.innerHTML = '<tr><td colspan="7" class="td-empty">Erro ao carregar: ' + esc(err.message) + '</td></tr>';
    }
  }

  function renderPagination(data) {
    var container = document.getElementById('transactions-pagination');
    if (!container || !data || !data.totalPages) { if (container) container.innerHTML = ''; return; }
    if (data.totalPages <= 1) { container.innerHTML = ''; return; }

    var html = '';
    html += '<button class="btn btn-ghost" ' + (currentPage <= 0 ? 'disabled' : '') + ' data-page="' + (currentPage - 1) + '">Anterior</button>';
    html += '<span class="pagination-info">Página ' + (currentPage + 1) + ' de ' + data.totalPages + '</span>';
    html += '<button class="btn btn-ghost" ' + (currentPage >= data.totalPages - 1 ? 'disabled' : '') + ' data-page="' + (currentPage + 1) + '">Próxima</button>';
    container.innerHTML = html;

    container.querySelectorAll('button[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        currentPage = parseInt(btn.dataset.page);
        loadTransactions();
      });
    });
  }

  // ── Categories ─────────────────────────────────────────────────────────
  async function loadCategories() {
    var tbody = document.getElementById('categories-table');
    tbody.innerHTML = '<tr><td colspan="4" class="td-empty">Carregando…</td></tr>';

    try {
      var filterType = document.getElementById('filter-type').value;
      var type = null;
      if (filterType === 'INCOME')  type = 'INCOME';
      if (filterType === 'EXPENSE') type = 'EXPENSE';

      var rows = await API.get(API.categories(type, false));
      rows = rows || [];

      document.getElementById('filter-count').textContent = rows.length + ' categoria(s)';

      if (rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="td-empty">Nenhuma categoria encontrada.</td></tr>';
        return;
      }

      tbody.innerHTML = rows.map(function (r) {
        var dirLabel = r.type === 'INCOME' ? '<span class="badge badge-success">Receita</span>'
                     : r.type === 'EXPENSE' ? '<span class="badge badge-danger">Despesa</span>'
                     : '<span class="badge badge-neutral">' + esc(r.type || '—') + '</span>';
        var statusBadge = r.active
          ? '<span class="badge badge-success">Ativa</span>'
          : '<span class="badge badge-neutral">Inativa</span>';
        return '<tr>'
          + '<td>' + esc(r.name) + '</td>'
          + '<td>' + dirLabel + '</td>'
          + '<td>' + statusBadge + '</td>'
          + '<td class="td-muted">' + esc(r.externalId || '—') + '</td>'
          + '</tr>';
      }).join('');
    } catch (err) {
      tbody.innerHTML = '<tr><td colspan="4" class="td-empty">Erro: ' + esc(err.message) + '</td></tr>';
    }
  }

  // ── Cost Centers ───────────────────────────────────────────────────────
  async function loadCostCenters() {
    var tbody = document.getElementById('cost-centers-table');
    tbody.innerHTML = '<tr><td colspan="3" class="td-empty">Carregando…</td></tr>';

    try {
      var rows = await API.get(API.costCenters(false));
      rows = rows || [];

      document.getElementById('filter-count').textContent = rows.length + ' centro(s) de custo';

      if (rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="td-empty">Nenhum centro de custo encontrado.</td></tr>';
        return;
      }

      tbody.innerHTML = rows.map(function (r) {
        var statusBadge = r.active
          ? '<span class="badge badge-success">Ativo</span>'
          : '<span class="badge badge-neutral">Inativo</span>';
        return '<tr>'
          + '<td>' + esc(r.name) + '</td>'
          + '<td>' + statusBadge + '</td>'
          + '<td class="td-muted">' + esc(r.externalId || '—') + '</td>'
          + '</tr>';
      }).join('');
    } catch (err) {
      tbody.innerHTML = '<tr><td colspan="3" class="td-empty">Erro: ' + esc(err.message) + '</td></tr>';
    }
  }

  // ── Helpers ────────────────────────────────────────────────────────────
  var _brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });
  function fmt(v) { return _brl.format(parseFloat(v) || 0); }

  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function formatDate(d) {
    if (!d) return '—';
    var parts = String(d).split('-');
    if (parts.length === 3) return parts[2] + '/' + parts[1] + '/' + parts[0];
    return d;
  }

  function typeLabel(type) {
    if (type === 'INCOME')   return '<span class="badge badge-success">Receita</span>';
    if (type === 'EXPENSE')  return '<span class="badge badge-danger">Despesa</span>';
    if (type === 'TRANSFER') return '<span class="badge badge-blue">Transferência</span>';
    return '<span class="badge badge-neutral">' + esc(type || '—') + '</span>';
  }

  function statusLabel(status) {
    if (status === 'PAID')           return '<span class="badge badge-success">Pago</span>';
    if (status === 'PENDING')        return '<span class="badge badge-warning">Pendente</span>';
    if (status === 'OVERDUE')        return '<span class="badge badge-danger">Vencido</span>';
    if (status === 'CANCELLED')      return '<span class="badge badge-neutral">Cancelado</span>';
    if (status === 'PARTIALLY_PAID') return '<span class="badge badge-warning">Parcial</span>';
    return '<span class="badge badge-neutral">' + esc(status || '—') + '</span>';
  }

  // ── Init ───────────────────────────────────────────────────────────────
  loadTransactions();
})();
