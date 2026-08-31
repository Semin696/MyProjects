// ===== MoonLight Visual — API-клиент =====
// Обёртка над бэкендом Express + MySQL

const API_BASE = (typeof API_URL !== 'undefined' && API_URL) || '';
const TOKEN_KEY = 'moonlight_token';
const SESSION_KEY = 'moonlight_session';

// --- Токен сессии ---
function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}
function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

// --- Текущий пользователь (для UI, без секретных данных) ---
function getSession() {
  const raw = sessionStorage.getItem(SESSION_KEY);
  return raw ? JSON.parse(raw) : null;
}
function setSession(user) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify({
    name: user.name,
    email: user.email
  }));
}
function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

// --- Совместимость с предыдущим кодом (getUsers возвращает Promise) ---
async function getUsers() {
  const me = getSession();
  return me ? [me] : [];
}

// --- Базовый запрос к API ---
async function api(path, options = {}) {
  const headers = Object.assign(
    { 'Content-Type': 'application/json' },
    options.headers || {}
  );
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(API_BASE + path, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  let data = null;
  const text = await res.text();
  try { data = text ? JSON.parse(text) : {}; } catch (e) { data = { detail: text }; }

  if (!res.ok) {
    const err = new Error((data && data.error) || 'Ошибка запроса');
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

// --- Загрузка/проверка текущего пользователя по токену ---
async function fetchCurrentUser() {
  const token = getToken();
  if (!token) {
    clearSession();
    return null;
  }
  try {
    const data = await api('/api/auth/me');
    setSession(data.user);
    return data.user;
  } catch (e) {
    if (e.status === 401) {
      setToken(null);
      clearSession();
      return null;
    }
    throw e;
  }
}
