// ===== MoonLight Visual — Логика страницы профиля =====
// Данные хранятся в базе данных (Express + MySQL)

// --- Хелпер: SVG-иконка ---
function icon(name, cls) {
  return '<svg class="icon ' + (cls || '') + '"><use href="#' + name + '"></use></svg>';
}

// --- Получить инициалы ---
function getInitials(name) {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
}

// --- Форматирование даты ---
function formatDate(dateStr) {
  const d = new Date(dateStr);
  const months = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
    'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
  return d.getDate() + ' ' + months[d.getMonth()] + ' ' + d.getFullYear();
}

// --- Рендер страницы профиля ---
async function renderProfile() {
  const session = getSession();
  const container = document.getElementById('profileContainer');

  if (!session) {
    // Пользователь не авторизован
    container.innerHTML = `
      <div class="profile-notauth">
        <div class="profile-notauth-icon">${icon('i-moon')}</div>
        <h2>Вы не авторизованы</h2>
        <p>Войдите или зарегистрируйтесь, чтобы увидеть свой профиль.</p>
        <div class="profile-notauth-actions">
          <button class="btn-primary" onclick="openModal('loginModal')">Войти</button>
          <button class="btn-ghost" onclick="openModal('registerModal')">Регистрация</button>
        </div>
      </div>
    `;
    return;
  }

  // Загружаем полные данные с сервера
  let data;
  try {
    data = await api('/api/user/profile');
  } catch (e) {
    if (e.status === 401) {
      // Сессия устарела
      setToken(null);
      clearSession();
      renderProfile();
      return;
    }
    container.innerHTML = '<p class="profile-error">Не удалось загрузить профиль. Попробуйте позже.</p>';
    return;
  }

  const user = data.user;
  const activeSub = data.subscription;
  const history = data.history || [];
  const isOwnerFlag = !!user.isOwner;

  // Обновляем сессию названием (на случай смены имени)
  setSession({ name: user.name, email: user.email });

  const initials = getInitials(user.name);
  const regDate = user.registeredAt ? formatDate(user.registeredAt) : 'недавно';
  const daysAgo = user.registeredAt
    ? Math.floor((Date.now() - new Date(user.registeredAt).getTime()) / (1000 * 60 * 60 * 24))
    : 0;

  // Данные подписки
  const currentTier = activeSub ? activeSub.tier : 'free';
  const tierInfo = SUBSCRIPTION_TIERS[currentTier];
  const daysLeft = daysUntilExpiry(activeSub);

  // Бейдж подписки
  const subBadge = activeSub
    ? `<div class="profile-badge" style="background: rgba(${tierInfo.id === 'pro' ? '14, 165, 233' : '56, 189, 248'}, 0.1); border-color: rgba(${tierInfo.id === 'pro' ? '14, 165, 233' : '56, 189, 248'}, 0.3); color: ${tierInfo.color};">
         ${icon(tierInfo.icon)}
         <span>Подписка ${tierInfo.name} · ${daysLeft} дн. осталось</span>
       </div>`
    : `<div class="profile-badge">
         ${icon('i-moon')}
         <span>Участник MoonLight Visual</span>
       </div>`;

  // История подписок
  let historyHTML = '';
  if (history.length > 0) {
    historyHTML = history.map(h => {
      const t = SUBSCRIPTION_TIERS[h.tier] || SUBSCRIPTION_TIERS.free;
      const expired = new Date(h.expiresAt) < new Date();
      return `
        <div class="sub-history-item ${expired ? 'expired' : 'active'}">
          <div class="sub-history-info">
            <span class="sub-history-icon">${icon(t.icon)}</span>
            <div class="sub-history-text">
              <span class="sub-history-name">${t.name}</span>
              <span class="sub-history-code">Код: ${escapeHtml(h.code)}</span>
            </div>
          </div>
          <div class="sub-history-dates">
            <span class="sub-history-date">${formatDate(h.activatedAt)}</span>
            <span class="sub-history-status ${expired ? 'expired' : 'active'}">
              ${expired ? 'Истекла' : 'Активна до ' + formatDate(h.expiresAt)}
            </span>
          </div>
        </div>
      `;
    }).join('');
  } else {
    historyHTML = '<p class="sub-history-empty">У вас пока нет активированных кодов.</p>';
  }

  container.innerHTML = `
    <!-- Шапка профиля -->
    <div class="profile-header">
      <div class="profile-avatar">${initials}</div>
      <h2 class="profile-name">${escapeHtml(user.name)}</h2>
      <p class="profile-email">${escapeHtml(user.email)}</p>
      ${subBadge}
    </div>

    <!-- Статистика -->
    <div class="profile-stats">
      <div class="profile-stat">
        <div class="profile-stat-num">${daysAgo}</div>
        <div class="profile-stat-label">Дней с нами</div>
      </div>
      <div class="profile-stat">
        <div class="profile-stat-num">${history.length}</div>
        <div class="profile-stat-label">Кодов активировано</div>
      </div>
      <div class="profile-stat">
        <div class="profile-stat-num profile-stat-icon">${icon(tierInfo.icon)}</div>
        <div class="profile-stat-label">Тариф</div>
      </div>
    </div>

    <!-- Подписка -->
    <div class="profile-settings">
      <h3 class="profile-settings-title">${icon(tierInfo.icon)} Моя подписка</h3>

      <div class="sub-status ${activeSub ? 'sub-active' : 'sub-free'}">
        <div class="sub-status-info">
          <span class="sub-status-tier" style="color: ${tierInfo.color};">${icon(tierInfo.icon)} ${tierInfo.name}</span>
          ${activeSub
            ? `<span class="sub-status-expire">Действует до ${formatDate(activeSub.expiresAt)} · ${daysLeft} дн.</span>`
            : '<span class="sub-status-expire">Базовый тариф</span>'}
        </div>
        <button class="btn-edit" onclick="openRedeemCode()">${icon('i-gift')} Активировать код</button>
      </div>

      <div class="sub-features">
        ${tierInfo.features.map(f => `<li>${f}</li>`).join('')}
      </div>

      <!-- История -->
      <div class="sub-history">
        <h4 class="sub-history-title">${icon('i-history')} История подписок</h4>
        <div class="sub-history-list">
          ${historyHTML}
        </div>
      </div>
    </div>

    <!-- Панель владельца (только для владельцев) -->
    ${isOwnerFlag ? '<div class="owner-panel-placeholder"></div>' : ''}

    <!-- Настройки -->
    <div class="profile-settings">
      <h3 class="profile-settings-title">${icon('i-settings')} Настройки аккаунта</h3>
      <div class="settings-list">
        <div class="settings-item">
          <div class="settings-item-info">
            <span class="settings-item-icon">${icon('i-user')}</span>
            <div class="settings-item-text">
              <span class="settings-item-label">Имя пользователя</span>
              <span class="settings-item-desc">${escapeHtml(user.name)}</span>
            </div>
          </div>
          <button class="btn-edit" onclick="openEditName('${escapeHtml(user.name)}')">Изменить</button>
        </div>

        <div class="settings-item">
          <div class="settings-item-info">
            <span class="settings-item-icon">${icon('i-mail')}</span>
            <div class="settings-item-text">
              <span class="settings-item-label">Email</span>
              <span class="settings-item-desc">${escapeHtml(user.email)}</span>
            </div>
          </div>
          <span class="settings-item-desc">Нельзя изменить</span>
        </div>

        <div class="settings-item">
          <div class="settings-item-info">
            <span class="settings-item-icon">${icon('i-lock')}</span>
            <div class="settings-item-text">
              <span class="settings-item-label">Пароль</span>
              <span class="settings-item-desc">••••••••</span>
            </div>
          </div>
          <button class="btn-edit" onclick="openEditPassword()">Сменить</button>
        </div>

        <div class="settings-item">
          <div class="settings-item-info">
            <span class="settings-item-icon">${icon('i-calendar')}</span>
            <div class="settings-item-text">
              <span class="settings-item-label">Дата регистрации</span>
              <span class="settings-item-desc">${regDate}</span>
            </div>
          </div>
        </div>

        <div class="settings-item">
          <div class="settings-item-info">
            <span class="settings-item-icon">${icon('i-trash')}</span>
            <div class="settings-item-text">
              <span class="settings-item-label">Удалить аккаунт</span>
              <span class="settings-item-desc">Безвозвратно удалить все данные</span>
            </div>
          </div>
          <button class="btn-danger" onclick="openDeleteAccount()">Удалить</button>
        </div>
      </div>
    </div>
  `;

  // Заполняем панель владельца асинхронно, если пользователь — владелец
  if (isOwnerFlag) {
    await renderOwnerPanel(user);
  }
}

