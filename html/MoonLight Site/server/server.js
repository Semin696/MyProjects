// ============================================================
// MoonLight Visual — Node.js + Express + MySQL бэкенд
// ============================================================
const express = require('express');
const cors = require('cors');
const path = require('path');
const bcrypt = require('bcryptjs');
const { initDb, query, startKeepAlive } = require('./db');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// --- Мидлвары ---
app.use(cors());
app.use(express.json());

// Раздаём статические файлы сайта (все html/css/js в корне проекта)
const publicDir = path.dirname(__dirname);

// Блокируем доступ к служебным файлам (server/, node_modules/, .env)
app.use((req, res, next) => {
  const url = (req.path || '').toLowerCase();
  const blocked = [
    '/server', '/node_modules', '/package.json', '/package-lock.json',
    '/.env', '/server.js', '/db.js', '/db.sql', '/secret'
  ];
  if (blocked.some(b => url.includes(b))) {
    return res.status(404).end();
  }
  next();
});

app.use(express.static(publicDir));

// ============================================================
// Утилиты
// ============================================================

// Генерация случайного токена сессии
function generateToken() {
  return require('crypto').randomBytes(32).toString('hex');
}

// Уровни и их ранг для сравнения
const TIER_RANK = { free: 0, premium: 1, pro: 2 };

// Нормализация email
function normEmail(email) {
  return String(email || '').trim().toLowerCase();
}

// Получить активную подписку пользователя (из активированных кодов)
async function getActiveSubscription(userEmail) {
  const [rows] = await query(
    'SELECT * FROM redeemed_codes WHERE user_email = ? AND expires_at > NOW()',
    [userEmail]
  );
  let best = null;
  rows.forEach(r => {
    if (!best || TIER_RANK[r.tier] > TIER_RANK[best.tier]) {
      best = {
        code: r.code,
        tier: r.tier,
        activatedAt: r.activated_at,
        expiresAt: r.expires_at
      };
    }
  });
  return best;
}

// Проверить, владелец ли пользователь
async function isOwner(userEmail) {
  const [rows] = await query('SELECT * FROM owners WHERE email = ?', [userEmail]);
  return rows.length > 0;
}

// ============================================================
// API: /api/auth — регистрация, вход, выход
// ============================================================

