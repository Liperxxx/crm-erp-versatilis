/**
 * consolidado.js — Consolidated view of Bustech + Versatilis group.
 * Depends on: api.js, components.js, charts.js.
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('consolidado', 'Consolidado — Grupo');

  /* ── Helpers ─────────────────────────────────────────────────────────── */
  var _brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });
  function _fmt(v)    { return _brl.format(parseFloat(v) || 0); }
  function _fmtPct(v) { return (parseFloat(v) || 0).toFixed(1) + '%'; }
  function _esc(s)    { return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
  function _set(id, text) { var el = document.getElementById(id); if (el) el.textContent = text; }

  /* ── State ────────────────────────────────────────────────────────────── */
  function _yr() { return new Date().getFullYear(); }
  var state = { start: _yr() + '-01-01', end: _yr() + '-12-31' };

  /* ── Init ─────────────────────────────────────────────────────────────── */
  (function _initFilters() {
    var s = document.getElementById('filter-start');
    var e = document.getElementById('filter-end');
    var b = document.getElementById('btn-apply');
    if (s) { s.value = state.start; s.addEventListener('change', function () { state.start = s.value; }); }
    if (e) { e.value = state.end;   e.addEventListener('change', function () { state.end   = e.value; }); }
    if (b) { b.addEventListener('click', loadData); }
  })();

  loadData();

  /* ── Load ─────────────────────────────────────────────────────────────── */
  function loadData() {
    var start = state.start;
    var end   = state.end;

    Promise.allSettled([
      API.get(API.consolidatedSummary(start, end)),
      API.get(API.consolidatedMonthlyResults(start, end)),
    ]).then(function (r) {
      var summary = r[0];
      var monthly = r[1];

      if (summary.status === 'fulfilled' && summary.value) {
        _renderKpis(summary.value);
        _renderShareBars(summary.value);
        _renderCompanyBreakdown(summary.value);
      }

      if (monthly.status === 'fulfilled' && monthly.value) {
        _renderChart(monthly.value);
        _renderTable(monthly.value);
      } else {
        _set('consol-table', '');
        var tb = document.getElementById('consol-table');
        if (tb) tb.innerHTML = '<tr><td colspan="6" class="td-empty">Sem dados para o período.</td></tr>';
      }
    });
  }

  /* ── KPIs ─────────────────────────────────────────────────────────────── */
  function _renderKpis(d) {
    _set('consol-revenue', _fmt(d.revenueTotal));
    _set('consol-expense', _fmt(d.expensesTotal));
    _set('consol-profit',  _fmt(d.profitTotal));
    _set('consol-margin',  _fmtPct(d.marginTotal));
  }

  /* ── Revenue share bars ───────────────────────────────────────────────── */
  function _renderShareBars(d) {
    var share = d.revenueShareByCompany;
    if (!Array.isArray(share)) return;

    share.forEach(function (item) {
      var pct  = parseFloat(item.sharePercent || 0).toFixed(1) + '%';
      var name = String(item.companyName || '').toLowerCase();
      var isA  = name.indexOf('bustech') >= 0 || item.companyId === 1;
      var suffix = isA ? 'a' : 'b';
      var lbl = document.getElementById('consol-share-' + suffix);
      var bar = document.getElementById('consol-bar-'   + suffix);
      if (lbl) lbl.textContent  = pct;
      if (bar) bar.style.width  = pct;
    });
  }

  /* ── Per-company breakdown ────────────────────────────────────────────── */
  function _renderCompanyBreakdown(d) {
    var share = d.revenueShareByCompany;
    if (!Array.isArray(share)) return;

    share.forEach(function (item) {
      var name   = String(item.companyName || '').toLowerCase();
      var isA    = name.indexOf('bustech') >= 0 || item.companyId === 1;
      var suffix = isA ? 'a' : 'b';
      // revenue is available from share entry; expense/profit not in summary — show what we have
      _set('consol-co-' + suffix + '-revenue', _fmt(item.revenue));
      _set('consol-co-' + suffix + '-share',   parseFloat(item.sharePercent || 0).toFixed(1) + '%');
    });
  }

  /* ── Chart ────────────────────────────────────────────────────────────── */
  function _renderChart(resp) {
    var pts = Array.isArray(resp) ? resp : (resp.points || []);
    if (!pts.length) return;

    var labels = pts.map(function (p) { return p.monthLabel || String(p.month); });
    renderBarChart('consol-chart-monthly', labels, [
      { label: 'Receita',  data: pts.map(function (p) { return parseFloat(p.revenue)  || 0; }), color: COLORS.blue,   fillColor: COLORS.blueLight  },
      { label: 'Despesas', data: pts.map(function (p) { return parseFloat(p.expense)  || 0; }), color: COLORS.orange, fillColor: COLORS.orangeLight },
      { label: 'Lucro',    data: pts.map(function (p) { return parseFloat(p.profit)   || 0; }), color: COLORS.green,  fillColor: COLORS.greenFill  },
    ]);
  }

  /* ── Monthly table ────────────────────────────────────────────────────── */
  function _renderTable(resp) {
    var tbody = document.getElementById('consol-table');
    if (!tbody) return;

    var pts = Array.isArray(resp) ? resp : (resp.points || []);

    if (!pts.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="td-empty">Sem dados para o período.</td></tr>';
      return;
    }

    tbody.innerHTML = pts.map(function (p) {
      var profit = parseFloat(p.profit) || 0;
      // byCompany: [{companyId, companyName, revenue, expense, profit}]
      var byComp = Array.isArray(p.byCompany) ? p.byCompany : [];
      var coA = byComp.find(function(c) { return c.companyId === 1 || String(c.companyName||'').toLowerCase().indexOf('bustech') >= 0; }) || {};
      var coB = byComp.find(function(c) { return c.companyId === 2 || String(c.companyName||'').toLowerCase().indexOf('versatilis') >= 0; }) || {};

      return '<tr>' +
        '<td>' + _esc(p.monthLabel || '') + '</td>' +
        '<td class="td-num">' + _fmt(p.revenue)  + '</td>' +
        '<td class="td-num">' + _fmt(p.expense)  + '</td>' +
        '<td class="td-num' + (profit < 0 ? ' text-danger' : '') + '">' + _fmt(p.profit) + '</td>' +
        '<td class="td-num">' + (coA.revenue != null ? _fmt(coA.revenue)  : '<span class="td-muted">—</span>') + '</td>' +
        '<td class="td-num">' + (coB.revenue != null ? _fmt(coB.revenue)  : '<span class="td-muted">—</span>') + '</td>' +
        '</tr>';
    }).join('');

    // Update breakdown cards with period totals from monthly response
    if (resp.revenueShareByCompany && !Array.isArray(resp)) {
      _renderShareBars(resp);
      _renderCompanyBreakdown(resp);
    }
  }

})();

