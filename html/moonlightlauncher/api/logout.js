const { getPool, json, readBody } = require('./_lib');

module.exports = async (req, res) => {
  if (req.method !== 'POST') return json(res, 405, { error: 'Метод не поддерживается' });
  try {
    const { token } = await readBody(req);
    if (token) {
      await getPool().execute('DELETE FROM mlv_sessions WHERE token = ?', [String(token)]);
    }
    return json(res, 200, { ok: true });
  } catch (e) {
    console.error(e);
    return json(res, 500, { error: 'Ошибка сервера' });
  }
};
