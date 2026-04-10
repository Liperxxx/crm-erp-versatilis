/**
 * components.js â€” Shared layout (sidebar + topbar) injected into every authenticated page.
 * Depends on: api.js loaded first.
 *
 * Shell uses CSS Grid: .erp-layout is the grid container.
 * Direct grid children: nav.sidebar | header.topbar | main.page-content
 */

const Layout = {

  activePage: '',

  /**
   * Build and inject sidebar + topbar into .erp-layout.
   * Converts .page-content div to <main> if needed; places sidebar,
   * topbar and main directly as CSS Grid children in .erp-layout.
   *
   * @param {string} activePage â€” key matching a _nav entry
   * @param {string} pageTitle  â€” text shown in the topbar
   */
  renderLayout(activePage, pageTitle) {
    this.activePage = activePage;

    var layout = document.querySelector('.erp-layout');
    if (!layout) return;

    // Find the page-specific content container (may be div or main)
    var content = layout.querySelector('.page-content');
    if (!content) return;

    // Promote to <main> for semantic HTML if not already
    var mainEl;
    if (content.tagName === 'MAIN') {
      mainEl = content;
    } else {
      mainEl = document.createElement('main');
      mainEl.className = 'page-content';
      while (content.firstChild) mainEl.appendChild(content.firstChild);
    }
    mainEl.id = 'main-content';

    var sidebar = this._buildSidebar();
    var topbar  = this._buildTopbar(pageTitle);

    // Skip-to-main link (accessibility: keyboard users can jump over sidebar)
    var skipLink = document.createElement('a');
    skipLink.className = 'skip-link';
    skipLink.href = '#main-content';
    skipLink.textContent = 'Ir para o conteÃºdo principal';
    document.body.insertBefore(skipLink, document.body.firstChild);

    // Place sidebar + topbar + main directly as grid children (no .main-area wrapper)
    layout.innerHTML = '';
    layout.appendChild(sidebar);
    layout.appendChild(topbar);
    layout.appendChild(mainEl);

    // Inject mobile sidebar overlay
    var overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    overlay.id = 'sidebar-overlay';
    document.body.appendChild(overlay);

    this._initUserWidget();
    this._initLogout();
    this._initMobileMenu();
  },

  /* â”€â”€ Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  _nav: [
    { group: 'Principal' },
    { key: 'dashboard',   label: 'Dashboard',       href: '/dashboard.html',
      icon: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>' },

    { group: 'Empresas' },
    { key: 'bustech',     label: 'Bustech',          href: '/empresas/bustech.html',
      icon: '<rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v2"/>' },
    { key: 'versatilis',  label: 'Versatilis',       href: '/empresas/versatilis.html',
      icon: '<path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>' },

    { group: 'RelatÃ³rios' },
    { key: 'comparativo', label: 'Comparativo',      href: '/comparativo.html',
      icon: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>' },
    { key: 'consolidado', label: 'Consolidado',      href: '/consolidado.html',
      icon: '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>' },

    { group: 'Financeiro' },
    { key: 'categorias',  label: 'Categorias',       href: '/categorias.html',
      icon: '<path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/>' },
    { key: 'centros',     label: 'Centros de Custo', href: '/centros-de-custo.html',
      icon: '<path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>' },

    { group: 'Administração' },
    { key: 'integracoes', label: 'Integrações',      href: '/configuracoes/integracoes.html',
      icon: '<path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/>' },
    { key: 'usuarios',    label: 'Usuários',         href: '/usuarios.html',
      icon: '<path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>' },
  ],

  /* â”€â”€ Sidebar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  _buildSidebar() {
    var nav = document.createElement('nav');
    nav.className = 'sidebar';
    nav.setAttribute('aria-label', 'NavegaÃ§Ã£o principal');

    var html = '';

    html += '<div class="sidebar-brand" aria-hidden="true">';
    html += '  <div class="sidebar-logo">ERP</div>';
    html += '  <div class="sidebar-brand-text">';
    html += '    <span class="sidebar-brand-name">Bustech Group</span>';
    html += '    <span class="sidebar-brand-sub">GestÃ£o Financeira</span>';
    html += '  </div>';
    html += '</div>';

    html += '<div class="sidebar-nav">';
    this._nav.forEach(function (item) {
      if (item.group) {
        html += '<div class="sidebar-group-label" aria-hidden="true">' + item.group + '</div>';
      } else {
        var active = item.key === Layout.activePage;
        html += '<a class="sidebar-link' + (active ? ' active' : '') + '" href="' + item.href + '"' + (active ? ' aria-current="page"' : '') + '>';
        html += '  <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" viewBox="0 0 24 24" aria-hidden="true">' + item.icon + '</svg>';
        html += '  ' + item.label;
        html += '</a>';
      }
    });
    html += '</div>';

    html += '<div class="sidebar-footer">';
    html += '  <a class="sidebar-link" href="#" data-action="logout" role="button">';
    html += '    <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" viewBox="0 0 24 24" aria-hidden="true">';
    html += '      <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>';
    html += '      <polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>';
    html += '    </svg>';
    html += '    Sair';
    html += '  </a>';
    html += '</div>';

    nav.innerHTML = html;
    return nav;
  },

  /* â”€â”€ Topbar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  _buildTopbar(pageTitle) {
    var header = document.createElement('header');
    header.className = 'topbar';

    var html = '';
    // Hamburger â€” visible only on mobile via CSS
    html += '<button class="menu-toggle" id="mobile-menu-btn" aria-label="Abrir menu de navegaÃ§Ã£o" aria-expanded="false" aria-controls="site-nav">';
    html += '  <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" aria-hidden="true">';
    html += '    <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>';
    html += '  </svg>';
    html += '</button>';

    html += '<div class="topbar-title"><strong>' + Layout._escHtml(pageTitle) + '</strong></div>';
    html += '<div class="topbar-spacer" aria-hidden="true"></div>';
    html += '<div class="user-widget" id="user-widget">';
    html += '  <div class="user-avatar" id="user-avatar" aria-hidden="true">--</div>';
    html += '  <div class="user-info">';
    html += '    <span class="user-name" id="user-name">Carregandoâ€¦</span>';
    html += '    <span class="user-role" id="user-role"></span>';
    html += '  </div>';
    html += '</div>';

    header.innerHTML = html;
    return header;
  },

  /* â”€â”€ User widget â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  _initUserWidget() {
    try {
      var raw = localStorage.getItem('erp_user');
      if (!raw) return;
      var u = JSON.parse(raw);
      var nameEl   = document.getElementById('user-name');
      var roleEl   = document.getElementById('user-role');
      var avatarEl = document.getElementById('user-avatar');
      if (nameEl)   nameEl.textContent   = u.name  || u.email || 'â€”';
      if (roleEl)   roleEl.textContent   = u.role  || '';
      if (avatarEl) avatarEl.textContent = Layout._initials(u.name || u.email || '');
    } catch (_) {}
  },

  _initLogout() {
    document.querySelectorAll('[data-action="logout"]').forEach(function (el) {
      el.addEventListener('click', function (e) {
        e.preventDefault();
        API.logout();
      });
    });
  },

  /* â”€â”€ Mobile menu â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  _initMobileMenu() {
    var btn     = document.getElementById('mobile-menu-btn');
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.getElementById('sidebar-overlay');
    if (!btn || !sidebar) return;

    function openMenu() {
      sidebar.classList.add('open');
      if (overlay) overlay.classList.add('visible');
      btn.setAttribute('aria-expanded', 'true');
    }
    function closeMenu() {
      sidebar.classList.remove('open');
      if (overlay) overlay.classList.remove('visible');
      btn.setAttribute('aria-expanded', 'false');
    }

    btn.addEventListener('click', function () {
      sidebar.classList.contains('open') ? closeMenu() : openMenu();
    });

    if (overlay) {
      overlay.addEventListener('click', closeMenu);
    }

    // Close on nav link click (useful on mobile)
    sidebar.querySelectorAll('.sidebar-link').forEach(function (link) {
      link.addEventListener('click', function () {
        if (window.innerWidth <= 768) closeMenu();
      });
    });
  },

  /* â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  _initials(name) {
    var parts = String(name || '').trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  },

  _escHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  },

  /** Redirect to login if no JWT token stored. */
  requireAuth() {
    if (!localStorage.getItem('erp_token')) {
      window.location.href = '/login.html';
    }
  },
};
