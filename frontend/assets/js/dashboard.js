/**
 * dashboard.js — Overview: consolidated KPIs + company snapshots.
 * Depends on: api.js, components.js, charts.js (loaded before this script).
 *
 * Backend endpoints used:
 *   GET /api/v1/dashboard/comparative?year=Y  → consolidated + all companies (ADMIN)
 *   GET /api/v1/dashboard?year=Y              → current company monthly series
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('dashboard', 'Dashboard');

  /* ── State ────────────────────────────────────────────────────────────── */
  var state = { year: new Date().getFullYear() };

  /* ── Filter init ──────────────────────────────────────────────────────── */
  (function _initFilter() {
    var sel = document.getElementById('filter-year');
    if (sel) {
      var cur = new Date().getFullYear();
      for (var y = cur; y >= cur - 4; y--) {
        var opt = document.createElement('option');
        opt.value = y;
        opt.textContent = y;
        if (y === cur) opt.selected = true;
        sel.appendChild(opt);
      }
      sel.addEventListener('change', function () {
        state.year = parseInt(this.value, 10);
      });
    }
    var btn = document.getElementById('btn-apply');
    if (btn) btn.addEventListener('click', loadDashboard);
  })();

  /* ── Initial load ─────────────────────────────────────────────────────── */
  loadDashboard();

  /* ── Main loader ──────────────────────────────────────────────────────── */
  async function loadDashboard() {
    _setSkeleton(true);
    _clearBanner();

    var year = state.year;

    var [comparResult, myDashResult] = await Promise.allSettled([
      API.get(API.dashboardComparative(year)),
      API.get(API.dashboardCompany(year)),
    ]);

    _setSkeleton(false);

    /* Comparative → consolidated KPIs + company snapshots */
    if (comparResult.status === 'fulfilled' && comparResult.value) {
      _renderConsolidated(comparResult.value.consolidated);
      _renderCompanySnaps(comparResult.value.companies);
    } else {
      var reason = comparResult.reason;
      console.warn('[Dashboard] comparative:', reason && reason.message);
      _showBanner(reason && reason.message
        ? reason.message
        : 'Não foi possível carregar os dados consolidados. Verifique se o backend está acessível.');
      _clearKpis(['consol-revenue', 'consol-expense', 'consol-profit', 'consol-margin']);
      _clearAllSnapshots();
    }

    /* Per-company → monthly series for mini charts */
    if (myDashResult.status === 'fulfilled' && myDashResult.value) {
      _renderCharts(myDashResult.value);
    } else {
      console.warn('[Dashboard] company endpoint:', myDashResult.reason && myDashResult.reason.message);
      _hideChart('bt-chart');
      _hideChart('vt-chart');
    }
  }

  /* ── Renderers ────────────────────────────────────────────────────────── */

  function _renderConsolidated(c) {
    if (!c) {
      _clearKpis(['consol-revenue', 'consol-expense', 'consol-profit', 'consol-margin']);
      return;
    }
    _set('consol-revenue', _fmt(c.totalIncome));
    _set('consol-expense', _fmt(c.totalExpense));
    _set('consol-profit',  _fmt(c.netResult));
    _set('consol-margin',  _fmtMargin(c.totalIncome, c.netResult));
  }

  function _renderCompanySnaps(companies) {
    if (!Array.isArray(companies) || !companies.length) {
      _clearAllSnapshots();
      return;
    }
    _renderSnap(
      companies.find(function (c) { return c.companyId === 1; }),
      'bt'
    );
    _renderSnap(
      companies.find(function (c) { return c.companyId === 2; }),
      'vt'
    );
  }

  function _renderSnap(snap, prefix) {
    var badgeEl = document.getElementById(prefix + '-badge');
    if (!snap) {
      _clearKpis([prefix + '-revenue', prefix + '-expense', prefix + '-profit', prefix + '-margin']);
      if (badgeEl) badgeEl.innerHTML = '<span class="badge badge-neutral">Sem dados</span>';
      return;
    }
    _set(prefix + '-revenue', _fmt(snap.totalIncome));
    _set(prefix + '-expense', _fmt(snap.totalExpense));
    _set(prefix + '-profit',  _fmt(snap.netResult));
    _set(prefix + '-margin',  _fmtMargin(snap.totalIncome, snap.netResult));

    if (badgeEl) {
      var positive = (parseFloat(snap.netResult) || 0) >= 0;
      badgeEl.innerHTML = positive
        ? '<span class="badge badge-success">Positivo</span>'
        : '<span class="badge badge-danger">Negativo</span>';
    }
  }

  function _clearAllSnapshots() {
    _clearKpis([
      'bt-revenue', 'bt-expense', 'bt-profit', 'bt-margin',
      'vt-revenue', 'vt-expense', 'vt-profit', 'vt-margin',
    ]);
    document.querySelectorAll('.company-snap-pill').forEach(function (b) {
      b.innerHTML = '<span class="badge badge-neutral">Sem dados</span>';
    });
  }

  /**
   * Render mini bar charts from the /api/v1/dashboard response.
   * Only the authenticated user's company has monthly data via this endpoint.
   * The matching company gets a chart; the other section is collapsed.
   */
  function _renderCharts(dash) {
    if (!dash) { _hideChart('bt-chart'); _hideChart('vt-chart'); return; }

    var summary = dash.companySummary || {};
    var cid     = summary.companyId;
    var income  = Array.isArray(dash.monthlyIncome) ? dash.monthlyIncome : [];

    /* Which snapshot prefix matches this company's data? */
    var ownPrefix   = (cid === 2) ? 'vt' : 'bt';
    var otherPrefix = (ownPrefix === 'bt') ? 'vt' : 'bt';
    var ownColor    = (ownPrefix === 'bt') ? COLORS.blue   : COLORS.orange;
    var ownFill     = (ownPrefix === 'bt') ? COLORS.blueLight : COLORS.orangeLight;

    if (income.length) {
      _drawMiniBars(ownPrefix + '-chart', income, ownColor, ownFill);
    } else {
      _hideChart(ownPrefix + '-chart');
    }

    /* No monthly data for the other company from this endpoint */
    _hideChart(otherPrefix + '-chart');
  }

  function _drawMiniBars(canvasId, series, color, fillColor) {
    if (typeof renderBarChart !== 'function') return;
    renderBarChart(
      canvasId,
      series.map(function (p) { return p.monthLabel || ('M' + p.month); }),
      [{
        label:     'Receita',
        data:      series.map(function (p) { return parseFloat(p.amount) || 0; }),
        color:     color,
        fillColor: fillColor,
      }]
    );
  }

  function _hideChart(canvasId) {
    var el = document.getElementById(canvasId);
    if (!el) return;
    var wrap = el.closest('.chart-wrap');
    if (wrap) wrap.style.display = 'none';
  }

  /* ── Skeleton / loading states ────────────────────────────────────────── */

  var SKEL_IDS = [
    'consol-revenue', 'consol-expense', 'consol-profit', 'consol-margin',
    'bt-revenue',     'bt-expense',     'bt-profit',     'bt-margin',
    'vt-revenue',     'vt-expense',     'vt-profit',     'vt-margin',
  ];

  function _setSkeleton(on) {
    SKEL_IDS.forEach(function (id) {
      var el = document.getElementById(id);
      if (!el) return;
      if (on) {
        el.textContent = '\u00b7\u00b7\u00b7';  /* ··· */
        el.classList.add('skeleton');
      } else {
        el.classList.remove('skeleton');
      }
    });

    /* Also skeleton/unskeleton badge containers while loading */
    document.querySelectorAll('.company-snap-pill').forEach(function (b) {
      if (on) b.classList.add('skeleton');
      else    b.classList.remove('skeleton');
    });
  }

  /* ── Error banner ─────────────────────────────────────────────────────── */

  function _showBanner(msg) {
    var main = document.getElementById('main-content');
    if (!main) return;
    var banner = document.createElement('div');
    banner.id = 'dash-banner';
    banner.setAttribute('role', 'alert');
    banner.style.cssText = [
      'background:var(--s-err-bg)',
      'color:var(--s-err)',
      'border:1px solid #f0b4b4',
      'border-radius:var(--r)',
      'padding:9px 14px',
      'font-size:12.5px',
      'line-height:1.5',
    ].join(';');
    banner.textContent = msg;
    var fb = main.querySelector('.filter-bar');
    if (fb) fb.insertAdjacentElement('afterend', banner);
    else main.prepend(banner);
  }

  function _clearBanner() {
    var el = document.getElementById('dash-banner');
    if (el) el.remove();
  }

  /* ── Formatters ───────────────────────────────────────────────────────── */

  var _brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });

  function _fmt(v) {
    return _brl.format(parseFloat(v) || 0);
  }

  function _fmtMargin(income, net) {
    var i = parseFloat(income) || 0;
    if (i === 0) return '0,0%';
    return ((parseFloat(net) || 0) / i * 100).toFixed(1).replace('.', ',') + '%';
  }

  function _clearKpis(ids) {
    ids.forEach(function (id) { _set(id, '—'); });
  }

  function _set(id, text) {
    var el = document.getElementById(id);
    if (el) el.textContent = text;
  }

})();
