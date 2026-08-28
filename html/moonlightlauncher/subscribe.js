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
  const res = await fetch(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Ошибка запроса');
  return data;
}

function isOwner() {
  return user && user.role === 'owner';
}

function renderAuthArea() {
  const area = $('authArea');
  if (!user) {
    area.innerHTML = `<a class="btn ghost small" href="index.html">Войти</a>`;
    return;
  }
  area.innerHTML = `
    <span class="badge name">${esc(user.username)}</span>
    <button class="btn ghost small" id="logoutBtn">Выйти</button>
  `;
  $('logoutBtn').addEventListener('click', async () => {
    const token = localStorage.getItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);
    if (token) await api('/api/logout', { method: 'POST', body: { token } }).catch(() => {});
    location.reload();
  });
}

function renderPage() {
  const guest = $('guestGate');
  const page = $('subPage');
  if (!user) {
    guest.classList.remove('hidden');
    page.classList.add('hidden');
    return;
  }
  guest.classList.add('hidden');
  page.classList.remove('hidden');
  $('subsBadge').innerHTML = user.subscribed
    ? `<span class="badge sub">${user.role === 'owner' ? 'Владельческая подписка · активна' : 'Подписка · активна'}</span>`
    : '<span class="badge nosub">Нет</span>';
  $('subOwnerBlock').classList.toggle('hidden', !isOwner());
}

async function init() {
  if (localStorage.getItem(TOKEN_KEY)) {
    try {
      const data = await api('/api/me');
      user = data.user;
    } catch {
      localStorage.removeItem(TOKEN_KEY);
    }
  }
  renderAuthArea();
  renderPage();
}

$('subActivateBtn').addEventListener('click', async () => {
  const code = $('subCodeInput').value.trim();
  if (!code) return;
  try {
    const data = await api('/api/activate', { method: 'POST', body: { code } });
    $('subStatus').innerHTML = `<span class="ok">${esc(data.message)}</span>`;
    $('subCodeInput').value = '';
    const me = await api('/api/me');
    user = me.user;
    renderAuthArea();
    renderPage();
  } catch (err) {
    $('subStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
});

$('tabGenKeyBtn').addEventListener('click', async () => {
  try {
    const data = await api('/api/codes', {
      method: 'POST',
      body: { type: $('keyType').value },
    });
    navigator.clipboard?.writeText(data.code).catch(() => {});
    $('tabGenKeyStatus').innerHTML =
      `<span class="ok">Ключ создан: <span class="code-value">${esc(data.code)}</span> — скопирован в буфер</span>`;
  } catch (err) {
    $('tabGenKeyStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
});

init();