// --- Панель владельца ---
async function renderOwnerPanel(user) {
  let codes = [];
  try {
    codes = await getOwnerCodes(user.email);
  } catch (e) {
    /* ignore */
  }

  // Список кодов
  let codesHTML = '';
  if (codes.length > 0) {
    codesHTML = codes.map(c => {
      const t = SUBSCRIPTION_TIERS[c.tier] || SUBSCRIPTION_TIERS.premium;
      return `
        <div class="owner-code-item">
          <div class="owner-code-info">
            <span class="owner-code-icon">${icon(t.icon)}</span>
            <div class="owner-code-text">
              <span class="owner-code-value">${escapeHtml(c.code)}</span>
              <span class="owner-code-meta">${t.name} · ${c.duration} дн. · ${c.note || '—'}</span>
            </div>
          </div>
          <div class="owner-code-actions">
            <button class="btn-copy" onclick="copyCode('${escapeHtml(c.code)}')">${icon('i-copy')} Копировать</button>
            <button class="btn-danger-sm" onclick="deleteOwnerCode('${escapeHtml(c.code)}')">${icon('i-trash')}</button>
          </div>
        </div>
      `;
    }).join('');
  } else {
    codesHTML = '<p class="owner-codes-empty">Пока нет созданных кодов.</p>';
  }

  const panelHTML = `
    <!-- Панель владельца -->
    <div class="profile-settings owner-panel">
      <h3 class="profile-settings-title">${icon('i-crown')} Панель владельца</h3>

      <div class="owner-badge">
        <span class="owner-badge-icon">${icon('i-crown')}</span>
        <span>Вы владелец MoonLight Visual. У вас есть доступ к управлению промокодами.</span>
      </div>

      <!-- Форма создания кода -->
      <div class="owner-create">
        <h4 class="owner-create-title">${icon('i-zap')} Создать новый промокод</h4>
        <form class="owner-create-form" id="createCodeForm" onsubmit="handleCreateCode(event)">
          <div class="form-row">
            <div class="form-group">
              <label for="newCodeTier">Уровень подписки</label>
              <select id="newCodeTier" required>
                <option value="premium">Premium</option>
                <option value="pro">Pro</option>
              </select>
            </div>
            <div class="form-group">
              <label for="newCodeDuration">Срок (дней)</label>
              <input type="number" id="newCodeDuration" placeholder="30" min="1" max="365" value="30" required>
            </div>
          </div>
          <div class="form-group">
            <label for="newCodeNote">Описание (необязательно)</label>
            <input type="text" id="newCodeNote" placeholder="Например: Код для тестеров">
          </div>
          <button type="submit" class="btn-submit">Создать код</button>
        </form>
      </div>

      <!-- Список кодов -->
      <div class="owner-codes">
        <h4 class="owner-create-title">${icon('i-copy')} Все промокоды (${codes.length})</h4>
        <div class="owner-codes-list">
          ${codesHTML}
        </div>
      </div>
    </div>
  `;

  // Подставляем панель в разметку (replace placeholder или append)
  const placeholder = document.querySelector('.owner-panel-placeholder');
  if (placeholder) {
    placeholder.innerHTML = panelHTML;
  }
  return panelHTML;
}

