const { json, auth } = require('./_lib');

module.exports = async (req, res) => {
  try {
    const user = await auth(req);
    if (!user) return json(res, 401, { error: 'Не авторизован' });
    return json(res, 200, {
      ok: true,
      user: {
        username: user.username,
        role: user.role,
        subscribed: !!user.subscribed,
        createdAt: user.created_at,
      },
    });
  } catch (e) {
    console.error(e);
    return json(res, 500, { error: 'Ошибка сервера' });
  }
};
