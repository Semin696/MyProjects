// MoonLight Visual — seed стартовых кодов
const { pool } = require('./db');

const defaults = [
  { code: 'MOONLIGHT2026', tier: 'premium', duration: 30, note: 'Стартовый код' },
  { code: 'VISUALPRO', tier: 'pro', duration: 90, note: 'Pro на 3 месяца' },
  { code: 'MINECRAFT', tier: 'premium', duration: 14, note: 'Premium на 2 недели' },
  { code: 'STARLIGHT', tier: 'pro', duration: 30, note: 'Pro на месяц' },
  { code: 'BLOCKCRAFT', tier: 'premium', duration: 60, note: 'Premium на 2 месяца' },
  { code: 'NIGHTOWL', tier: 'pro', duration: 365, note: 'Pro на год' }
];

async function seed() {
  for (const c of defaults) {
    try {
      await pool.query(
        'INSERT IGNORE INTO codes (code, tier, duration, note, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)',
        [c.code, c.tier, c.duration, c.note, 'system', new Date()]
      );
      console.log('OK:', c.code);
    } catch (e) {
      console.log('FAIL:', c.code, '->', e.message);
    }
  }
  const [rows] = await pool.query('SELECT code FROM codes');
  console.log('Коды в базе:', rows.map(r => r.code).join(', '));
  process.exit(0);
}
seed();