// --- Создание кода ---
async function handleCreateCode(e) {
  e.preventDefault();
  const session = getSession();
  const tier = document.getElementById('newCodeTier').value;
  const duration = parseInt(document.getElementById('newCodeDuration').value);
  const note = document.getElementById('newCodeNote').value.trim();

  const result = await createCode(session.email, tier, duration, note);

  if (!result.success) {
    showToast(result.message, 'error');
    return;
  }

  showToast(result.message, 'success');
  renderProfile();
}

// --- Удаление кода ---
async function deleteOwnerCode(codeStr) {
  const session = getSession();
  const result = await deleteCode(session.email, codeStr);

  if (!result.success) {
    showToast(result.message, 'error');
    return;
  }

  showToast(result.message, 'info');
  renderProfile();
}

// --- Копирование кода ---
function copyCode(codeStr) {
  navigator.clipboard.writeText(codeStr).then(() => {
    showToast('Код ' + codeStr + ' скопирован!', 'success');
  }).catch(() => {
    const textarea = document.createElement('textarea');
    textarea.value = codeStr;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    showToast('Код ' + codeStr + ' скопирован!', 'success');
  });
}

// --- Активация кода ---
function openRedeemCode() {
  document.getElementById('redeemCodeForm').reset();
  document.getElementById('codeInputError').textContent = '';
  openModal('redeemCodeModal');
}

