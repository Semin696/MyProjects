// ============================================================
// MoonLight Visual — подключение к MySQL
// ============================================================
const mysql = require('mysql2/promise');
require('dotenv').config();

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT || '3306', 10),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 5,
  queueLimit: 0,
  connectTimeout: 30000,
  enableKeepAlive: true,
  keepAliveInitialDelay: 0
});

async function initDb() {
  try {
    const conn = await pool.getConnection();
    console.log('✅ Подключение к базе данных успешно:', process.env.DB_NAME);
    conn.release();
    return true;
  } catch (err) {
    console.error('❌ Ошибка подключения к базе данных:', err.message);
    return false;
  }
}

// Сетевые ошибки, при которых соединение можно считать оборванным
// (FreeSQLDatabase рвёт простаивающие соединения)
function isNetworkError(err) {
  const codes = [
    'ECONNRESET', 'ETIMEDOUT', 'ECONNREFUSED', 'EPIPE',
    'PROTOCOL_CONNECTION_LOST', 'PROTOCOL_ENQUEUE_AFTER_FATAL_ERROR',
    'PROTOCOL_ENQUEUE_AFTER_QUIT', 'PROTOCOL_ORDER_ERROR', 'ER_CLIENT_INTERACTION_TIMEOUT'
  ];
  return codes.includes(err && err.code);
}

// Выполнить запрос с автоматическим повтором при сетевых сбоях
async function query(sql, params, options = {}) {
  const maxRetries = options.retries != null ? options.retries : 3;
  let lastErr;
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await pool.query(sql, params);
    } catch (err) {
      lastErr = err;
      // Повторяем только сетевые ошибки, SQL-ошибки возвращаем сразу
      if (!isNetworkError(err)) {
        throw err;
      }
      console.warn('⚠️ Сетевая ошибка БД, повтор (' + (attempt + 1) + '/' + maxRetries + '):', err.code);
      // Небольшая пауза перед повтором
      if (attempt < maxRetries) {
        await new Promise(res => setTimeout(res, 300 * (attempt + 1)));
      }
    }
  }
  throw lastErr;
}

// Фоновый keep-alive пинг, чтобы соединения не простаивали
let keepAliveTimer = null;
function startKeepAlive(intervalMs = 30000) {
  if (keepAliveTimer) clearInterval(keepAliveTimer);
  keepAliveTimer = setInterval(async () => {
    try {
      await pool.query('SELECT 1');
    } catch (e) {
      // игнорируем, retry-логика пересоздаст соединение
    }
  }, intervalMs);
  // Не держим процесс открытым из-за таймера
  if (keepAliveTimer.unref) keepAliveTimer.unref();
}

module.exports = { pool, initDb, query, startKeepAlive };
