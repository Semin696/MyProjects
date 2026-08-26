const { getPool, ensureSchema, json, readBody, sha256, makeToken } = require('./_lib');

module.exports = async (req, res) => {
  if (req.method !== 'POST') return json(res, 405, { error: 'Метод не поддерживается' });
  try {
    const { username, password } = await readBody(req);
    await ensureSchema();
    const [rows] = await getPool().execute(
      'SELECT id, username, role, subscribed, pass_hash, salt FROM mlv_users WHERE username = ?',
      [String(username || '').trim()]
    );
    const user = rows[0];
    if (!user || sha256(user.salt + String(password || '')) !== user.pass_hash) {
      return json(res, 401, { error: 'Неверный никнейм или пароль' });
    }
    const token = makeToken();
    await getPool().execute('INSERT INTO mlv_sessions (token, user_id) VALUES (?, ?)', [token, user.id]);
    return json(res, 200, {
      ok: true,
      token,
      user: { username: user.username, role: user.role, subscribed: !!user.subscribed },
    });
  } catch (e) {
    console.error(e);
    return json(res, 500, { error: 'Ошибка сервера' });
  }
};
