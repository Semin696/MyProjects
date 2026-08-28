const REPO_OWNER = 'Semin696';
const REPO_NAME = 'moonlightlauncher';
const FOLDER = 'realise';
const EXE_EXT = /\.(exe|zip|jar|msi)$/i;
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/contents/${FOLDER}`;
const TOKEN_KEY = 'mlv_token';

let user = null;
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
  const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Ошибка запроса');
  return data;
}

function isOwner() {
  return user && user.role === 'owner';
}

function roleText() {
  if (user.role === 'owner') return 'Владелец';
  if (user.role === 'tester') return 'Тестер';
  return 'Пользователь';
}

function subLabel() {
  return isOwner() ? 'Владельческая подписка · активна' : 'Подписка · активна';
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

function formatSize(bytes) {
  if (bytes == null) return '';
  const mb = bytes / 1024 / 1024;
  return mb >= 1 ? `${mb.toFixed(1)} МБ` : `${(bytes / 1024).toFixed(0)} КБ`;
}

function loadDownloads() {
  if (downloadsLoaded) return;
  downloadsLoaded = true;
  const btn = $('dlBtn');
  fetch(`${API_URL}?t=${Date.now()}`)
    .then(r => {
      if (!r.ok) throw new Error(r.status);
      return r.json();
    })
    .then(files => {
      if (!Array.isArray(files)) throw new Error('bad response');
      const build = files.find(f => f.type === 'file' && EXE_EXT.test(f.name));
      if (!build) {
        btn.textContent = 'Лаунчер пока не загружен';
        return;
      }
      btn.textContent = 'Скачать лаунчер';
      btn.href = build.download_url;
      btn.setAttribute('download', '');
      btn.classList.remove('disabled');
    })
    .catch(() => {
      btn.textContent = 'Лаунчер пока не загружен';
    });
}

function renderProfile() {
  $('pName').textContent = user.username;
  $('pDate').textContent = user.createdAt
    ? new Date(user.createdAt).toLocaleDateString('ru-RU')
    : '—';
  $('pId').textContent = user.id ?? '—';
  $('pRole').textContent = roleText();
  $('pSubStatus').innerHTML = user.subscribed
    ? `<span class="ok">${subLabel()}</span>`
    : '<span class="err">Нет</span>';
  $('launcherSubs').innerHTML = user.subscribed
    ? `<span class="badge sub">${subLabel()}</span>`
    : '<span class="badge nosub">Нет</span>';
  $('subOwnerBlock').classList.toggle('hidden', !isOwner());
  $('pEmail').textContent = user.email || '—';
  resetPassEye();
}

let realPassword = null;
let passVisible = false;

function resetPassEye() {
  realPassword = null;
  passVisible = false;
  $('pPass').textContent = '••••••••••';
  $('passEye').title = 'Показать пароль';
}

$('passEye').addEventListener('click', async () => {
  if (passVisible) {
    passVisible = false;
    $('pPass').textContent = '••••••••••';
    $('passEye').title = 'Показать пароль';
    return;
  }
  if (realPassword == null) {
    try {
      const data = await api('/api/credentials');
      realPassword = data.password;
      if (data.email && !$('pEmail').textContent.includes('@')) {
        $('pEmail').textContent = data.email;
      }
    } catch {
      $('pPass').textContent = '—';
      return;
    }
  }
  passVisible = true;
  $('pPass').textContent = realPassword ?? '—';
  $('passEye').title = 'Скрыть пароль';
});

function switchSec(sec) {
  document.querySelectorAll('.side-link').forEach(l => l.classList.toggle('active', l.dataset.sec === sec));
  $('secAccount').classList.toggle('hidden', sec !== 'account');
  $('secLauncher').classList.toggle('hidden', sec !== 'launcher');
  $('secSub').classList.toggle('hidden', sec !== 'sub');
  if (sec === 'launcher') renderLauncher();
}

function renderLauncher() {
  const btn = $('dlBtn');
  if (!user.subscribed) {
    btn.textContent = 'Нет активной подписки';
    btn.removeAttribute('href');
    btn.removeAttribute('download');
    btn.classList.add('disabled');
    return;
  }
  btn.classList.remove('disabled');
  loadDownloads();
}

async function activate(inputId, statusId) {
  const code = $(inputId).value.trim();
  if (!code) return;
  try {
    const data = await api('/api/activate', { method: 'POST', body: { code } });
    $(statusId).innerHTML = `<span class="ok">${esc(data.message)}</span>`;
    $(inputId).value = '';
    const me = await api('/api/me');
    user = me.user;
    renderAuthArea();
    renderProfile();
  } catch (err) {
    $(statusId).innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
}

$('accActivateBtn').addEventListener('click', () => activate('accCodeInput', 'accStatus'));

$('tabGenKeyBtn').addEventListener('click', async () => {
  try {
    const data = await api('/api/codes', { method: 'POST', body: { type: $('keyType').value } });
    navigator.clipboard?.writeText(data.code).catch(() => {});
    $('tabGenKeyStatus').innerHTML =
      `<span class="ok">Ключ создан: <span class="code-value">${esc(data.code)}</span> — скопирован в буфер</span>`;
  } catch (err) {
    $('tabGenKeyStatus').innerHTML = `<span class="err">${esc(err.message)}</span>`;
  }
});

document.querySelectorAll('.buy-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    $('shopStatus').innerHTML =
      '<span class="err">Покупки временно недоступны. Подписка выдаётся по ключу активации.</span>';
  });
});

document.querySelectorAll('.side-link').forEach(link => {
  link.addEventListener('click', () => {
    history.replaceState(null, '', `#${link.dataset.sec}`);
    switchSec(link.dataset.sec);
  });
});

async function init() {
  const hash = location.hash.slice(1);
  if (!localStorage.getItem(TOKEN_KEY)) {
    $('pageLoading').classList.add('hidden');
    $('guestGate').classList.remove('hidden');
    return;
  }
  try {
    const me = await api('/api/me');
    user = me.user;
  } catch {
    localStorage.removeItem(TOKEN_KEY);
  }
  $('pageLoading').classList.add('hidden');
  if (!user) {
    $('guestGate').classList.remove('hidden');
    return;
  }
  $('profileContent').classList.remove('hidden');
  renderAuthArea();
  renderProfile();
  switchSec(['account', 'launcher', 'sub'].includes(hash) ? hash : 'account');
}

init();
