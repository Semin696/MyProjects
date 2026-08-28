const mysql = require('mysql2/promise');
const crypto = require('crypto');

const OWNER_CODE = process.env.OWNER_CODE || 'MLV-OWNER-2026';

function getPool() {
  if (!global.__mlvPool) {
    global.__mlvPool = mysql.createPool({
      host: process.env.MYSQL_HOST,
      port: Number(process.env.MYSQL_PORT || 3306),
      user: process.env.MYSQL_USER,
      password: process.env.MYSQL_PASSWORD,
      database: process.env.MYSQL_DATABASE || 'railway',
      ssl: { rejectUnauthorized: false },
      waitForConnections: true,
      connectionLimit: 5,
    });
  }
  return global.__mlvPool;
}

async function ensureSchema() {
  if (!global.__mlvSchema) {
    global.__mlvSchema = (async () => {
      const db = getPool();
      await db.execute(`
        CREATE TABLE IF NOT EXISTS mlv_users (
          id INT AUTO_INCREMENT PRIMARY KEY,
          username VARCHAR(32) NOT NULL UNIQUE,
          pass_hash CHAR(64) NOT NULL,
          salt CHAR(32) NOT NULL,
          role ENUM('user','owner') NOT NULL DEFAULT 'user',
          subscribed TINYINT(1) NOT NULL DEFAULT 0,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) CHARACTER SET utf8mb4
      `);
      await db.execute(`
        CREATE TABLE IF NOT EXISTS mlv_codes (
          code VARCHAR(32) PRIMARY KEY,
          type ENUM('sub','tester','owner') NULL,
          created_by INT NULL,
          used_by INT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          used_at TIMESTAMP NULL
        ) CHARACTER SET utf8mb4
      `);
      await db.execute(`
        CREATE TABLE IF NOT EXISTS mlv_sessions (
          token CHAR(64) PRIMARY KEY,
          user_id INT NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          FOREIGN KEY (user_id) REFERENCES mlv_users(id) ON DELETE CASCADE
        ) CHARACTER SET utf8mb4
      `);
      await db.execute(
        "ALTER TABLE mlv_users MODIFY COLUMN role ENUM('user','tester','owner') NOT NULL DEFAULT 'user'"
      ).catch(() => {});
      await db.execute(
        "ALTER TABLE mlv_codes ADD COLUMN type ENUM('sub','tester','owner') NULL"
      ).catch(() => {});
      await db.execute(
        'ALTER TABLE mlv_users ADD COLUMN email VARCHAR(255) NULL'
      ).catch(() => {});
      await db.execute(
        'ALTER TABLE mlv_users ADD COLUMN pass_enc TEXT NULL'
      ).catch(() => {});
    })().catch(e => {
      global.__mlvSchema = null;
      throw e;
    });
  }
  return global.__mlvSchema;
}

function json(res, status, obj) {
  res.statusCode = status;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.end(JSON.stringify(obj));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.on('data', c => {
      data += c;
      if (data.length > 1e5) reject(new Error('too large'));
    });
    req.on('end', () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch {
        resolve({});
      }
    });
    req.on('error', reject);
  });
}

function sha256(str) {
  return crypto.createHash('sha256').update(str).digest('hex');
}

const PASS_SECRET = process.env.PASS_SECRET || 'mlv-fallback-secret-key';

function encryptText(text) {
  const key = crypto.createHash('sha256').update(PASS_SECRET).digest();
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
  const enc = Buffer.concat([cipher.update(String(text), 'utf8'), cipher.final()]);
  return iv.toString('hex') + ':' + enc.toString('hex');
}

function decryptText(data) {
  try {
    const [ivHex, encHex] = String(data).split(':');
    const key = crypto.createHash('sha256').update(PASS_SECRET).digest();
    const decipher = crypto.createDecipheriv('aes-256-cbc', key, Buffer.from(ivHex, 'hex'));
    return Buffer.concat([decipher.update(Buffer.from(encHex, 'hex')), decipher.final()]).toString('utf8');
  } catch {
    return null;
  }
}

function makeSalt() {
  return crypto.randomBytes(16).toString('hex');
}

function makeToken() {
  return crypto.randomBytes(32).toString('hex');
}

const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const CODE_PREFIXES = {
  sub: 'MONO-SUB',
  tester: 'MONO-TESTER',
  owner: 'MONO-OWNER',
};

function makeCode(type) {
  const prefix = CODE_PREFIXES[type] || CODE_PREFIXES.tester;
  const bytes = crypto.randomBytes(6);
  let out = '';
  for (let i = 0; i < 6; i++) out += CODE_ALPHABET[bytes[i] % CODE_ALPHABET.length];
  return `${prefix}-${out}`;
}

async function auth(req) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return null;
  await ensureSchema();
  const [rows] = await getPool().execute(
    `SELECT u.id, u.username, u.role, u.subscribed, u.created_at, u.email
     FROM mlv_sessions s JOIN mlv_users u ON u.id = s.user_id
     WHERE s.token = ? AND s.created_at > NOW() - INTERVAL 30 DAY`,
    [token]
  );  return rows[0] || null;
}

module.exports = {
  OWNER_CODE,
  getPool,
  ensureSchema,
  json,
  readBody,
  sha256,
  makeSalt,
  makeToken,
  makeCode,
  auth,
  encryptText,
  decryptText,
};
