/**
 * auth.js — Login page logic
 *
 * Depends on: api.js (loaded before this script)
 */

document.addEventListener('DOMContentLoaded', function () {
  // Already logged in → go to dashboard
  if (localStorage.getItem('erp_token')) {
    window.location.href = '/dashboard.html';
    return;
  }

  const form      = document.getElementById('login-form');
  const emailEl   = document.getElementById('email');
  const passEl    = document.getElementById('password');
  const errorEl   = document.getElementById('login-error');
  const submitBtn = document.getElementById('btn-login');

  function showError(msg) {
    errorEl.textContent = msg;
    errorEl.classList.add('visible');
  }

  function clearError() {
    errorEl.textContent = '';
    errorEl.classList.remove('visible');
  }

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    clearError();

    const email    = emailEl.value.trim();
    const password = passEl.value;

    if (!email || !password) {
      showError('Preencha o e-mail e a senha.');
      return;
    }

    submitBtn.disabled    = true;
    submitBtn.textContent = 'Entrando...';

    try {
      let res;
      try {
        res = await fetch(API.BASE + API.LOGIN, {
          method:  'POST',
          headers: { 'Content-Type': 'application/json' },
          body:    JSON.stringify({ email, password }),
        });
      } catch (_) {
        showError('Não foi possível conectar ao servidor. Verifique se o backend está rodando.');
        return;
      }

      const json = await res.json().catch(() => null);

      if (!res.ok) {
        const msg = json?.message || (res.status === 401
          ? 'E-mail ou senha inválidos.'
          : 'Erro ao autenticar. Tente novamente.');
        showError(msg);
        return;
      }

      const authData = json.data;           // ApiResponse<AuthResponse>.data
      const user     = authData.user;       // UserResponse

      localStorage.setItem('erp_token', authData.token);
      localStorage.setItem('erp_user',  JSON.stringify(user));

      window.location.href = '/dashboard.html';

    } finally {
      submitBtn.disabled    = false;
      submitBtn.textContent = 'Entrar';
    }
  });

  // Auto-focus email
  emailEl.focus();
});
