// ===== MoonLight Visual — Логика сайта =====
// Данные хранятся в базе данных (Express + MySQL)

// Валидация email
function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// Очистка ошибок формы
function clearErrors(prefix) {
  const errorFields = document.querySelectorAll(`#${prefix}Form .form-error`);
  const inputs = document.querySelectorAll(`#${prefix}Form input`);
  errorFields.forEach(el => el.textContent = '');
  inputs.forEach(el => el.classList.remove('input-error'));
}

// Показать ошибку
function showError(fieldId, message) {
  const errorEl = document.getElementById(fieldId + 'Error');
  const inputEl = document.getElementById(fieldId);
  if (errorEl) errorEl.textContent = message;
  if (inputEl) inputEl.classList.add('input-error');
}

// --- Регистрация ---
async function handleRegister(e) {
  e.preventDefault();
  clearErrors('reg');

  const name = document.getElementById('regName').value.trim();
  const email = document.getElementById('regEmail').value.trim().toLowerCase();
  const password = document.getElementById('regPassword').value;
  const confirm = document.getElementById('regPasswordConfirm').value;
  let hasError = false;

  // Валидация имени
  if (name.length < 2) {
    showError('regName', 'Имя должно содержать минимум 2 символа');
    hasError = true;
  }

  // Валидация email
  if (!isValidEmail(email)) {
    showError('regEmail', 'Введите корректный email');
    hasError = true;
  }

  // Валидация пароля
  if (password.length < 6) {
    showError('regPassword', 'Пароль должен быть минимум 6 символов');
    hasError = true;
  }

  // Проверка совпадения паролей
  if (password !== confirm) {
    showError('regConfirm', 'Пароли не совпадают');
    hasError = true;
  }

  if (hasError) return;

  try {
    const data = await api('/api/auth/register', {
      method: 'POST',
      body: { name, email, password }
    });
    setToken(data.token);
    setSession(data.user);
    updateAuthUI();
    closeModal('registerModal');
    showToast('Регистрация успешна! Добро пожаловать в MoonLight Visual! 🌙', 'success');
    document.getElementById('registerForm').reset();
  } catch (err) {
    if (err.status === 409) {
      showError('regEmail', 'Пользователь с этим email уже существует');
      showToast('Этот email уже зарегистрирован', 'error');
    } else {
      showError('regEmail', err.message);
      showToast(err.message, 'error');
    }
  }
}

// --- Авторизация ---
async function handleLogin(e) {
  e.preventDefault();
  clearErrors('login');

  const email = document.getElementById('loginEmail').value.trim().toLowerCase();
  const password = document.getElementById('loginPassword').value;
  let hasError = false;

  if (!isValidEmail(email)) {
    showError('loginEmail', 'Введите корректный email');
    hasError = true;
  }

  if (password.length < 1) {
    showError('loginPassword', 'Введите пароль');
    hasError = true;
  }

  if (hasError) return;

  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: { email, password }
    });
    setToken(data.token);
    setSession(data.user);
    updateAuthUI();
    closeModal('loginModal');
    showToast('С возвращением, ' + data.user.name + '! 🌙', 'success');
    document.getElementById('loginForm').reset();
  } catch (err) {
    if (err.status === 404) {
      showError('loginEmail', 'Пользователь не найден');
      showToast('Аккаунт не найден. Зарегистрируйтесь!', 'error');
    } else if (err.status === 401) {
      showError('loginPassword', 'Неверный пароль');
      showToast('Неверный пароль', 'error');
    } else {
      showToast(err.message, 'error');
    }
  }
}

// --- Выход ---
async function logout() {
  try {
    await api('/api/auth/logout', { method: 'POST', body: {} });
  } catch (e) { /* ignore */ }
  setToken(null);
  clearSession();
  updateAuthUI();
  showToast('Вы вышли из аккаунта', 'info');
}

// --- Обновление UI в зависимости от сессии ---
function updateAuthUI() {
  const session = getSession();
  const navAuth = document.getElementById('navAuth');
  const navUser = document.getElementById('navUser');
  const userGreeting = document.getElementById('userGreeting');

  if (session) {
    if (navAuth) navAuth.classList.add('hidden');
    if (navUser) navUser.classList.remove('hidden');
    if (userGreeting) {
      userGreeting.innerHTML = 'Привет, <a href="profile.html" class="user-name-link" title="Открыть профиль">' +
        '<svg class="icon"><use href="#i-user"></use></svg><strong>' + escapeHtml(session.name) + '</strong></a>';
    }
  } else {
    if (navAuth) navAuth.classList.remove('hidden');
    if (navUser) navUser.classList.add('hidden');
  }
}

// --- Модальные окна ---
function openModal(id) {
  document.querySelectorAll('.modal-overlay.active').forEach(m => {
    m.classList.remove('active');
  });
  document.getElementById(id).classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
  document.body.style.overflow = '';
}

function switchModal(fromId, toId) {
  closeModal(fromId);
  setTimeout(() => openModal(toId), 200);
}

// Закрытие по клику на overlay
document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', e => {
    if (e.target === overlay) {
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    }
  });
});

