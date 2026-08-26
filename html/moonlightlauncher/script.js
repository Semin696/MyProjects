const REPO_OWNER = 'Semin696';
const REPO_NAME = 'moonlightlauncher';
const FOLDER = 'realise';
const EXE_EXT = /\.(exe|zip|jar|msi)$/i;
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/contents/${FOLDER}`;

const TOKEN_KEY = 'mlv_token';

let state = { user: null };
let downloadsLoaded = false;

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

function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  state.user = user;
  renderAll();
}

async function applySession(token) {
  localStorage.setItem(TOKEN_KEY, token);
  const me = await api('/api/me');
  state.user = me.user;
  renderAll();
  return me.user;
}

function clearSession() {
  const token = localStorage.getItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_KEY);
  state.user = null;
  renderAll();
  if (token) api('/api/logout', { method: 'POST', body: { token } }).catch(() => {});
}

function isOwner() {
  return state.user && state.user.role === 'owner';
}

function hasSub() {
  return state.user && state.user.subscribed;
}

function formatSize(bytes) {
  if (bytes == null) return '';
  const mb = bytes / 1024 / 1024;
  return mb >= 1 ? `${mb.toFixed(1)} МБ` : `${(bytes / 1024).toFixed(0)} КБ`;
}

async function loadDownloads() {
  if (downloadsLoaded) return;
  downloadsLoaded = true;
  try {
    const res = await fetch(`${API_URL}?t=${Date.now()}`);
    if (!res.ok) throw new Error(res.status);
    const files = await res.json();
    if (!Array.isArray(files)) throw new Error('bad response');
    const builds = files.filter(f => f.type === 'file' && EXE_EXT.test(f.name));
    if (builds.length === 0) {
      $('downloadList').classList.add('hidden');
      $('errorState').classList.remove('hidden');
      return;
    }
    $('downloadList').innerHTML = builds.map(file => `
      <div class="download-item">
        <div class="file-info">
          <div class="file-name">${esc(file.name)}</div>
          <div class="file-meta">Размер: ${formatSize(file.size)}</div>
        </div>
        <a class="btn primary" href="${esc(file.download_url)}" download>Скачать</a>
      </div>
    `).join('');
  } catch {
    $('downloadList').classList.add('hidden');
    $('errorState').classList.remove('hidden');
  }
}

function renderAuthArea() {
  const area = $('authArea');
  if (!state.user) {
    area.innerHTML = `<button class="btn ghost small" id="openAuthBtn">Войти</button>`;
    $('openAuthBtn').addEventListener('click', () => openModal('authOverlay'));
    return;
  }
  const badge = hasSub()
    ? `<span class="badge sub">Подписка</span>`
    : `<span class="badge nosub">Без подписки</span>`;
  const ownerBtn = isOwner()
    ? `<button class="btn ghost small" id="ownerPanelBtn">Панель владельца</button>`
    : '';
  area.innerHTML = `
    <span class="badge name link" id="profileBadge" title="Открыть профиль">${esc(state.user.username)}</span>
    ${badge}
    ${ownerBtn}
    <button class="btn ghost small" id="logoutBtn">Выйти</button>
  `;
  $('logoutBtn').addEventListener('click', clearSession);
  $('profileBadge').addEventListener('click', () => openProfile('info'));
  if (isOwner()) {
    $('ownerPanelBtn').addEventListener('click', () => {
      openModal('ownerOverlay');
      refreshCodes();
    });
  }
}

function renderLauncherTab() {
  const subs = $('launcherSubs');
  const gate = $('launcherGate');
  const dl = $('launcherDownloads');
  if (!state.user) return;
  subs.innerHTML = hasSub()
    ? '<span class="badge sub">Тестовая подписка · активна</span>'
    : '<span class="badge nosub">Нет</span>';
  if (!hasSub()) {
    gate.classList.remove('hidden');
    dl.classList.add('hidden');
    return;
  }
  gate.classList.add('hidden');
  dl.classList.remove('hidden');
  loadDownloads();
}

function renderAll() {
  renderAuthArea();
}

async function restoreSession() {
  if (!localStorage.getItem(TOKEN_KEY)) {
    renderAll();
    return;
  }
  try {
    const data = await api('/api/me');
    state.user = data.user;
    renderAll();
  } catch {
    localStorage.removeItem(TOKEN_KEY);
    renderAll();
  }
}

function renderProfile() {
  const u = state.user;
  if (!u) return;
  $('profileAvatar').textContent = (u.username[0] || '?').toUpperCase();
  $('profileName').textContent = u.username;
  let roleText = 'Пользователь';
  if (u.role === 'owner') roleText = 'Владелец';
  else if (u.role === 'tester') roleText = 'Тестер';
  else if (u.subscribed) roleText = 'С подпиской';
  $('profileRole').textContent = roleText;
  $('profileSub').innerHTML = hasSub()
    ? '<span class="ok">Активна</span>'
    : '<span class="err">Нет</span>';
  $('profileDate').textContent = u.createdAt
    ? new Date(u.createdAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })
    : '—';
  $('profileOwnerBtn').classList.toggle('hidden', !isOwner());
}

function switchProfileTab(tab) {
  document.querySelectorAll('[data-profile-tab]').forEach(t => t.classList.toggle('active', t.dataset.profileTab === tab));
  $('profileTabInfo').classList.toggle('hidden', tab !== 'info');
  $('profileTabLauncher').classList.toggle('hidden', tab !== 'launcher');
  $('profileTabSub').classList.toggle('hidden', tab !== 'sub');
}

function openProfile(tab) {
  renderProfile();
  if (tab === 'launcher') renderLauncherTab();
  switchProfileTab(tab);
  openModal('profileOverlay');
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

function requestDownload() {
  if (!state.user) {
    const hint = $('heroHint');
    hint.textContent = 'Скачивание доступно в профиле после входа и подписки.';
    hint.classList.remove('hidden');
    setTimeout(() => hint.classList.add('hidden'), 4000);
    return;
  }
  openProfile('launcher');
}

$('heroDownloadBtn').addEventListener('click', requestDownload);
$('navDownloadLink').addEventListener('click', e => {
  e.preventDefault();
  requestDownload();
});

document.querySelectorAll('[data-profile-tab]').forEach(tab => {
  tab.addEventListener('click', () => {
    switchProfileTab(tab.dataset.profileTab);
    if (tab.dataset.profileTab === 'launcher') renderLauncherTab();
  });
});

$('toSubTabBtn').addEventListener('click', () => switchProfileTab('sub'));

$('subActivateBtn').addEventListener('click', async () => {
  const code = $('subCodeInput').value.trim();
  if (!code) return;
  try {
    const data = await api('/api/activate', { method: 'POST', body: { code } });
    $('subStatus').innerHTML = `<span class="ok">${esc(data.message)}</span>`;
    $('subCodeInput').value = '';
    const me = await api('/api/me');
    state.user = me.user;
    renderAll();
    renderProfile();
    renderLauncherTab();
    switchProfileTab('launcher');
  } catch (err) {
    $('subStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
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
    await applySession(data.token);
    closeModal('authOverlay');
    $('loginForm').reset();
  } catch (err) {
    authStatus(err.message, false);
  }
});

$('registerForm').addEventListener('submit', async e => {
  e.preventDefault();
  const username = $('regUser').value.trim();
  const password = $('regPass').value;
  if (!username || !password) return authStatus('Заполните никнейм и пароль', false);
  try {
    const data = await api('/api/register', { method: 'POST', body: { username, password } });
    await applySession(data.token);
    closeModal('authOverlay');
    $('registerForm').reset();
  } catch (err) {
    authStatus(err.message, false);
  }
});

async function refreshCodes() {
  const list = $('codesList');
  list.innerHTML = `<div class="loading"><div class="spinner"></div><p>Загрузка...</p></div>`;
  try {
    const data = await api('/api/codes');
    if (data.codes.length === 0) {
      list.innerHTML = `<p class="hint">Кодов пока нет.</p>`;
      return;
    }
    list.innerHTML = data.codes.map(c => `
      <div class="code-row">
        <div>
          <div class="code-value">${esc(c.code)}</div>
          <div class="code-meta">${
            c.used_by_name
              ? `Использован: ${esc(c.used_by_name)}`
              : 'Свободен'
          }</div>
        </div>
        ${
          c.used_by_name
            ? ''
            : `<button class="btn ghost small del-code" data-code="${esc(c.code)}">Удалить</button>`
        }
      </div>
    `).join('');
    list.querySelectorAll('.del-code').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await api(`/api/codes?code=${encodeURIComponent(btn.dataset.code)}`, { method: 'DELETE' });
          refreshCodes();
        } catch (err) {
          $('ownerStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
        }
      });
    });
  } catch (err) {
    list.innerHTML = '';
    $('ownerStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
}

$('genCodeBtn').addEventListener('click', async () => {
  try {
    const data = await api('/api/codes', {
      method: 'POST',
      body: { type: $('codeType').value },
    });
    navigator.clipboard?.writeText(data.code).catch(() => {});
    refreshCodes();
  } catch (err) {
    $('ownerStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
});

restoreSession();
