/**
 * charts.js — Shared Chart.js helpers used by all pages with charts.
 * Load AFTER Chart.js CDN and BEFORE page-specific scripts.
 */

const COLORS = {
  blue:        '#1a56db',
  blueLight:   'rgba(26,86,219,.10)',
  orange:      '#b45309',
  orangeLight: 'rgba(180,83,9,.10)',
  green:       '#166534',
  greenFill:   'rgba(22,101,52,.10)',
  red:         '#991b1b',
  redFill:     'rgba(153,27,27,.10)',
  gray:        '#7b8ea5',
  grayLight:   'rgba(123,142,165,.08)',
  border:      '#e8ecf3',
};

if (typeof Chart !== 'undefined') {
  Chart.defaults.font.family   = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif";
  Chart.defaults.font.size     = 11.5;
  Chart.defaults.color         = '#7b8ea5';
  Chart.defaults.plugins.legend.labels.boxWidth   = 10;
  Chart.defaults.plugins.legend.labels.padding    = 14;
  Chart.defaults.plugins.tooltip.padding          = 10;
  Chart.defaults.plugins.tooltip.cornerRadius     = 4;
  Chart.defaults.plugins.tooltip.titleFont.weight = '600';
  Chart.defaults.animation.duration               = 300;
}

var _chartInstances = {};

function _destroyChart(id) {
  if (_chartInstances[id]) { _chartInstances[id].destroy(); delete _chartInstances[id]; }
}

function _fmtK(v) {
  if (Math.abs(v) >= 1e6) return 'R$' + (v / 1e6).toFixed(1) + 'M';
  if (Math.abs(v) >= 1e3) return 'R$' + (v / 1e3).toFixed(0) + 'k';
  return 'R$' + v;
}

var _brlFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 });

/**
 * Grouped bar chart.
 * @param {string} canvasId
 * @param {string[]} labels
 * @param {{ label: string, data: number[], color: string, fillColor?: string }[]} datasets
 */
function renderBarChart(canvasId, labels, datasets) {
  var canvas = document.getElementById(canvasId);
  if (!canvas || typeof Chart === 'undefined') return;
  _destroyChart(canvasId);
  _chartInstances[canvasId] = new Chart(canvas.getContext('2d'), {
    type: 'bar',
    data: {
      labels: labels,
      datasets: datasets.map(function (d) {
        return {
          label:           d.label,
          data:            d.data,
          backgroundColor: d.fillColor || d.color,
          borderColor:     d.color,
          borderWidth:     1.5,
          borderRadius:    3,
          borderSkipped:   false,
        };
      }),
    },
    options: {
      responsive:          true,
      maintainAspectRatio: false,
      interaction:         { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'top', align: 'end' },
        tooltip: { callbacks: { label: function (ctx) { return ' ' + ctx.dataset.label + ': ' + _brlFmt.format(ctx.raw); } } },
      },
      scales: {
        x: { grid: { color: COLORS.border }, ticks: { maxRotation: 0 } },
        y: { grid: { color: COLORS.border }, ticks: { callback: _fmtK } },
      },
    },
  });
}

/**
 * Line chart with optional fill.
 * @param {string} canvasId
 * @param {string[]} labels
 * @param {{ label: string, data: number[], color: string, fill?: boolean, fillColor?: string }[]} datasets
 */
function renderLineChart(canvasId, labels, datasets) {
  var canvas = document.getElementById(canvasId);
  if (!canvas || typeof Chart === 'undefined') return;
  _destroyChart(canvasId);
  _chartInstances[canvasId] = new Chart(canvas.getContext('2d'), {
    type: 'line',
    data: {
      labels: labels,
      datasets: datasets.map(function (d) {
        return {
          label:           d.label,
          data:            d.data,
          borderColor:     d.color,
          backgroundColor: d.fill ? (d.fillColor || d.color.replace(')', ', 0.08)').replace('rgb', 'rgba')) : 'transparent',
          borderWidth:     2,
          pointRadius:     3,
          pointHoverRadius:5,
          tension:         0.3,
          fill:            !!d.fill,
        };
      }),
    },
    options: {
      responsive:          true,
      maintainAspectRatio: false,
      interaction:         { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'top', align: 'end' },
        tooltip: { callbacks: { label: function (ctx) { return ' ' + ctx.dataset.label + ': ' + _brlFmt.format(ctx.raw); } } },
      },
      scales: {
        x: { grid: { color: COLORS.border } },
        y: { grid: { color: COLORS.border }, ticks: { callback: _fmtK } },
      },
    },
  });
}

/**
 * Horizontal grouped bar chart — ideal for per-category / per-cost-center breakdowns.
 * @param {string}   canvasId
 * @param {string[]} labels    Y-axis labels (e.g. category names)
 * @param {{ label: string, data: number[], color: string }[]} datasets
 */
function renderHorizontalBarChart(canvasId, labels, datasets) {
  var canvas = document.getElementById(canvasId);
  if (!canvas || typeof Chart === 'undefined') return;
  _destroyChart(canvasId);
  _chartInstances[canvasId] = new Chart(canvas.getContext('2d'), {
    type: 'bar',
    data: {
      labels: labels,
      datasets: datasets.map(function (d) {
        return {
          label:              d.label,
          data:               d.data,
          backgroundColor:    d.color + 'cc',
          borderColor:        d.color,
          borderWidth:        1.5,
          borderRadius:       2,
          borderSkipped:      false,
          barPercentage:      0.65,
          categoryPercentage: 0.85,
        };
      }),
    },
    options: {
      indexAxis:           'y',
      responsive:          true,
      maintainAspectRatio: false,
      interaction:         { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'top', align: 'end' },
        tooltip: {
          callbacks: {
            label: function (ctx) {
              return ' ' + ctx.dataset.label + ': ' + _brlFmt.format(ctx.raw);
            },
          },
        },
      },
      scales: {
        x: {
          grid:  { color: COLORS.border },
          ticks: { callback: _fmtK, maxTicksLimit: 5 },
        },
        y: {
          grid: { display: false },
        },
      },
    },
  });
}
