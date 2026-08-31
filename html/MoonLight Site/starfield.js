// ===== MoonLight Visual — Анимированный звёздный фон =====

(function () {
  const canvas = document.getElementById('starfield');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  let stars = [];
  let shootingStars = [];
  let mouseX = 0;
  let mouseY = 0;
  let w, h;

  // --- Параметры ---
  const STAR_COUNT_BASE = 200;
  const STAR_COLORS = [
    { r: 56, g: 189, b: 248 },   // голубой
    { r: 125, g: 211, b: 252 },  // светло-голубой
    { r: 255, g: 255, b: 255 },  // белый
    { r: 14, g: 165, b: 233 }    // тёмно-голубой
  ];

  // --- Изменение размера ---
  function resize() {
    w = canvas.width = window.innerWidth;
    h = canvas.height = window.innerHeight;
  }

  // --- Создание звёзд ---
  function initStars() {
    stars = [];
    const count = Math.min(STAR_COUNT_BASE, Math.floor((w * h) / 6000));
    for (let i = 0; i < count; i++) {
      const colorIndex = Math.floor(Math.random() * STAR_COLORS.length);
      stars.push({
        x: Math.random() * w,
        y: Math.random() * h,
        size: Math.random() * 2 + 0.5,
        baseOpacity: Math.random() * 0.6 + 0.2,
        twinkleSpeed: Math.random() * 0.02 + 0.005,
        twinklePhase: Math.random() * Math.PI * 2,
        color: STAR_COLORS[colorIndex],
        vx: (Math.random() - 0.5) * 0.05,
        vy: (Math.random() - 0.5) * 0.05,
        depth: Math.random() * 0.8 + 0.2 // для параллакса
      });
    }
  }

  // --- Создание падающей звезды ---
  function spawnShootingStar() {
    if (Math.random() > 0.003) return; // редкое появление

    const angle = Math.PI * 0.2 + Math.random() * Math.PI * 0.15;
    const speed = 8 + Math.random() * 6;

    shootingStars.push({
      x: Math.random() * w,
      y: Math.random() * h * 0.5,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: 1,
      decay: 0.01 + Math.random() * 0.01,
      length: 60 + Math.random() * 40
    });
  }

  // --- Отрисовка звёзд ---
  function drawStars() {
    for (let i = 0; i < stars.length; i++) {
      const s = stars[i];

      // Мерцание
      s.twinklePhase += s.twinkleSpeed;
      const twinkle = Math.sin(s.twinklePhase) * 0.3 + 0.7;
      const opacity = s.baseOpacity * twinkle;

      // Движение
      s.x += s.vx;
      s.y += s.vy;

      // Параллакс от мыши
      const parallaxX = (mouseX - w / 2) * s.depth * 0.01;
      const parallaxY = (mouseY - h / 2) * s.depth * 0.01;

      // Обёртывание по краям
      let px = s.x + parallaxX;
      let py = s.y + parallaxY;
      if (px < 0) px += w;
      if (px > w) px -= w;
      if (py < 0) py += h;
      if (py > h) py -= h;

      // Рисуем звезду
      const c = s.color;
      ctx.beginPath();
      ctx.arc(px, py, s.size, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(' + c.r + ',' + c.g + ',' + c.b + ',' + opacity + ')';
      ctx.fill();

      // Свечение для крупных звёзд
      if (s.size > 1.5) {
        ctx.beginPath();
        ctx.arc(px, py, s.size * 3, 0, Math.PI * 2);
        const grad = ctx.createRadialGradient(px, py, 0, px, py, s.size * 3);
        grad.addColorStop(0, 'rgba(' + c.r + ',' + c.g + ',' + c.b + ',' + (opacity * 0.3) + ')');
        grad.addColorStop(1, 'rgba(' + c.r + ',' + c.g + ',' + c.b + ',0)');
        ctx.fillStyle = grad;
        ctx.fill();
      }
    }
  }

  // --- Отрисовка падающих звёзд ---
  function drawShootingStars() {
    for (let i = shootingStars.length - 1; i >= 0; i--) {
      const ss = shootingStars[i];

      ss.x += ss.vx;
      ss.y += ss.vy;
      ss.life -= ss.decay;

      if (ss.life <= 0 || ss.x > w + 100 || ss.y > h + 100) {
        shootingStars.splice(i, 1);
        continue;
      }

      // Хвост
      const tailX = ss.x - ss.vx * ss.length / 10;
      const tailY = ss.y - ss.vy * ss.length / 10;

      const grad = ctx.createLinearGradient(tailX, tailY, ss.x, ss.y);
      grad.addColorStop(0, 'rgba(56, 189, 248, 0)');
      grad.addColorStop(0.5, 'rgba(56, 189, 248, ' + (ss.life * 0.4) + ')');
      grad.addColorStop(1, 'rgba(255, 255, 255, ' + ss.life + ')');

      ctx.beginPath();
      ctx.moveTo(tailX, tailY);
      ctx.lineTo(ss.x, ss.y);
      ctx.strokeStyle = grad;
      ctx.lineWidth = 2;
      ctx.lineCap = 'round';
      ctx.stroke();

      // Голова
      ctx.beginPath();
      ctx.arc(ss.x, ss.y, 2, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(255, 255, 255, ' + ss.life + ')';
      ctx.fill();
    }
  }

  // --- Главный цикл ---
  function animate() {
    ctx.clearRect(0, 0, w, h);
    drawStars();
    spawnShootingStar();
    drawShootingStars();
    requestAnimationFrame(animate);
  }

  // --- События мыши ---
  window.addEventListener('mousemove', function (e) {
    mouseX = e.clientX;
    mouseY = e.clientY;
  });

  // --- События изменения размера ---
  window.addEventListener('resize', function () {
    resize();
    initStars();
  });

  // --- Запуск ---
  resize();
  initStars();
  animate();
})();
