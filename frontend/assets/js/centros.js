/**
 * centros.js — Cost center analytics dashboard.
 * Modes: single company (Bustech or Versatilis) or comparative (both).
 * Depends on: api.js, components.js, charts.js, Chart.js CDN.
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('centros', 'Centros de Custo');

  /* ── Formatters ───────────────────────────────────────────────────────── */
  var _brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });
  function BRL(n) { return _brl.format(+(n) || 0); }
  function PCT(n) { return (+(n) || 0).toFixed(1) + '%'; }
  function _n(v)  { return +(v) || 0; }
  function _esc(s) {
    return String(s || '')
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function _trunc(s, max) {
    s = String(s || '');
    return s.length > max ? s.slice(0, max - 1) + '\u2026' : s;
  }

  /* ── DOM refs ─────────────────────────────────────────────────────────── */
  var elStart      = document.getElementById('filter-start');
  var elEnd        = document.getElementById('filter-end');
  var elCompany    = document.getElementById('filter-company');
  var elMetrica    = document.getElementById('filter-metrica');
  var elMSep       = document.getElementById('metrica-sep');
  var elApply      = document.getElementById('btn-apply');
  var elKpiRow     = document.getElementById('kpi-row');
  var elChartSec   = document.getElementById('chart-section');
  var elChartCont  = document.getElementById('chart-container');
  var elChartTitle = document.getElementById('chart-title');
  var elTable      = document.getElementById('table-container');
  var elCCCount    = document.getElementById('cc-count');
  var elMain       = document.getElementById('main-content');

  /* ── Default period: current month ───────────────────────────────────── */
  (function () {
    var t  = new Date();
    var y  = t.getFullYear();
    var m  = String(t.getMonth() + 1).padStart(2, '0');
    var d  = String(t.getDate()).padStart(2, '0');
    elStart.value = y + '-' + m + '-01';
    elEnd.value   = y + '-' + m + '-' + d;
  }());

  /* ── Empresa toggle ───────────────────────────────────────────────────── */
  elCompany.addEventListener('change', function () {
    var isComp = elCompany.value === 'comp';
    elMetrica.style.display = isComp ? '' : 'none';
    elMSep.style.display    = isComp ? '' : 'none';
  });

  /* ── Apply ────────────────────────────────────────────────────────────── */
  elApply.addEventListener('click', _load);
  _load();

  /* ── Core load ────────────────────────────────────────────────────────── */
  function _load() {
    var start   = elStart.value;
    var end     = elEnd.value;
    var empresa = elCompany.value;
    var metrica = elMetrica.value;

    if (!start || !end) {
      _showBanner('Selecione o período completo antes de aplicar.');
      return;
    }

    _clearBanner();
    _showSkeleton();
    elApply.disabled = true;

    var p;
    if (empresa === 'comp') {
      var url;
      if (metrica === 'revenue')       url = API.comparisonCostCenterRevenue(1, 2, start, end);
      else if (metrica === 'expenses') url = API.comparisonCostCenterExpenses(1, 2, start, end);
      else                             url = API.comparisonCostCenterSummary(1, 2, start, end);
      p = API.get(url).then(function (data) { _renderComp(data, metrica); });
    } else {
      var id   = Number(empresa);
      var name = id === 1 ? 'Bustech' : 'Versatilis';
      p = API.get(API.companyCostCenterAnalytics(id, start, end))
            .then(function (data) { _renderSingle(data, name); });
    }

    p.catch(function (err) {
      _showBanner('Erro ao carregar dados: ' + (err && err.message ? err.message : 'tente novamente.'));
      elKpiRow.innerHTML       = '';
      elChartSec.style.display = 'none';
      elTable.innerHTML        = '';
      elCCCount.textContent    = '';
    }).finally(function () {
      elApply.disabled = false;
    });
  }

  /* ── Render: single company ───────────────────────────────────────────── */
  function _renderSingle(data, name) {
    if (!data) { _showEmpty(); return; }

    var grandRev  = _n(data.grandTotalRevenue);
    var grandExp  = _n(data.grandTotalExpenses);
    var grandProf = grandRev - grandExp;

    elKpiRow.innerHTML =
      _kpiCard('Receita Total \u00b7 ' + name, BRL(grandRev),  'text-success') +
      _kpiCard('Despesas \u00b7 ' + name,      BRL(grandExp),  'text-danger') +
      _kpiCard('Lucro L\u00edquido',           BRL(grandProf), grandProf >= 0 ? 'text-success' : 'text-danger');

    var items = data.items || [];
    elCCCount.textContent = items.length + ' centro' + (items.length === 1 ? '' : 's');

    if (!items.length) { _showEmpty(); return; }

    /* Chart */
    var labels = items.map(function (i) { return _trunc(i.costCenterName, 34); });
    elChartCont.style.height = Math.max(220, items.length * 44 + 70) + 'px';
    elChartTitle.textContent = 'Receita e Despesas por Centro de Custo \u2014 ' + name;
    elChartSec.style.display = '';
    renderHorizontalBarChart('cc-chart', labels, [
      { label: 'Receita',  data: items.map(function (i) { return _n(i.totalRevenue);  }), color: COLORS.blue   },
      { label: 'Despesas', data: items.map(function (i) { return _n(i.totalExpenses); }), color: COLORS.orange },
    ]);

    /* Table */
    var rows = items.map(function (it, idx) {
      var profit = _n(it.totalRevenue) - _n(it.totalExpenses);
      var lc     = profit >= 0 ? 'text-success' : 'text-danger';
      return '<tr>' +
        '<td class="td-num">'              + (idx + 1)                       + '</td>' +
        '<td>'                             + _esc(it.costCenterName)         + '</td>' +
        '<td class="td-num text-success">' + BRL(it.totalRevenue)            + '</td>' +
        '<td class="td-num text-danger">'  + BRL(it.totalExpenses)           + '</td>' +
        '<td class="td-num ' + lc + '">'   + BRL(profit)                     + '</td>' +
        '<td class="td-num">'              + PCT(it.percentualSobreReceita)  + '</td>' +
        '<td class="td-num">'              + PCT(it.percentualSobreDespesa)  + '</td>' +
        '</tr>';
    }).join('');

    elTable.innerHTML =
      '<table>' +
      '<thead><tr>' +
      '<th class="num">#</th>' +
      '<th>Centro de Custo</th>' +
      '<th class="num">Receita</th>' +
      '<th class="num">Despesas</th>' +
      '<th class="num">Lucro</th>' +
      '<th class="num">% Receita</th>' +
      '<th class="num">% Despesa</th>' +
      '</tr></thead>' +
      '<tbody>' + rows + '</tbody>' +
      '</table>';
  }

  /* ── Render: comparative ──────────────────────────────────────────────── */
  function _renderComp(data, metrica) {
    if (!data) { _showEmpty(); return; }

    var label     = _metricLabel(data.metric, metrica);
    var diffClass = _n(data.difference) >= 0 ? 'text-success' : 'text-danger';
    var compA     = data.companyA || 'Bustech';
    var compB     = data.companyB || 'Versatilis';

    elKpiRow.innerHTML =
      _kpiCard(compA + ' \u00b7 ' + label, BRL(data.totalA)) +
      _kpiCard(compB + ' \u00b7 ' + label, BRL(data.totalB)) +
      _kpiCard('Diferen\u00e7a (A \u2212 B)', BRL(data.difference),      diffClass) +
      _kpiCard('Varia\u00e7\u00e3o %',        PCT(data.differencePercent), diffClass);

    var items = data.items || [];
    elCCCount.textContent = items.length + ' centro' + (items.length === 1 ? '' : 's');

    if (!items.length) { _showEmpty(); return; }

    /* Chart */
    var labels = items.map(function (i) { return _trunc(i.costCenterName, 34); });
    elChartCont.style.height = Math.max(220, items.length * 44 + 70) + 'px';
    elChartTitle.textContent = label + ' por Centro de Custo \u2014 ' + compA + ' vs ' + compB;
    elChartSec.style.display = '';

    /* Summary mode shows both revenue and expenses per CC using companyAValue as revenue for A
       and companyBValue as revenue for B; for clarity render only the combined series */
    renderHorizontalBarChart('cc-chart', labels, [
      { label: compA, data: items.map(function (i) { return _n(i.companyAValue); }), color: COLORS.blue   },
      { label: compB, data: items.map(function (i) { return _n(i.companyBValue); }), color: COLORS.orange },
    ]);

    /* Table */
    var rows = items.map(function (it, idx) {
      var dc = _n(it.difference) >= 0 ? 'text-success' : 'text-danger';
      return '<tr>' +
        '<td class="td-num">'            + (idx + 1)                 + '</td>' +
        '<td>'                           + _esc(it.costCenterName)   + '</td>' +
        '<td class="td-num">'            + BRL(it.companyAValue)     + '</td>' +
        '<td class="td-num">'            + BRL(it.companyBValue)     + '</td>' +
        '<td class="td-num ' + dc + '"> ' + BRL(it.difference)        + '</td>' +
        '<td class="td-num ' + dc + '">' + PCT(it.differencePercent) + '</td>' +
        '</tr>';
    }).join('');

    elTable.innerHTML =
      '<table>' +
      '<thead><tr>' +
      '<th class="num">#</th>' +
      '<th>Centro de Custo</th>' +
      '<th class="num">' + _esc(compA) + '</th>' +
      '<th class="num">' + _esc(compB) + '</th>' +
      '<th class="num">Diferen\u00e7a</th>' +
      '<th class="num">Dif %</th>' +
      '</tr></thead>' +
      '<tbody>' + rows + '</tbody>' +
      '</table>';
  }

  /* ── Helpers ──────────────────────────────────────────────────────────── */
  function _metricLabel(apiMetric, uiMetric) {
    if (apiMetric === 'REVENUE') return 'Receita';
    if (apiMetric === 'EXPENSE') return 'Despesas';
    /* summary returns REVENUE metric field; use UI selection for label */
    if (uiMetric === 'revenue')  return 'Receita';
    if (uiMetric === 'expenses') return 'Despesas';
    return 'Receita + Despesas';
  }

  function _kpiCard(label, value, cls) {
    return '<div class="kpi-card">' +
      '<div class="kpi-label">' + _esc(label) + '</div>' +
      '<div class="kpi-value' + (cls ? ' ' + cls : '') + '">' + value + '</div>' +
      '</div>';
  }

  function _showSkeleton() {
    elKpiRow.innerHTML =
      '<div class="kpi-card skeleton" style="height:72px"></div>' +
      '<div class="kpi-card skeleton" style="height:72px"></div>' +
      '<div class="kpi-card skeleton" style="height:72px"></div>';
    elChartSec.style.display = 'none';
    elTable.innerHTML        = '<p class="td-empty">Carregando\u2026</p>';
    elCCCount.textContent    = '';
  }

  function _showEmpty() {
    elChartSec.style.display = 'none';
    elTable.innerHTML        = '<p class="td-empty">Nenhum centro de custo com movimenta\u00e7\u00e3o no per\u00edodo.</p>';
    elCCCount.textContent    = '';
  }

  function _showBanner(msg) {
    _clearBanner();
    var b = document.createElement('div');
    b.id = 'cc-banner';
    b.setAttribute('role', 'alert');
    b.style.cssText = [
      'background:var(--s-err-bg)',
      'color:var(--s-err)',
      'border:1px solid #f0b4b4',
      'border-radius:var(--r)',
      'padding:9px 14px',
      'font-size:12.5px',
      'line-height:1.5',
    ].join(';');
    b.textContent = msg;
    var fb = elMain ? elMain.querySelector('.filter-bar') : null;
    if (fb) fb.insertAdjacentElement('afterend', b);
    else if (elMain) elMain.prepend(b);
  }

  function _clearBanner() {
    var el = document.getElementById('cc-banner');
    if (el) el.remove();
  }

}());
