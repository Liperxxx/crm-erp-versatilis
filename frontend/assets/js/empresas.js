/**
 * empresas.js — Company listing and detail view
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('empresas', 'Empresas');

  var grid   = document.getElementById('companies-grid');
  var detail = document.getElementById('company-detail');

  // ── Load companies ─────────────────────────────────────────────────────
  async function loadCompanies() {
    grid.innerHTML = '<div class="td-empty">Carregando…</div>';
    detail.style.display = 'none';

    try {
      var companies = await API.get(API.companies());
      companies = companies || [];

      document.getElementById('company-count').textContent = companies.length + ' empresa(s)';

      if (companies.length === 0) {
        grid.innerHTML = '<div class="td-empty">Nenhuma empresa cadastrada.</div>';
        return;
      }

      grid.innerHTML = companies.map(function (c) {
        var statusBadge = c.active
          ? '<span class="badge badge-success">Ativa</span>'
          : '<span class="badge badge-neutral">Inativa</span>';
        return '<div class="company-card" data-id="' + c.id + '">'
          + '  <div class="company-card-header">'
          + '    <div class="company-card-icon">' + esc(c.name || '?').charAt(0).toUpperCase() + '</div>'
          + '    <div>'
          + '      <div class="company-card-name">' + esc(c.name) + '</div>'
          + '      <div class="company-card-slug">' + esc(c.slug || '—') + '</div>'
          + '    </div>'
          + '    <div class="company-card-status">' + statusBadge + '</div>'
          + '  </div>'
          + '  <div class="company-card-meta">'
          + '    <span>ID: ' + c.id + '</span>'
          + '    <span>Criada em: ' + formatDatetime(c.createdAt) + '</span>'
          + '  </div>'
          + '</div>';
      }).join('');

      grid.querySelectorAll('.company-card').forEach(function (card) {
        card.addEventListener('click', function () {
          showDetail(card.dataset.id, companies);
        });
      });
    } catch (err) {
      grid.innerHTML = '<div class="td-empty">Erro: ' + esc(err.message) + '</div>';
    }
  }

  // ── Company detail ─────────────────────────────────────────────────────
  function showDetail(id, companies) {
    var c = companies.find(function (x) { return String(x.id) === String(id); });
    if (!c) return;

    var statusBadge = c.active
      ? '<span class="badge badge-success">Ativa</span>'
      : '<span class="badge badge-neutral">Inativa</span>';

    detail.style.display = 'block';
    detail.innerHTML = ''
      + '<div class="table-card">'
      + '  <div class="table-card-header">'
      + '    <span class="table-card-title">' + esc(c.name) + ' ' + statusBadge + '</span>'
      + '    <button class="btn btn-ghost" id="btn-back-companies">Voltar</button>'
      + '  </div>'
      + '  <table>'
      + '    <tbody>'
      + '      <tr><td class="td-secondary" style="width:140px">ID</td><td>' + c.id + '</td></tr>'
      + '      <tr><td class="td-secondary">Nome</td><td>' + esc(c.name) + '</td></tr>'
      + '      <tr><td class="td-secondary">Slug</td><td>' + esc(c.slug || '—') + '</td></tr>'
      + '      <tr><td class="td-secondary">Status</td><td>' + statusBadge + '</td></tr>'
      + '      <tr><td class="td-secondary">Criada em</td><td>' + formatDatetime(c.createdAt) + '</td></tr>'
      + '      <tr><td class="td-secondary">Atualizada em</td><td>' + formatDatetime(c.updatedAt) + '</td></tr>'
      + '    </tbody>'
      + '  </table>'
      + '</div>';

    grid.style.display = 'none';

    document.getElementById('btn-back-companies').addEventListener('click', function () {
      detail.style.display = 'none';
      grid.style.display = '';
    });
  }

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
  loadCompanies();
})();