// Регистрация
app.post('/api/auth/register', async (req, res) => {
  try {
    const name = String(req.body.name || '').trim();
    let email = normEmail(req.body.email);
    const password = String(req.body.password || '');

    if (name.length < 2) {
      return res.status(400).json({ error: 'Имя должно содержать минимум 2 символа' });
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ error: 'Введите корректный email' });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: 'Пароль должен быть минимум 6 символов' });
    }

    const [existing] = await query('SELECT id FROM users WHERE email = ?', [email]);
    if (existing.length > 0) {
      return res.status(409).json({ error: 'Пользователь с этим email уже существует' });
    }

    const now = new Date();
    const passwordHash = bcrypt.hashSync(password, 10);
    const [result] = await query(
      'INSERT INTO users (name, email, password, created_at) VALUES (?, ?, ?, ?)',
      [name, email, passwordHash, now]
    );

    const token = generateToken();
    await query(
      'INSERT INTO sessions (token, user_email, created_at) VALUES (?, ?, ?)',
      [token, email, now]
    );

    res.status(201).json({
      token,
      user: { name, email, registeredAt: now.toISOString() }
    });
  } catch (err) {
    console.error('register error:', err.message);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Вход
app.post('/api/auth/login', async (req, res) => {
  try {
    const email = normEmail(req.body.email);
    const password = String(req.body.password || '');

    const [users] = await query('SELECT * FROM users WHERE email = ?', [email]);
    if (users.length === 0) {
      return res.status(404).json({ error: 'Пользователь не найден' });
    }

    const user = users[0];
    if (!bcrypt.compareSync(password, user.password)) {
      return res.status(401).json({ error: 'Неверный пароль' });
    }

    const token = generateToken();
    await query(
      'INSERT INTO sessions (token, user_email, created_at) VALUES (?, ?, ?)',
      [token, email, new Date()]
    );

    res.json({
      token,
      user: { name: user.name, email: user.email, registeredAt: user.created_at }
    });
  } catch (err) {
    console.error('login error:', err.message);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Выход (удаляем сессию)
app.post('/api/auth/logout', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  if (token) {
    await query('DELETE FROM sessions WHERE token = ?', [token]);
  }
  res.json({ success: true });
});

// Проверка токена / получение текущего пользователя
app.get('/api/auth/me', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  if (!token) {
    return res.status(401).json({ error: 'Не авторизован' });
  }
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Сессия не найдена' });
    }
    const email = sessions[0].user_email;
    const [users] = await query('SELECT * FROM users WHERE email = ?', [email]);
    if (users.length === 0) {
      return res.status(401).json({ error: 'Пользователь не найден' });
    }
    const user = users[0];
    const activeSub = await getActiveSubscription(user.email);
    const owner = await isOwner(user.email);
    res.json({
      user: {
        name: user.name,
        email: user.email,
        registeredAt: user.created_at,
        isOwner: owner,
        subscription: activeSub
      }
    });
  } catch (err) {
    console.error('me error:', err.message);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// ============================================================
// API: /api/user — профиль пользователя
// ============================================================

// Получить профиль (полные данные подписки, историю)
app.get('/api/user/profile', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    const [users] = await query('SELECT * FROM users WHERE email = ?', [email]);
    const user = users[0];

    const [redeemed] = await query(
      'SELECT * FROM redeemed_codes WHERE user_email = ? ORDER BY activated_at DESC',
      [email]
    );

    const history = redeemed.map(r => ({
      code: r.code,
      tier: r.tier,
      activatedAt: r.activated_at,
      expiresAt: r.expires_at
    }));

    const activeSub = await getActiveSubscription(email);
    const owner = await isOwner(email);

    res.json({
      user: {
        name: user.name,
        email: user.email,
        registeredAt: user.created_at,
        isOwner: owner
      },
      subscription: activeSub,
      history
    });
  } catch (err) {
    console.error('profile error:', err.message);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Изменить имя
app.put('/api/user/name', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  const newName = String(req.body.name || '').trim();
  if (newName.length < 2) {
    return res.status(400).json({ error: 'Имя должно содержать минимум 2 символа' });
  }
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    await query('UPDATE users SET name = ? WHERE email = ?', [newName, email]);
    res.json({ success: true, name: newName });
  } catch (err) {
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Сменить пароль
app.put('/api/user/password', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  const oldPass = String(req.body.oldPassword || '');
  const newPass = String(req.body.newPassword || '');
  if (newPass.length < 6) {
    return res.status(400).json({ error: 'Пароль должен быть минимум 6 символов' });
  }
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    const [users] = await query('SELECT * FROM users WHERE email = ?', [email]);
    const user = users[0];

    if (!bcrypt.compareSync(oldPass, user.password)) {
      return res.status(401).json({ error: 'Неверный текущий пароль' });
    }
    if (bcrypt.compareSync(newPass, user.password)) {
      return res.status(400).json({ error: 'Новый пароль не должен совпадать со старым' });
    }

    const hash = bcrypt.hashSync(newPass, 10);
    await query('UPDATE users SET password = ? WHERE email = ?', [hash, email]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Удалить аккаунт
app.delete('/api/user', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  const password = String(req.body.password || '');
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    const [users] = await query('SELECT * FROM users WHERE email = ?', [email]);
    const user = users[0];
    if (!bcrypt.compareSync(password, user.password)) {
      return res.status(401).json({ error: 'Неверный пароль' });
    }
    // Каскадные FK удалят сессии, активированные коды и ownership
    await query('DELETE FROM users WHERE email = ?', [email]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// ============================================================
// API: /api/codes — промокоды
// ============================================================

// Получить все доступные коды (для владельца)
app.get('/api/codes', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    if (!(await isOwner(email))) {
      return res.status(403).json({ error: 'Недостаточно прав' });
    }
    const [codes] = await query(
      'SELECT code, tier, duration, note, created_by, created_at FROM codes ORDER BY created_at DESC'
    );
    res.json({ codes });
  } catch (err) {
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Создать код (только владелец)
app.post('/api/codes', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    if (!(await isOwner(email))) {
      return res.status(403).json({ error: 'Недостаточно прав для создания кодов' });
    }

    const tier = String(req.body.tier || 'premium');
    const duration = parseInt(req.body.duration, 10) || 30;
    const note = String(req.body.note || '').trim() || 'Создан владельцем';

    // Генерация уникального кода
    let codeStr = '';
    do {
      const prefix = tier === 'pro' ? 'PRO' : 'PREM';
      const random = Math.random().toString(36).substring(2, 8).toUpperCase();
      codeStr = prefix + '-' + random;
      const [exists] = await query('SELECT id FROM codes WHERE code = ?', [codeStr]);
      if (exists.length === 0) break;
    } while (true);

    await query(
      'INSERT INTO codes (code, tier, duration, note, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)',
      [codeStr, tier, duration, note, email, new Date()]
    );

    res.status(201).json({ success: true, code: codeStr });
  } catch (err) {
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Удалить код (только владелец)
app.delete('/api/codes/:code', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  const codeStr = String(req.params.code || '').toUpperCase();
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;
    if (!(await isOwner(email))) {
      return res.status(403).json({ error: 'Недостаточно прав' });
    }
    const [result] = await query('DELETE FROM codes WHERE code = ?', [codeStr]);
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Код не найден' });
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// ============================================================
// API: /api/redeem — активация кодов
// ============================================================

// Активировать код
app.post('/api/redeem', async (req, res) => {
  const token = String(req.headers.authorization || '').replace('Bearer ', '');
  const codeInput = String(req.body.code || '').trim().toUpperCase();
  try {
    const [sessions] = await query('SELECT * FROM sessions WHERE token = ?', [token]);
    if (sessions.length === 0) {
      return res.status(401).json({ error: 'Не авторизован' });
    }
    const email = sessions[0].user_email;

    // Код владельца
    const OWNER_CODE = 'MOONLIGHTOWNER';
    if (codeInput === OWNER_CODE) {
      if (await isOwner(email)) {
        return res.status(400).json({ error: 'Вы уже являетесь владельцем.' });
      }
      await query('INSERT IGNORE INTO owners (email, activated_at) VALUES (?, ?)', [email, new Date()]);
      return res.json({ success: true, isOwner: true, message: 'Права владельца получены! Доступна панель управления кодами.' });
    }

    // Обычный код
    const [found] = await query('SELECT * FROM codes WHERE code = ?', [codeInput]);
    if (found.length === 0) {
      return res.status(404).json({ error: 'Код не найден. Проверьте правильность ввода.' });
    }
    const code = found[0];

    const [already] = await query(
      'SELECT id FROM redeemed_codes WHERE user_email = ? AND code = ?',
      [email, codeInput]
    );
    if (already.length > 0) {
      return res.status(400).json({ error: 'Этот код уже был вами активирован.' });
    }

    const now = new Date();
    const expiresAt = new Date(now.getTime() + code.duration * 24 * 60 * 60 * 1000);

    await query(
      'INSERT INTO redeemed_codes (user_email, code, tier, activated_at, expires_at) VALUES (?, ?, ?, ?, ?)',
      [email, code.code, code.tier, now, expiresAt]
    );

    const tierNames = { premium: 'Premium', pro: 'Pro' };
    res.json({
      success: true,
      tier: code.tier,
      expiresAt: expiresAt.toISOString(),
      message: 'Код успешно активирован! Подписка ' + (tierNames[code.tier] || code.tier) + ' активна на ' + code.duration + ' дней.'
    });
  } catch (err) {
    console.error('redeem error:', err.message);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// ============================================================
// Запуск сервера
// ============================================================
async function start() {
  const ok = await initDb();
  if (!ok) {
    console.error('Не удалось подключиться к базе. Проверьте .env файл.');
    process.exit(1);
  }
  // Пинг каждые 10 секунд, чтобы соединения не простаивали
  startKeepAlive(10000);
  app.listen(PORT, () => {
    console.log('🚀 MoonLight Visual сервер запущен на http://localhost:' + PORT);
  });
}

start();
