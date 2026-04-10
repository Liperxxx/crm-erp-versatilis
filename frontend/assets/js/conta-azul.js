/**
 * conta-azul.js — Conta Azul integration status and sync management
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('conta-azul', 'Conta Azul');

  var grid    = document.getElementById('connections-grid');
  var logDiv  = document.getElementById('sync-log');
  var logBody = document.getElementById('sync-log-body');
  var logEntries = [];

  // ── Load companies then check each status ──────────────────────────────
  async function loadConnections() {
    grid.innerHTML = '<div class="td-empty">Carregando…</div>';

    try {
      var companies = await API.get(API.companies());
      companies = companies || [];

      if (companies.length === 0) {
        grid.innerHTML = '<div class="td-empty">Nenhuma empresa cadastrada.</div>';
        return;
      }

      // Fetch status for each company
      var statuses = await Promise.allSettled(
        companies.map(function (c) { return API.get(API.contaAzulStatus(c.id)); })
      );

      grid.innerHTML = companies.map(function (c, i) {
        var st = statuses[i];
        var data = (st.status === 'fulfilled' && st.value) ? st.value : null;
        return buildConnectionCard(c, data);
      }).join('');

      // Bind sync buttons
      grid.querySelectorAll('[data-sync]').forEach(function (btn) {
        btn.addEventListener('click', function () {
          syncCompany(parseInt(btn.dataset.sync), btn);
        });
      });

    } catch (err) {
      grid.innerHTML = '<div class="td-empty">Erro: ' + esc(err.message) + '</div>';
    }
  }

  function buildConnectionCard(company, status) {
    var connected = status && status.connected;
    var expired   = status && status.tokenExpired;

    var statusBadge, statusText;
    if (!status) {
      statusBadge = '<span class="badge badge-neutral">Sem dados</span>';
      statusText = 'Não foi possível obter o status da conexão.';
    } else if (connected && !expired) {
      statusBadge = '<span class="badge badge-success">Conectado</span>';
      statusText = 'Empresa externa: ' + esc(status.externalCompanyName || '—');
    } else if (expired) {
      statusBadge = '<span class="badge badge-warning">Token Expirado</span>';
      statusText = 'O token de acesso expirou. Reautorize a conexão.';
    } else {
      statusBadge = '<span class="badge badge-danger">' + esc(status.status || 'Desconectado') + '</span>';
      statusText = 'Conexão inativa ou revogada.';
    }

    var lastSync = status && status.lastSyncAt ? formatDatetime(status.lastSyncAt) : 'Nunca';

    return '<div class="company-card">'
      + '  <div class="company-card-header">'
      + '    <div class="company-card-icon" style="background:var(--blue)">' + esc(company.name || '?').charAt(0).toUpperCase() + '</div>'
      + '    <div>'
      + '      <div class="company-card-name">' + esc(company.name) + '</div>'
      + '      <div class="company-card-slug">' + statusText + '</div>'
      + '    </div>'
      + '    <div class="company-card-status">' + statusBadge + '</div>'
      + '  </div>'
      + '  <div class="company-card-meta">'
      + '    <span>Última sinc.: ' + lastSync + '</span>'
      + '    <button class="btn btn-primary" data-sync="' + company.id + '">Sincronizar</button>'
      + '  </div>'
      + '</div>';
  }

  // ── Sync ───────────────────────────────────────────────────────────────
  async function syncCompany(companyId, btn) {
    if (btn) { btn.disabled = true; btn.textContent = 'Sincronizando…'; }
    addLog('Iniciando sincronização da empresa #' + companyId + '…');

    try {
      await API.post(API.contaAzulSync(companyId));
      addLog('Sincronização da empresa #' + companyId + ' concluída.', 'success');
    } catch (err) {
      addLog('Erro ao sincronizar empresa #' + companyId + ': ' + err.message, 'error');
    }

    if (btn) { btn.disabled = false; btn.textContent = 'Sincronizar'; }
    loadConnections();
  }

  document.getElementById('btn-sync-all').addEventListener('click', async function () {
    var btn = this;
    btn.disabled = true;
    btn.querySelector('svg') && (btn.innerHTML = 'Sincronizando…');
    addLog('Sincronização geral iniciada…');

    try {
      await API.post(API.contaAzulSyncAll());
      addLog('Sincronização geral concluída.', 'success');
    } catch (err) {
      addLog('Erro na sincronização geral: ' + err.message, 'error');
    }

    btn.disabled = false;
    btn.innerHTML = '<svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.7" viewBox="0 0 24 24"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg> Sincronizar Todas';
    loadConnections();
  });

  // ── Log ────────────────────────────────────────────────────────────────
  function addLog(message, type) {
    logDiv.style.display = 'block';
    var cls = type === 'error' ? 'text-danger' : type === 'success' ? 'text-success' : '';
    var time = new Date().toLocaleTimeString('pt-BR');
    logEntries.push('<div class="sync-log-entry ' + cls + '"><span class="td-muted">' + time + '</span> ' + esc(message) + '</div>');
    logBody.innerHTML = logEntries.join('');
    logBody.scrollTop = logBody.scrollHeight;
  }

  document.getElementById('btn-clear-log').addEventListener('click', function () {
    logEntries = [];
    logBody.innerHTML = '';
    logDiv.style.display = 'none';
  });

  // ── Helpers ────────────────────────────────────────────────────────────
  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function formatDatetime(iso) {
    if (!iso) return '—';
    try {
      var d = new Date(iso);
      return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    } catch (_) { return iso; }
  }

  // ── Init ───────────────────────────────────────────────────────────────
  loadConnections();
})();
