const { OWNER_CODE, getPool, ensureSchema, json, readBody, sha256, makeSalt, makeToken, encryptText } = require('./_lib');

module.exports = async (req, res) => {
  if (req.method !== 'POST') return json(res, 405, { error: 'Метод не поддерживается' });
  try {
    const body = await readBody(req);
    const username = String(body.username || '').trim();
    const password = String(body.password || '');
    const email = String(body.email || '').trim().toLowerCase();
    const code = String(body.code || '').trim();

    if (!/^[A-Za-z0-9_]{3,20}$/.test(username)) {
      return json(res, 400, { error: 'Никнейм: 3–20 символов, латиница, цифры, _' });
    }
    if (password.length < 4) {
      return json(res, 400, { error: 'Пароль минимум 4 символа' });
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      return json(res, 400, { error: 'Введите корректный email' });
    }

    await ensureSchema();
    const db = getPool();

    const [existing] = await db.execute('SELECT id FROM mlv_users WHERE username = ?', [username]);
    if (existing.length > 0) {
      return json(res, 409, { error: 'Такой никнейм уже занят' });
    }
    const [existingEmail] = await db.execute('SELECT id FROM mlv_users WHERE email = ?', [email]);
    if (existingEmail.length > 0) {
      return json(res, 409, { error: 'Этот email уже используется' });
    }

    let role = 'user';
    let subscribed = 0;
    let codeRow = null;

    if (code) {
      if (code === OWNER_CODE || code.startsWith('MONO-OWNER-')) {
        role = 'owner';
        subscribed = 1;
      } else if (/^MONO-(SUB|TESTER)-/.test(code)) {
        const [rows] = await db.execute(
          'SELECT code FROM mlv_codes WHERE code = ? AND used_by IS NULL',
          [code]
        );
        if (rows.length === 0) {
          return json(res, 400, { error: 'Неверный или уже использованный код' });
        }
        if (code.startsWith('MONO-TESTER-')) role = 'tester';
        subscribed = 1;
      } else {
        return json(res, 400, { error: 'Неверный код' });
      }
      codeRow = code;
    }

    const salt = makeSalt();
    const hash = sha256(salt + password);
    const [result] = await db.execute(
      'INSERT INTO mlv_users (username, pass_hash, salt, role, subscribed, email, pass_enc) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [username, hash, salt, role, subscribed, email, encryptText(password)]
    );

    if (codeRow) {
      await db.execute('UPDATE mlv_codes SET used_by = ?, used_at = NOW() WHERE code = ?', [
        result.insertId,
        codeRow,
      ]);
    }

    const token = makeToken();
    await db.execute('INSERT INTO mlv_sessions (token, user_id) VALUES (?, ?)', [token, result.insertId]);

    return json(res, 200, { ok: true, token, user: { username, role, subscribed } });
  } catch (e) {
    console.error(e);
    return json(res, 500, { error: 'Ошибка сервера' });
  }
};
