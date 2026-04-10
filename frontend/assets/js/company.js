/**
 * company.js — Shared analytics engine for company detail pages (Bustech, Versatilis).
 * Context is read from <body> data attributes:
 *   data-company-id   — numeric company ID (1=Bustech, 2=Versatilis)
 *   data-page-key     — sidebar active key ('bustech' | 'versatilis')
 *   data-page-title   — page heading shown in topbar
 *
 * Depends on: api.js, components.js, charts.js (loaded before this script).
 */
(function () {
  'use strict';

  var companyId = parseInt(document.body.dataset.companyId, 10);
  var pageKey   = document.body.dataset.pageKey   || 'bustech';
  var pageTitle = document.body.dataset.pageTitle || 'Empresa';

  Layout.requireAuth();
  Layout.renderLayout(pageKey, pageTitle);

  /* ── Formatters ─────────────────────────────────────────────────────── */
  var _brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });
  function BRL(v)  { return _brl.format(parseFloat(v) || 0); }
  function PCT(v)  { return (parseFloat(v) || 0).toFixed(1) + '%'; }
  function _n(v)   { return parseFloat(v) || 0; }
  function _esc(s) {
    return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;')
                          .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function _set(id, txt) { var el = document.getElementById(id); if (el) el.textContent = txt; }
  function _trunc(s, max) { s = String(s || ''); return s.length > max ? s.slice(0, max - 1) + '\u2026' : s; }

  /* ── DOM refs ───────────────────────────────────────────────────────── */
  var elStart = document.getElementById('filter-start');
  var elEnd   = document.getElementById('filter-end');
  var elApply = document.getElementById('btn-apply');
  var elMain  = document.getElementById('main-content');

  /* ── Default period: current year ──────────────────────────────────── */
  var _yr = new Date().getFullYear();
  if (elStart) elStart.value = _yr + '-01-01';
  if (elEnd)   elEnd.value   = _yr + '-12-31';

  if (elApply) elApply.addEventListener('click', _load);

  _load();

  /* ── Core load ──────────────────────────────────────────────────────── */
  function _load() {
    var start = elStart ? elStart.value : (_yr + '-01-01');
    var end   = elEnd   ? elEnd.value   : (_yr + '-12-31');
    if (!start || !end) { _showBanner('Selecione o período antes de aplicar.'); return; }
    _clearBanner();
    _resetDisplay();
    if (elApply) elApply.disabled = true;

    Promise.allSettled([
      API.get(API.companySummary(companyId, start, end)),              // [0] DashboardSummaryResponse
      API.get(API.companyMonthlyProfit(companyId, start, end)),        // [1] List<MonthlyProfitPoint>
      API.get(API.companyCategoryAnalytics(companyId, start, end)),    // [2] CategoryAnalyticsResponse
      API.get(API.companyCostCenterAnalytics(companyId, start, end)),  // [3] CostCenterAnalyticsResponse
      API.get(API.companyCashFlow(companyId, start, end)),             // [4] CashFlowResponse
    ]).then(function (r) {
      var allFailed = r.every(function (x) { return x.status === 'rejected'; });
      if (allFailed) {
        _showBanner('Erro ao carregar dados. Verifique a conexão com o servidor.');
        return;
      }
      _renderKpis(        r[0].status === 'fulfilled' ? r[0].value : null);
      _renderCharts(      r[1].status === 'fulfilled' ? r[1].value : null);
      _renderCatSection(  r[2].status === 'fulfilled' ? r[2].value : null);
      _renderCCSection(   r[3].status === 'fulfilled' ? r[3].value : null);
      _renderCashFlow(    r[4].status === 'fulfilled' ? r[4].value : null);
    }).finally(function () {
      if (elApply) elApply.disabled = false;
    });
  }

  /* ── KPIs ───────────────────────────────────────────────────────────── */
  function _renderKpis(d) {
    if (!d) return;
    var profit  = _n(d.profit);
    var pending = parseInt(d.pendingCount)  || 0;
    var overdue = parseInt(d.overdueCount)  || 0;

    _set('kpi-revenue', BRL(d.totalRevenue));
    _set('kpi-expense', BRL(d.totalExpense));
    _set('kpi-profit',  BRL(d.profit));
    _set('kpi-margin',  PCT(d.margin));
    _set('kpi-balance', BRL(d.cashBalance));
    _set('kpi-overdue', BRL(d.overdueAmount));

    var deltaEl = document.getElementById('kpi-delta-profit');
    if (deltaEl) {
      deltaEl.textContent = profit >= 0 ? 'Positivo' : 'Negativo';
      deltaEl.className   = 'kpi-delta ' + (profit >= 0 ? 'positive' : 'negative');
    }
    var pendEl = document.getElementById('kpi-pending-count');
    if (pendEl) {
      pendEl.textContent = pending + ' pendente' + (pending === 1 ? '' : 's') +
                           ' \u00b7 ' + overdue + ' vencido' + (overdue === 1 ? '' : 's');
      pendEl.className   = 'kpi-delta ' + (overdue > 0 ? 'negative' : 'neutral');
    }
  }

  /* ── Monthly charts ─────────────────────────────────────────────────── */
  // Uses MonthlyProfitPoint: { year, month, monthLabel, revenue, expense, profit }
  function _renderCharts(pts) {
    if (!Array.isArray(pts) || !pts.length) return;
    var labels   = pts.map(function (p) { return p.monthLabel || String(p.month); });
    var revData  = pts.map(function (p) { return _n(p.revenue); });
    var expData  = pts.map(function (p) { return _n(p.expense); });
    var profData = pts.map(function (p) { return _n(p.profit);  });

    renderLineChart('chart-revenue', labels, [
      { label: 'Receita',  data: revData,  color: COLORS.blue,   fill: true, fillColor: COLORS.blueLight  },
    ]);
    renderLineChart('chart-expense', labels, [
      { label: 'Despesas', data: expData,  color: COLORS.orange, fill: true, fillColor: COLORS.orangeLight },
    ]);
    renderBarChart('chart-profit', labels, [
      { label: 'Lucro',    data: profData, color: COLORS.green,  fillColor: COLORS.greenFill },
    ]);
  }

  /* ── Category analytics ─────────────────────────────────────────────── */
  function _renderCatSection(data) {
    var countEl   = document.getElementById('cat-count');
    var chartWrap = document.getElementById('cat-chart-wrap');
    var tableEl   = document.getElementById('cat-table');
    if (!tableEl) return;

    var items = data && Array.isArray(data.items) ? data.items : [];

    if (!items.length) {
      if (countEl)   countEl.textContent     = '';
      if (chartWrap) chartWrap.style.display = 'none';
      tableEl.innerHTML = '<p class="td-empty">Nenhuma categoria com movimenta\u00e7\u00e3o no per\u00edodo.</p>';
      return;
    }

    if (countEl) countEl.textContent = items.length + ' categoria' + (items.length === 1 ? '' : 's');
    if (chartWrap) {
      chartWrap.style.height  = Math.max(180, items.length * 44 + 60) + 'px';
      chartWrap.style.display = '';
    }

    renderHorizontalBarChart('cat-chart',
      items.map(function (i) { return _trunc(i.category, 34); }),
      [
        { label: 'Receita',  data: items.map(function (i) { return _n(i.totalRevenue);  }), color: COLORS.blue   },
        { label: 'Despesas', data: items.map(function (i) { return _n(i.totalExpenses); }), color: COLORS.orange },
      ]
    );

    tableEl.innerHTML =
      '<table><thead><tr>' +
      '<th class="num">#</th><th>Categoria</th>' +
      '<th class="num">Receita</th><th class="num">Despesas</th><th class="num">Lucro</th>' +
      '<th class="num">% Rec.</th><th class="num">% Desp.</th>' +
      '</tr></thead><tbody>' +
      items.map(function (it, idx) {
        var lc = _n(it.totalProfit) >= 0 ? 'text-success' : 'text-danger';
        return '<tr>' +
          '<td class="td-num">'              + (idx + 1)                      + '</td>' +
          '<td>'                             + _esc(it.category)              + '</td>' +
          '<td class="td-num text-success">' + BRL(it.totalRevenue)           + '</td>' +
          '<td class="td-num text-danger">'  + BRL(it.totalExpenses)          + '</td>' +
          '<td class="td-num ' + lc + '">'   + BRL(it.totalProfit)            + '</td>' +
          '<td class="td-num">'              + PCT(it.percentualSobreReceita) + '</td>' +
          '<td class="td-num">'              + PCT(it.percentualSobreDespesa) + '</td>' +
          '</tr>';
      }).join('') +
      '</tbody></table>';
  }

  /* ── Cost center analytics ──────────────────────────────────────────── */
  function _renderCCSection(data) {
    var countEl   = document.getElementById('cc-count');
    var chartWrap = document.getElementById('cc-chart-wrap');
    var tableEl   = document.getElementById('cc-table');
    if (!tableEl) return;

    var items = data && Array.isArray(data.items) ? data.items : [];

    if (!items.length) {
      if (countEl)   countEl.textContent     = '';
      if (chartWrap) chartWrap.style.display = 'none';
      tableEl.innerHTML = '<p class="td-empty">Nenhum centro de custo com movimenta\u00e7\u00e3o no per\u00edodo.</p>';
      return;
    }

    if (countEl) countEl.textContent = items.length + ' centro' + (items.length === 1 ? '' : 's');
    if (chartWrap) {
      chartWrap.style.height  = Math.max(180, items.length * 44 + 60) + 'px';
      chartWrap.style.display = '';
    }

    renderHorizontalBarChart('cc-chart',
      items.map(function (i) { return _trunc(i.costCenterName, 34); }),
      [
        { label: 'Receita',  data: items.map(function (i) { return _n(i.totalRevenue);  }), color: COLORS.blue   },
        { label: 'Despesas', data: items.map(function (i) { return _n(i.totalExpenses); }), color: COLORS.orange },
      ]
    );

    tableEl.innerHTML =
      '<table><thead><tr>' +
      '<th class="num">#</th><th>Centro de Custo</th>' +
      '<th class="num">Receita</th><th class="num">Despesas</th><th class="num">Lucro</th>' +
      '<th class="num">% Rec.</th><th class="num">% Desp.</th>' +
      '</tr></thead><tbody>' +
      items.map(function (it, idx) {
        var profit = _n(it.totalRevenue) - _n(it.totalExpenses);
        var lc = profit >= 0 ? 'text-success' : 'text-danger';
        return '<tr>' +
          '<td class="td-num">'              + (idx + 1)                      + '</td>' +
          '<td>'                             + _esc(it.costCenterName)        + '</td>' +
          '<td class="td-num text-success">' + BRL(it.totalRevenue)           + '</td>' +
          '<td class="td-num text-danger">'  + BRL(it.totalExpenses)          + '</td>' +
          '<td class="td-num ' + lc + '">'   + BRL(profit)                    + '</td>' +
          '<td class="td-num">'              + PCT(it.percentualSobreReceita) + '</td>' +
          '<td class="td-num">'              + PCT(it.percentualSobreDespesa) + '</td>' +
          '</tr>';
      }).join('') +
      '</tbody></table>';
  }

  /* ── Cash flow table ────────────────────────────────────────────────── */
  function _renderCashFlow(resp) {
    var tbody = document.getElementById('cashflow-table');
    if (!tbody) return;

    var pts = resp && Array.isArray(resp.points) ? resp.points : [];
    if (!pts.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="td-empty">Sem dados de fluxo para o per\u00edodo.</td></tr>';
      return;
    }

    tbody.innerHTML = pts.map(function (p) {
      var net = _n(p.net);
      var cum = _n(p.cumulativeNet);
      return '<tr>' +
        '<td>'                                                                          + _esc(p.monthLabel || '') + '</td>' +
        '<td class="td-num">'                                                           + BRL(p.income)           + '</td>' +
        '<td class="td-num">'                                                           + BRL(p.expense)          + '</td>' +
        '<td class="td-num ' + (net >= 0 ? 'text-success' : 'text-danger') + '">'      + BRL(p.net)              + '</td>' +
        '<td class="td-num ' + (cum >= 0 ? 'text-success' : 'text-danger') + '">'      + BRL(p.cumulativeNet)    + '</td>' +
        '</tr>';
    }).join('');
  }

  /* ── Display helpers ────────────────────────────────────────────────── */
  function _resetDisplay() {
    ['kpi-revenue', 'kpi-expense', 'kpi-profit', 'kpi-margin', 'kpi-balance', 'kpi-overdue']
      .forEach(function (id) { _set(id, '\u2014'); });
    var tbody = document.getElementById('cashflow-table');
    if (tbody) tbody.innerHTML = '<tr><td colspan="5" class="td-empty">Carregando\u2026</td></tr>';
    var catT = document.getElementById('cat-table');
    if (catT) catT.innerHTML = '<p class="td-empty">Carregando\u2026</p>';
    var ccT  = document.getElementById('cc-table');
    if (ccT)  ccT.innerHTML  = '<p class="td-empty">Carregando\u2026</p>';
  }

  function _showBanner(msg) {
    _clearBanner();
    var b = document.createElement('div');
    b.id = 'co-banner';
    b.setAttribute('role', 'alert');
    b.style.cssText = 'background:var(--s-err-bg,#fff5f5);color:var(--s-err,#c00);' +
      'border:1px solid #f0b4b4;border-radius:var(--r,6px);padding:9px 14px;font-size:12.5px;line-height:1.5;';
    b.textContent = msg;
    var fb = elMain ? elMain.querySelector('.filter-bar') : null;
    if (fb) fb.insertAdjacentElement('afterend', b);
    else if (elMain) elMain.prepend(b);
  }

  function _clearBanner() {
    var el = document.getElementById('co-banner');
    if (el) el.remove();
  }

}());
