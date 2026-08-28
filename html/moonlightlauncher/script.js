const TOKEN_KEY = 'mlv_token';

let user = null;

const $ = id => document.getElementById(id);

function esc(str) {
  const div = document.createElement('div');
  div.textContent = String(str);
  return div.innerHTML;
}

async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Ошибка запроса');
  return data;
}

function renderAuthArea() {
  const area = $('authArea');
  if (!user) {
    area.innerHTML = `<button class="btn ghost small" id="openAuthBtn">Войти</button>`;
    $('openAuthBtn').addEventListener('click', () => openModal('authOverlay'));
    return;
  }
  const badge = user.subscribed
    ? `<span class="badge sub">${user.role === 'owner' ? 'Владельческая подписка' : 'Подписка'}</span>`
    : `<span class="badge nosub">Без подписки</span>`;
  area.innerHTML = `
    <a class="badge name link" href="profile.html" title="Открыть профиль">${esc(user.username)}</a>
    ${badge}
    <button class="btn ghost small" id="logoutBtn">Выйти</button>
  `;
  $('logoutBtn').addEventListener('click', async () => {
    const token = localStorage.getItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);
    if (token) await api('/api/logout', { method: 'POST', body: { token } }).catch(() => {});
    location.reload();
  });
}

function requestDownload() {
  if (!user) {
    const hint = $('heroHint');
    hint.textContent = 'Скачивание доступно в профиле после входа и подписки.';
    hint.classList.remove('hidden');
    setTimeout(() => hint.classList.add('hidden'), 4000);
    return;
  }
  location.href = 'profile.html#launcher';
}

function openModal(id) {
  $(id).classList.remove('hidden');
}

function closeModal(id) {
  $(id).classList.add('hidden');
}

document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', e => {
    if (e.target === overlay) overlay.classList.add('hidden');
  });
});

document.querySelectorAll('[data-close]').forEach(btn => {
  btn.addEventListener('click', () => closeModal(btn.dataset.close));
});

$('heroDownloadBtn').addEventListener('click', requestDownload);

document.querySelectorAll('.pricing-card-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    if (user) location.href = 'profile.html#sub';
    else {
      const hint = $('heroHint');
      hint.textContent = 'Войдите, чтобы приобрести подписку.';
      hint.classList.remove('hidden');
      setTimeout(() => hint.classList.add('hidden'), 4000);
    }
  });
});

document.querySelectorAll('[data-auth-tab]').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('[data-auth-tab]').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    $('loginForm').classList.toggle('hidden', tab.dataset.authTab !== 'login');
    $('registerForm').classList.toggle('hidden', tab.dataset.authTab !== 'register');
    $('authStatus').innerHTML = '';
  });
});

function authStatus(msg, ok) {
  $('authStatus').innerHTML = `<span class="${ok ? 'ok' : 'err'}">${esc(msg)}</span>`;
}

$('loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  const username = $('loginUser').value.trim();
  const password = $('loginPass').value;
  if (!username || !password) return authStatus('Заполните все поля', false);
  try {
    const data = await api('/api/login', { method: 'POST', body: { username, password } });
    localStorage.setItem(TOKEN_KEY, data.token);
    const me = await api('/api/me');
    user = me.user;
    renderAuthArea();
    closeModal('authOverlay');
    $('loginForm').reset();
  } catch (err) {
    authStatus(err.message, false);
  }
});

$('registerForm').addEventListener('submit', async e => {
  e.preventDefault();
  const username = $('regUser').value.trim();
  const email = $('regEmail').value.trim();
  const password = $('regPass').value;
  if (!username || !email || !password) return authStatus('Заполните все поля', false);
  try {
    const data = await api('/api/register', { method: 'POST', body: { username, email, password } });
    localStorage.setItem(TOKEN_KEY, data.token);
    const me = await api('/api/me');
    user = me.user;
    renderAuthArea();
    closeModal('authOverlay');
    $('registerForm').reset();
  } catch (err) {
    authStatus(err.message, false);
  }
});

async function init() {
  if (localStorage.getItem(TOKEN_KEY)) {
    try {
      const me = await api('/api/me');
      user = me.user;
    } catch {
      localStorage.removeItem(TOKEN_KEY);
    }
  }
  renderAuthArea();
}

init();
