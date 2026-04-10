/**
 * usuarios.js — User listing
 */
(function () {
  'use strict';

  Layout.requireAuth();
  Layout.renderLayout('usuarios', 'Usuários');

  var currentPage = 0;
  var pageSize = 20;

  document.getElementById('filter-role').addEventListener('change', function () {
    currentPage = 0;
    loadUsers();
  });

  // ── Load users ─────────────────────────────────────────────────────────
  async function loadUsers() {
    var tbody = document.getElementById('users-table');
    tbody.innerHTML = '<tr><td colspan="6" class="td-empty">Carregando…</td></tr>';

    try {
      var data = await API.get(API.users(currentPage, pageSize));
      var rows = data.content || data || [];
      var filterRole = document.getElementById('filter-role').value;

      if (filterRole) {
        rows = rows.filter(function (r) { return r.role === filterRole; });
      }

      document.getElementById('user-count').textContent = rows.length + ' usuário(s)';

      if (rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="td-empty">Nenhum usuário encontrado.</td></tr>';
        renderPagination(data);
        return;
      }

      tbody.innerHTML = rows.map(function (u) {
        var roleBadge = roleLabel(u.role);
        var statusBadge = u.active
          ? '<span class="badge badge-success">Ativo</span>'
          : '<span class="badge badge-neutral">Inativo</span>';
        return '<tr>'
          + '<td>' + esc(u.name || '—') + '</td>'
          + '<td class="td-secondary">' + esc(u.email) + '</td>'
          + '<td>' + roleBadge + '</td>'
          + '<td class="td-secondary">' + esc(u.companyName || '—') + '</td>'
          + '<td>' + statusBadge + '</td>'
          + '<td class="td-secondary">' + formatDatetime(u.createdAt) + '</td>'
          + '</tr>';
      }).join('');

      renderPagination(data);
    } catch (err) {
      tbody.innerHTML = '<tr><td colspan="6" class="td-empty">Erro: ' + esc(err.message) + '</td></tr>';
    }
  }

  function renderPagination(data) {
    var container = document.getElementById('users-pagination');
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
        loadUsers();
      });
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

  function roleLabel(role) {
    if (role === 'ADMIN')    return '<span class="badge badge-blue">Admin</span>';
    if (role === 'MANAGER')  return '<span class="badge badge-warning">Gerente</span>';
    if (role === 'OPERATOR') return '<span class="badge badge-success">Operador</span>';
    if (role === 'VIEWER')   return '<span class="badge badge-neutral">Visualizador</span>';
    return '<span class="badge badge-neutral">' + esc(role || '—') + '</span>';
  }

  // ── Init ───────────────────────────────────────────────────────────────
  loadUsers();
})();
