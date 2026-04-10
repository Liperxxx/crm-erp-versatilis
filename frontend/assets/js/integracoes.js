/**
 * integracoes.js — Conta Azul integration status + sync controls.
 * Depends on: api.js, components.js.
 *
 * ContaAzulConnectionStatusResponse:
 *   companyId, status (ACTIVE|EXPIRED|REVOKED|ERROR), connected, tokenExpired,
 *   externalCompanyName, lastSyncAt, expiresAt
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('integracoes', 'Integrações');

  var COMPANIES = [
    { id: 1, name: 'Bustech',    key: 'bustech'    },
    { id: 2, name: 'Versatilis', key: 'versatilis' },
  ];

  /* ── Init ─────────────────────────────────────────────────────────────── */
  var btnRefresh = document.getElementById('btn-refresh-log');
  if (btnRefresh) btnRefresh.addEventListener('click', loadAll);

  loadAll();

  /* ── Load ─────────────────────────────────────────────────────────────── */
  async function loadAll() {
    var grid = document.getElementById('integration-grid');
    if (grid) grid.innerHTML = '<div class="table-card" style="grid-column:1/-1"><p class="td-empty" style="padding:32px;text-align:center">Carregando…</p></div>';

    var results = await Promise.allSettled(
      COMPANIES.map(function (c) {
        return API.get(API.contaAzulStatus(c.id)).then(function (r) {
          return { company: c, status: r };
        });
      })
    );

    _renderCards(results);
  }

  /* ── Render integration cards ─────────────────────────────────────────── */
  function _renderCards(results) {
    var grid = document.getElementById('integration-grid');
    if (!grid) return;

    var html = results.map(function (result) {
      if (result.status !== 'fulfilled') {
        var c = result.reason && result.reason.company ? result.reason.company : { name: '?' };
        return _card({ company: { name: c.name }, statusData: null, error: true });
      }
      return _card(result.value);
    }).join('');

    grid.innerHTML = html || '<p class="td-empty">Nenhuma integração disponível.</p>';

    /* Bind sync buttons */
    results.forEach(function (result) {
      if (result.status !== 'fulfilled') return;
      var companyId = result.value.company.id;
      var btn = document.getElementById('btn-sync-' + companyId);
      if (btn) {
        btn.addEventListener('click', function () { _triggerSync(companyId, btn); });
      }
    });
  }

  function _card(payload) {
    var company    = payload.company;
    var s          = payload.status || {};
    var hasError   = payload.error  || false;

    var connected  = !hasError && s.connected;
    var statusText = hasError ? 'Erro' : _statusLabel(s.status);
    var dotClass   = hasError ? 'dot-err' : (connected ? 'dot-ok' : 'dot-off');

    var metaLines  = [];
    if (s.externalCompanyName) metaLines.push('Empresa: ' + _esc(s.externalCompanyName));
    if (s.lastSyncAt)          metaLines.push('Última sync: ' + _fmtDate(s.lastSyncAt));
    if (s.expiresAt)           metaLines.push('Expira em: ' + _fmtDate(s.expiresAt));

    return '<div class="integration-card">' +
      '<div class="integration-card-header">' +
        '<span class="integration-card-name">' + _esc(company.name) + '</span>' +
        '<span class="integration-status"><span class="integration-status-dot ' + dotClass + '"></span><span class="integration-status-text">' + statusText + '</span></span>' +
      '</div>' +
      (metaLines.length ? '<div class="integration-meta">' + metaLines.map(_esc).join('<br>') + '</div>' : '') +
      '<div class="integration-actions">' +
        '<button id="btn-sync-' + company.id + '" class="btn btn-sm btn-primary" ' + (connected ? '' : 'disabled title="Integração não conectada"') + '>' +
          '<svg width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" aria-hidden="true"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>' +
          ' Sincronizar' +
        '</button>' +
      '</div>' +
    '</div>';
  }

  /* ── Trigger sync ─────────────────────────────────────────────────────── */
  async function _triggerSync(companyId, btn) {
    if (!btn) return;
    btn.disabled = true;
    btn.textContent = 'Sincronizando…';
    try {
      await API.post(API.contaAzulSync(companyId), {});
      btn.textContent = '✓ Iniciado';
      setTimeout(function () { loadAll(); }, 2000);
    } catch (e) {
      btn.textContent = 'Erro';
      btn.disabled = false;
      setTimeout(function () { btn.textContent = 'Sincronizar'; }, 3000);
    }
  }

  /* ── Helpers ─────────────────────────────────────────────────────────── */
  var _STATUS_LABEL = { ACTIVE: 'Ativo', EXPIRED: 'Expirado', REVOKED: 'Revogado', ERROR: 'Erro' };
  function _statusLabel(s) { return _STATUS_LABEL[s] || (s || 'Desconectado'); }

  function _fmtDate(v) {
    if (!v) return '';
    try {
      return new Date(v).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch (e) { return String(v); }
  }

  function _esc(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }

})();
