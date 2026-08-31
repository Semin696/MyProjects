// MoonLight Visual — применение схемы БД
// Запуск: node server/apply-db.js
const fs = require('fs');
const path = require('path');
const { pool } = require('./db');

(async () => {
  const sqlPath = path.join(__dirname, 'db.sql');
  const sql = fs.readFileSync(sqlPath, 'utf8');

  // Дроп всех существующих таблиц (для чистоты)
  const [tables] = await pool.query('SHOW TABLES');
  for (const row of tables) {
    const name = Object.values(row)[0];
    await pool.query('DROP TABLE IF EXISTS `' + name + '`');
    console.log('dropped:', name);
  }

  // Убираем комментарии (строки, начинающиеся с --) и пустые строки
  const cleaned = sql
    .split('\n')
    .filter(line => !line.trim().startsWith('--'))
    .join('\n');

  const statements = cleaned
    .split(';')
    .map(s => s.trim())
    .filter(s => s);

  for (const st of statements) {
    if (!st) continue;
    try {
      await pool.query(st);
      console.log('OK:', st.split('(')[0].trim());
    } catch (e) {
      console.log('FAIL:', st.split('(')[0].trim(), '->', e.message);
      console.log('SQL:', st);
    }
  }

  const [final] = await pool.query('SHOW TABLES');
  console.log('TABLES:', final.map(r => Object.values(r)[0]).join(', '));
  process.exit(0);
})();