async function handleRedeemCode(e) {
  e.preventDefault();
  const session = getSession();
  const codeInput = document.getElementById('codeInput').value;
  const errorEl = document.getElementById('codeInputError');
  errorEl.textContent = '';

  const result = await redeemCode(session.email, codeInput);

  if (!result.success) {
    errorEl.textContent = result.message;
    return;
  }

  closeModal('redeemCodeModal');
  showToast(result.message, 'success');
  renderProfile();
}

// --- Модальные окна профиля ---

// Редактирование имени
function openEditName(currentName) {
  document.getElementById('editNameInput').value = currentName;
  document.getElementById('editNameError').textContent = '';
  openModal('editNameModal');
}

async function handleEditName(e) {
  e.preventDefault();
  const newName = document.getElementById('editNameInput').value.trim();
  const errorEl = document.getElementById('editNameError');

  if (newName.length < 2) {
    errorEl.textContent = 'Имя должно содержать минимум 2 символа';
    return;
  }

  try {
    const data = await api('/api/user/name', { method: 'PUT', body: { name: newName } });
    const session = getSession();
    setSession({ name: data.name, email: session.email });
    closeModal('editNameModal');
    showToast('Имя успешно изменено', 'success');
    renderProfile();
    updateAuthUI();
  } catch (err) {
    errorEl.textContent = err.message;
  }
}

// Смена пароля
function openEditPassword() {
  document.getElementById('editPasswordForm').reset();
  document.querySelectorAll('#editPasswordForm .form-error').forEach(el => el.textContent = '');
  openModal('editPasswordModal');
}

async function handleEditPassword(e) {
  e.preventDefault();
  const oldPass = document.getElementById('oldPassword').value;
  const newPass = document.getElementById('newPassword').value;
  const confirmPass = document.getElementById('confirmNewPassword').value;
  let hasError = false;

  ['oldPasswordError', 'newPasswordError', 'confirmNewPasswordError'].forEach(id => {
    document.getElementById(id).textContent = '';
  });

  if (newPass.length < 6) {
    document.getElementById('newPasswordError').textContent = 'Пароль должен быть минимум 6 символов';
    hasError = true;
  }

  if (newPass !== confirmPass) {
    document.getElementById('confirmNewPasswordError').textContent = 'Пароли не совпадают';
    hasError = true;
  }

  if (oldPass === newPass && !hasError) {
    document.getElementById('newPasswordError').textContent = 'Новый пароль не должен совпадать со старым';
    hasError = true;
  }

  if (hasError) return;

  try {
    await api('/api/user/password', {
      method: 'PUT',
      body: { oldPassword: oldPass, newPassword: newPass }
    });
    closeModal('editPasswordModal');
    showToast('Пароль успешно изменен', 'success');
  } catch (err) {
    if (err.status === 401) {
      document.getElementById('oldPasswordError').textContent = 'Неверный текущий пароль';
    } else {
      document.getElementById('newPasswordError').textContent = err.message;
    }
  }
}

// Удаление аккаунта
function openDeleteAccount() {
  document.getElementById('deleteAccountForm').reset();
  document.getElementById('deletePasswordError').textContent = '';
  openModal('deleteAccountModal');
}

async function handleDeleteAccount(e) {
  e.preventDefault();
  const password = document.getElementById('deletePassword').value;
  const errorEl = document.getElementById('deletePasswordError');

  try {
    await api('/api/user', { method: 'DELETE', body: { password } });
    setToken(null);
    clearSession();
    closeModal('deleteAccountModal');
    showToast('Аккаунт удален', 'info');
    setTimeout(() => {
      window.location.href = 'index.html';
    }, 1200);
  } catch (err) {
    errorEl.textContent = err.status === 401 ? 'Неверный пароль' : err.message;
  }
}

// --- Инициализация ---
document.addEventListener('DOMContentLoaded', async () => {
  if (getToken()) {
    try {
      await fetchCurrentUser();
    } catch (e) { /* ignore */ }
  }
  await renderProfile();
});
