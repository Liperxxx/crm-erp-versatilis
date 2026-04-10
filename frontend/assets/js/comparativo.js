/**
 * comparativo.js — Comparison between Bustech (id=1) and Versatilis (id=2).
 * Depends on: api.js, components.js, charts.js.
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('comparativo', 'Comparativo — Bustech × Versatilis');

  /* ── Helpers ─────────────────────────────────────────────────────────── */
  var _brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });
  function _fmt(v)    { return _brl.format(parseFloat(v) || 0); }
  function _fmtPct(v) { return (parseFloat(v) || 0).toFixed(1) + '%'; }
  function _diffCls(v) { var n = parseFloat(v); return n > 0 ? 'diff-pos' : n < 0 ? 'diff-neg' : ''; }
  function _signPct(v) { if (v == null) return '—'; var n = parseFloat(v); return (n > 0 ? '+' : '') + n.toFixed(1) + '%'; }
  function _esc(s)    { return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
  function _set(id, text) { var el = document.getElementById(id); if (el) el.textContent = text; }

  /* ── State ────────────────────────────────────────────────────────────── */
  var _catDir = 'EXPENSE';
  var _ccDir  = 'EXPENSE';
  var _catData = {};
  var _ccData  = {};

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

  (function _initDirButtons() {
    document.querySelectorAll('[id^="btn-cat-"]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        _catDir = btn.getAttribute('data-dir');
        document.getElementById('btn-cat-income').className  = _catDir === 'INCOME'  ? 'btn btn-primary' : 'btn btn-ghost';
        document.getElementById('btn-cat-expense').className = _catDir === 'EXPENSE' ? 'btn btn-primary' : 'btn btn-ghost';
        _renderCatTable(_catData[_catDir]);
      });
    });
    document.querySelectorAll('[id^="btn-cc-"]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        _ccDir = btn.getAttribute('data-dir');
        document.getElementById('btn-cc-income').className  = _ccDir === 'INCOME'  ? 'btn btn-primary' : 'btn btn-ghost';
        document.getElementById('btn-cc-expense').className = _ccDir === 'EXPENSE' ? 'btn btn-primary' : 'btn btn-ghost';
        _renderCCTable(_ccData[_ccDir]);
      });
    });
  })();

  loadData();

  /* ── Load ─────────────────────────────────────────────────────────────── */
  function loadData() {
    var start = state.start;
    var end   = state.end;

    _resetDisplays();

    Promise.allSettled([
      API.get(API.comparisonSummary(1, 2, start, end)),           // [0]
      API.get(API.comparisonMonthlyResults(1, 2, start, end)),    // [1]
      API.get(API.comparisonCategories(1, 2, 'INCOME',  start, end)), // [2]
      API.get(API.comparisonCategories(1, 2, 'EXPENSE', start, end)), // [3]
      API.get(API.comparisonCostCenters(1, 2, 'INCOME',  start, end)),// [4]
      API.get(API.comparisonCostCenters(1, 2, 'EXPENSE', start, end)),// [5]
    ]).then(function (r) {
      if (r[0].status === 'fulfilled' && r[0].value) _renderSummary(r[0].value);
      if (r[1].status === 'fulfilled' && r[1].value) {
        _renderCharts(r[1].value);
        _renderMonthlyTable(r[1].value);
      }
      _catData['INCOME']  = r[2].status === 'fulfilled' ? r[2].value : null;
      _catData['EXPENSE'] = r[3].status === 'fulfilled' ? r[3].value : null;
      _ccData['INCOME']   = r[4].status === 'fulfilled' ? r[4].value : null;
      _ccData['EXPENSE']  = r[5].status === 'fulfilled' ? r[5].value : null;
      _renderCatTable(_catData[_catDir]);
      _renderCCTable(_ccData[_ccDir]);
    });
  }

  /* ── Reset ────────────────────────────────────────────────────────────── */
  function _resetDisplays() {
    ['comp-a-revenue','comp-a-expense','comp-a-profit','comp-a-margin',
     'comp-b-revenue','comp-b-expense','comp-b-profit','comp-b-margin'].forEach(function (id) { _set(id, '—'); });
    var hide = ['comp-a-leader','comp-b-leader'];
    hide.forEach(function (id) { var el = document.getElementById(id); if (el) el.style.display = 'none'; });
    document.getElementById('comp-table').innerHTML     = '<tr><td colspan="7" class="td-empty">Carregando…</td></tr>';
    document.getElementById('comp-cat-tbody').innerHTML = '<tr><td colspan="5" class="td-empty">Carregando…</td></tr>';
    document.getElementById('comp-cc-tbody').innerHTML  = '<tr><td colspan="5" class="td-empty">Carregando…</td></tr>';
  }

  /* ── Summary KPIs ─────────────────────────────────────────────────────── */
  function _renderSummary(d) {
    var a = d.companyA || {};
    var b = d.companyB || {};

    _set('comp-a-name', a.companyName || 'Empresa A');
    _set('comp-b-name', b.companyName || 'Empresa B');

    _set('comp-a-revenue', _fmt(a.revenue));
    _set('comp-a-expense', _fmt(a.expense));
    _set('comp-a-profit',  _fmt(a.profit));
    _set('comp-a-margin',  _fmtPct(a.margin));
    _set('comp-b-revenue', _fmt(b.revenue));
    _set('comp-b-expense', _fmt(b.expense));
    _set('comp-b-profit',  _fmt(b.profit));
    _set('comp-b-margin',  _fmtPct(b.margin));

    // Leader badges — show on whichever company leads revenue
    var aName = String(a.companyName || '');
    var leaderRev = String(d.higherRevenue || '');
    if (leaderRev) {
      var elA = document.getElementById('comp-a-leader');
      var elB = document.getElementById('comp-b-leader');
      if (elA) elA.style.display = leaderRev === aName ? '' : 'none';
      if (elB) elB.style.display = leaderRev !== aName ? '' : 'none';
    }
  }

  /* ── Charts ───────────────────────────────────────────────────────────── */
  function _renderCharts(resp) {
    var pts = Array.isArray(resp) ? resp : (resp.points || []);
    if (!pts.length) return;

    var labels = pts.map(function (p) { return p.monthLabel || String(p.month); });

    renderBarChart('comp-chart-revenue', labels, [
      { label: 'Bustech',    data: pts.map(function (p) { return parseFloat(p.revenueA) || 0; }), color: COLORS.blue,   fillColor: COLORS.blueLight   },
      { label: 'Versatilis', data: pts.map(function (p) { return parseFloat(p.revenueB) || 0; }), color: COLORS.orange, fillColor: COLORS.orangeLight },
    ]);
    renderLineChart('comp-chart-profit', labels, [
      { label: 'Bustech',    data: pts.map(function (p) { return parseFloat(p.profitA) || 0; }), color: COLORS.blue,   fill: false },
      { label: 'Versatilis', data: pts.map(function (p) { return parseFloat(p.profitB) || 0; }), color: COLORS.orange, fill: false },
    ]);
  }

  /* ── Category table ───────────────────────────────────────────────────── */
  function _renderCatTable(data) {
    var tbody = document.getElementById('comp-cat-tbody');
    var tfoot = document.getElementById('comp-cat-tfoot');
    var thA   = document.getElementById('comp-cat-th-a');
    var thB   = document.getElementById('comp-cat-th-b');

    if (!data || !data.items || !data.items.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="td-empty">Sem dados para o período selecionado.</td></tr>';
      tfoot.innerHTML = '';
      return;
    }

    if (thA) thA.textContent = data.companyA || 'Empresa A';
    if (thB) thB.textContent = data.companyB || 'Empresa B';

    tbody.innerHTML = data.items.map(function (item) {
      var diff = parseFloat(item.amountA || 0) - parseFloat(item.amountB || 0);
      return '<tr>' +
        '<td>' + _esc(item.categoryName) + '</td>' +
        '<td class="td-num">' + _fmt(item.amountA) + '</td>' +
        '<td class="td-num">' + _fmt(item.amountB) + '</td>' +
        '<td class="td-num">' + _fmt(diff) + '</td>' +
        '<td class="td-num ' + _diffCls(item.diffPct) + '">' + _signPct(item.diffPct) + '</td>' +
        '</tr>';
    }).join('');

    tfoot.innerHTML = '<tr style="font-weight:600;background:#f7f9fc">' +
      '<td>Total</td>' +
      '<td class="td-num">' + _fmt(data.totalA) + '</td>' +
      '<td class="td-num">' + _fmt(data.totalB) + '</td>' +
      '<td class="td-num">' + _fmt(parseFloat(data.totalA || 0) - parseFloat(data.totalB || 0)) + '</td>' +
      '<td class="td-num ' + _diffCls(data.diffPct) + '">' + _signPct(data.diffPct) + '</td>' +
      '</tr>';
  }

  /* ── Cost-center table ────────────────────────────────────────────────── */
  function _renderCCTable(data) {
    var tbody = document.getElementById('comp-cc-tbody');
    var tfoot = document.getElementById('comp-cc-tfoot');
    var thA   = document.getElementById('comp-cc-th-a');
    var thB   = document.getElementById('comp-cc-th-b');

    if (!data || !data.items || !data.items.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="td-empty">Sem dados para o período selecionado.</td></tr>';
      tfoot.innerHTML = '';
      return;
    }

    if (thA) thA.textContent = data.companyA || 'Empresa A';
    if (thB) thB.textContent = data.companyB || 'Empresa B';

    tbody.innerHTML = data.items.map(function (item) {
      var diff = parseFloat(item.amountA || 0) - parseFloat(item.amountB || 0);
      return '<tr>' +
        '<td>' + _esc(item.costCenterName) + '</td>' +
        '<td class="td-num">' + _fmt(item.amountA) + '</td>' +
        '<td class="td-num">' + _fmt(item.amountB) + '</td>' +
        '<td class="td-num">' + _fmt(diff) + '</td>' +
        '<td class="td-num ' + _diffCls(item.diffPct) + '">' + _signPct(item.diffPct) + '</td>' +
        '</tr>';
    }).join('');

    tfoot.innerHTML = '<tr style="font-weight:600;background:#f7f9fc">' +
      '<td>Total</td>' +
      '<td class="td-num">' + _fmt(data.totalA) + '</td>' +
      '<td class="td-num">' + _fmt(data.totalB) + '</td>' +
      '<td class="td-num">' + _fmt(parseFloat(data.totalA || 0) - parseFloat(data.totalB || 0)) + '</td>' +
      '<td class="td-num ' + _diffCls(data.diffPct) + '">' + _signPct(data.diffPct) + '</td>' +
      '</tr>';
  }

  /* ── Monthly results table ────────────────────────────────────────────── */
  function _renderMonthlyTable(resp) {
    var tbody = document.getElementById('comp-table');
    var pts   = Array.isArray(resp) ? resp : (resp.points || []);

    if (!pts.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="td-empty">Sem dados para o período.</td></tr>';
      return;
    }

    tbody.innerHTML = pts.map(function (p) {
      var pA = parseFloat(p.profitA || 0);
      var pB = parseFloat(p.profitB || 0);
      return '<tr>' +
        '<td>' + _esc(p.monthLabel || '') + '</td>' +
        '<td class="td-num">' + _fmt(p.revenueA) + '</td>' +
        '<td class="td-num">' + _fmt(p.revenueB) + '</td>' +
        '<td class="td-num">' + _fmt(p.expenseA) + '</td>' +
        '<td class="td-num">' + _fmt(p.expenseB) + '</td>' +
        '<td class="td-num' + (pA < 0 ? ' text-danger' : '') + '">' + _fmt(p.profitA) + '</td>' +
        '<td class="td-num' + (pB < 0 ? ' text-danger' : '') + '">' + _fmt(p.profitB) + '</td>' +
        '</tr>';
    }).join('');
  }

})();

