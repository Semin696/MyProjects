// ===== MoonLight Visual — Система подписок =====
// Данные хранятся в базе данных (Express + MySQL)

// --- Код владельца (мастер-код) ---
const OWNER_CODE = 'MOONLIGHTOWNER';

// --- Уровни подписки ---
const SUBSCRIPTION_TIERS = {
  free: {
    id: 'free',
    name: 'Free',
    icon: 'i-moon',
    color: '#6a8aa0',
    features: [
      'Доступ к базовым визуалам',
      'Просмотр контента',
      'Сообщество Discord'
    ]
  },
  premium: {
    id: 'premium',
    name: 'Premium',
    icon: 'i-star',
    color: '#38bdf8',
    features: [
      'Всё из Free',
      'Ранний доступ к новым текстурам',
      'Эксклюзивные ресурспаки',
      'Особая роль в Discord'
    ]
  },
  pro: {
    id: 'pro',
    name: 'Pro',
    icon: 'i-gem',
    color: '#0ea5e9',
    features: [
      'Всё из Premium',
      'Полный доступ ко всем визуалам',
      'Эксклюзивные шейдеры',
      'Приоритетная поддержка',
      'Голос за новые текстуры'
    ]
  }
};

// --- Инициализация (больше не нужна, данные на сервере) ---
function initCodes() {}

// --- Получить все коды (только для владельца) ---
async function getAllCodes() {
  const data = await api('/api/codes');
  return data.codes || [];
}

// --- Проверить, является ли пользователь владельцем ---
async function isOwner(userEmail) {
  if (!userEmail) {
    const me = getSession();
    if (me) userEmail = me.email;
  }
  try {
    const data = await fetchCurrentUser();
    return !!(data && data.isOwner);
  } catch (e) {
    return false;
  }
}

// --- Проверить код и активировать ---
async function redeemCode(userEmail, codeInput) {
  try {
    const data = await api('/api/redeem', {
      method: 'POST',
      body: { code: codeInput }
    });
    if (data.isOwner) {
      return { success: true, isOwner: true, message: data.message };
    }
    return {
      success: true,
      tier: data.tier,
      expiresAt: data.expiresAt,
      message: data.message
    };
  } catch (err) {
    return { success: false, message: err.message };
  }
}

// --- Создать новый код (только для владельца) ---
async function createCode(ownerEmail, tier, duration, note) {
  try {
    const data = await api('/api/codes', {
      method: 'POST',
      body: { tier, duration, note }
    });
    return { success: true, code: data.code, message: 'Код ' + data.code + ' создан!' };
  } catch (err) {
    return { success: false, message: err.message };
  }
}

// --- Удалить код (только для владельца) ---
async function deleteCode(ownerEmail, codeStr) {
  try {
    await api('/api/codes/' + encodeURIComponent(codeStr), { method: 'DELETE' });
    return { success: true, message: 'Код ' + codeStr + ' удалён.' };
  } catch (err) {
    return { success: false, message: err.message };
  }
}

// --- Получить коды, созданные владельцем ---
async function getOwnerCodes(ownerEmail) {
  try {
    return await getAllCodes();
  } catch (e) {
    return [];
  }
}

// --- Получить активную подписку пользователя ---
async function getActiveSubscription(userEmail) {
  try {
    const data = await api('/api/user/profile');
    return data.subscription || null;
  } catch (e) {
    return null;
  }
}

// --- Проверить, активна ли подписка ---
function isSubscriptionActive(sub) {
  if (!sub) return false;
  return new Date(sub.expiresAt) > new Date();
}

// --- Осталось дней до истечения ---
function daysUntilExpiry(sub) {
  if (!sub) return 0;
  const ms = new Date(sub.expiresAt).getTime() - Date.now();
  return Math.max(0, Math.ceil(ms / (1000 * 60 * 60 * 24)));
}

// --- Форматирование даты ---
function formatDate(dateStr) {
  const d = new Date(dateStr);
  const months = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
    'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
  return d.getDate() + ' ' + months[d.getMonth()] + ' ' + d.getFullYear();
}

// --- Получить уровень подписки пользователя ---
async function getUserTier(userEmail) {
  const sub = await getActiveSubscription(userEmail);
  if (sub) return sub.tier;
  return 'free';
}

// --- Получить историю подписок ---
async function getSubscriptionHistory(userEmail) {
  try {
    const data = await api('/api/user/profile');
    return data.history || [];
  } catch (e) {
    return [];
  }
}

// --- Инициализировать при загрузке ---
initCodes();