// Закрытие по Escape
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-overlay.active').forEach(m => {
      m.classList.remove('active');
      document.body.style.overflow = '';
    });
  }
});

// --- Показ/скрытие пароля ---
function togglePassword(inputId, btn) {
  const input = document.getElementById(inputId);
  const useEl = btn.querySelector('use');
  if (input.type === 'password') {
    input.type = 'text';
    if (useEl) useEl.setAttribute('href', '#i-eye-off');
    else btn.textContent = '🙈';
  } else {
    input.type = 'password';
    if (useEl) useEl.setAttribute('href', '#i-eye');
    else btn.textContent = '👁';
  }
}

// --- Toast уведомления ---
let toastTimer = null;
function showToast(message, type = 'info') {
  const toast = document.getElementById('toast');
  const icons = { success: 'i-check', error: 'i-x', info: 'i-moon' };
  toast.innerHTML = '<span class="toast-icon"><svg class="icon"><use href="#' + icons[type] + '"></use></svg></span><span>' + escapeHtml(message) + '</span>';
  toast.className = 'toast ' + type + ' show';

  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.className = 'toast ' + type;
  }, 3500);
}

// --- Бургер меню ---
function toggleBurger() {
  const navLinks = document.getElementById('navLinks');
  const burger = document.getElementById('burger');
  navLinks.classList.toggle('open');
  burger.classList.toggle('active');
}

// Закрытие бургера при клике на ссылку
document.querySelectorAll('.nav-link').forEach(link => {
  link.addEventListener('click', () => {
    const navLinks = document.getElementById('navLinks');
    const burger = document.getElementById('burger');
    if (window.innerWidth <= 680) {
      navLinks.classList.remove('open');
      burger.classList.remove('active');
    }
  });
});

// --- Прокрутка к секции ---
function scrollToSection(id) {
  const el = document.getElementById(id);
  if (el) el.scrollIntoView({ behavior: 'smooth' });
}

// --- Анимация прогресс-бара ---
function animateProgress() {
  const fill = document.getElementById('progressFill');
  const percent = document.getElementById('progressPercent');
  let current = 0;
  const target = 65;
  const interval = setInterval(() => {
    current++;
    if (current >= target) {
      current = target;
      clearInterval(interval);
    }
    fill.style.width = current + '%';
    percent.textContent = current + '%';
  }, 30);
}

// --- Утилита: экранирование HTML ---
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// --- Анимации появления при скролле ---
function initRevealAnimations() {
  const revealTargets = document.querySelectorAll(
    '.section-header, .about-card, .pricing-card, .contact-card, .dev-task, .dev-progress, .pricing-code-note'
  );

  revealTargets.forEach((el, i) => {
    el.classList.add('reveal');
    const parent = el.parentElement;
    const siblings = parent ? Array.from(parent.children).filter(c => c.classList.contains('reveal')) : [];
    if (siblings.length > 1) {
      el.style.transitionDelay = (siblings.indexOf(el) * 0.08) + 's';
    }
  });

  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed');
        revealObserver.unobserve(entry.target);
      }
    });
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

  revealTargets.forEach(el => revealObserver.observe(el));
}

// --- Параллакс для hero-карточек ---
function initHeroParallax() {
  const cards = document.querySelectorAll('.visual-card');
  if (!cards.length) return;

  window.addEventListener('mousemove', (e) => {
    const x = (e.clientX / window.innerWidth - 0.5) * 2;
    const y = (e.clientY / window.innerHeight - 0.5) * 2;
    cards.forEach((card, i) => {
      const depth = (i % 2 === 0 ? 1 : -1) * (10 + i * 3);
      card.style.transform = `translate(${x * depth}px, ${y * depth}px)`;
    });
  });
}

// --- Анимация счётчиков ---
function initCounters() {
  const counters = document.querySelectorAll('[data-count]');
  if (!counters.length) return;

  const counterObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const el = entry.target;
        const target = parseFloat(el.getAttribute('data-count'));
        const suffix = el.getAttribute('data-suffix') || '';
        const duration = 1500;
        const start = performance.now();

        function tick(now) {
          const progress = Math.min((now - start) / duration, 1);
          const eased = 1 - Math.pow(1 - progress, 3);
          const current = Math.floor(target * eased);
          el.textContent = current + suffix;
          if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
        counterObserver.unobserve(el);
      }
    });
  }, { threshold: 0.5 });

  counters.forEach(el => counterObserver.observe(el));
}

// --- Инициализация при загрузке ---
document.addEventListener('DOMContentLoaded', async () => {
  // Проверяем сессию на сервере (обновляем данные, если токен ещё валиден)
  if (getToken()) {
    try {
      await fetchCurrentUser();
    } catch (e) { /* офлайн или ошибка сети — используем кэш */ }
  }
  updateAuthUI();
  initRevealAnimations();
  initHeroParallax();
  initCounters();

  // Запускаем анимацию прогресса, когда секция видна
  const statusSection = document.getElementById('status');
  if (statusSection) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          animateProgress();
          observer.disconnect();
        }
      });
    }, { threshold: 0.3 });
    observer.observe(statusSection);
  }
});
