const { getPool, ensureSchema, json, auth, decryptText } = require('./_lib');

module.exports = async (req, res) => {
  try {
    const user = await auth(req);
    if (!user) return json(res, 401, { error: 'Не авторизован' });
    await ensureSchema();
    const [rows] = await getPool().execute(
      'SELECT email, pass_enc FROM mlv_users WHERE id = ?',
      [user.id]
    );
    if (rows.length === 0) return json(res, 404, { error: 'Пользователь не найден' });
    return json(res, 200, {
      ok: true,
      email: rows[0].email || null,
      password: rows[0].pass_enc ? decryptText(rows[0].pass_enc) : null,
    });
  } catch (e) {
    console.error(e);
    return json(res, 500, { error: 'Ошибка сервера' });
  }
};
