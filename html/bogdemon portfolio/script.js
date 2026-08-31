(() => {
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const year = $("#year");
  if (year) year.textContent = String(new Date().getFullYear());

  const nav = $("#nav");
  const progress = $(".progress span");
  const menuBtn = $("#menuBtn");
  const menu = $("#menu");
  const glow = $(".pointer-glow");

  const onScroll = () => {
    const y = window.scrollY;
    nav.classList.toggle("is-solid", y > 12);
    const max = document.documentElement.scrollHeight - window.innerHeight;
    progress.style.width = `${max > 0 ? (y / max) * 100 : 0}%`;

    const pos = y + 120;
    let current = "top";
    $$("main section[id]").forEach((section) => {
      if (section.offsetTop <= pos) current = section.id;
    });
    $$(".menu a").forEach((link) => {
      const href = link.getAttribute("href") || "";
      link.classList.toggle("is-active", href === `#${current}`);
    });
  };
  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });

  const closeMenu = () => {
    menu.classList.remove("is-open");
    menuBtn.classList.remove("is-open");
    menuBtn.setAttribute("aria-expanded", "false");
  };

  menuBtn.addEventListener("click", () => {
    const open = !menu.classList.contains("is-open");
    menu.classList.toggle("is-open", open);
    menuBtn.classList.toggle("is-open", open);
    menuBtn.setAttribute("aria-expanded", String(open));
  });

  menu.addEventListener("click", (e) => {
    if (e.target.closest("a")) closeMenu();
  });

  document.addEventListener("mousemove", (e) => {
    document.documentElement.style.setProperty("--mx", `${e.clientX}px`);
    document.documentElement.style.setProperty("--my", `${e.clientY}px`);
  });

  $$(".work-card").forEach((card) => {
    card.addEventListener("pointermove", (e) => {
      if (reduced) return;
      const r = card.getBoundingClientRect();
      const px = (e.clientX - r.left) / r.width;
      const py = (e.clientY - r.top) / r.height;
      card.style.setProperty("--ry", `${(px - 0.5) * 8}deg`);
      card.style.setProperty("--rx", `${(0.5 - py) * 8}deg`);
    });
    card.addEventListener("pointerleave", () => {
      card.style.setProperty("--rx", "0deg");
      card.style.setProperty("--ry", "0deg");
    });
  });

  const filters = $$(".chip");
  filters.forEach((chip) => {
    chip.addEventListener("click", () => {
      const kind = chip.dataset.filter;
      filters.forEach((c) => {
        c.classList.toggle("is-on", c === chip);
        c.setAttribute("aria-selected", String(c === chip));
      });
      $$(".work-card").forEach((card) => {
        const show = kind === "all" || card.dataset.kind === kind;
        card.classList.toggle("is-hide", !show);
      });
    });
  });

  const modal = $("#modal");
  const openCard = (card) => {
    $("#modalTitle").textContent = card.dataset.title || "";
    $("#modalMeta").textContent = card.dataset.meta || "";
    $("#modalDesc").textContent = card.dataset.desc || "";
    $("#modalStack").textContent = card.dataset.stack || "";
    if (typeof modal.showModal === "function") modal.showModal();
  };

  $("#workGrid").addEventListener("click", (e) => {
    const card = e.target.closest(".work-card");
    if (card) openCard(card);
  });

  $("#workGrid").addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      const card = e.target.closest(".work-card");
      if (card) {
        e.preventDefault();
        openCard(card);
      }
    }
  });

  $("#modalClose").addEventListener("click", () => modal.close());
  modal.addEventListener("click", (e) => {
    if (e.target === modal) modal.close();
  });

  const hint = $("#copyHint");
  $("#copyDiscord").addEventListener("click", async () => {
    const nick = $("#copyDiscord").dataset.nick || "demindemon";
    try {
      await navigator.clipboard.writeText(nick);
    } catch {
      const input = document.createElement("textarea");
      input.value = nick;
      document.body.appendChild(input);
      input.select();
      document.execCommand("copy");
      input.remove();
    }
    hint.hidden = false;
    setTimeout(() => {
      hint.hidden = true;
    }, 2200);
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeMenu();
  });

  const canvas = $("#embers");
  const ctx = canvas.getContext("2d", { alpha: true });
  let sparks = [];
  let wisps = [];
  let rings = [];

  const resize = () => {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = Math.floor(window.innerWidth * dpr);
    canvas.height = Math.floor(window.innerHeight * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  };

  const spawnSpark = () => ({
    x: Math.random() * window.innerWidth,
    y: window.innerHeight + Math.random() * 90,
    r: Math.random() * 1.8 + 0.4,
    v: Math.random() * 0.85 + 0.28,
    drift: Math.random() * 0.7 - 0.35,
    a: Math.random() * 0.5 + 0.18,
    gold: Math.random() > 0.72,
  });

  const spawnWisp = () => ({
    x: Math.random() * window.innerWidth,
    y: Math.random() * window.innerHeight,
    r: Math.random() * 90 + 50,
    vx: (Math.random() - 0.5) * 0.18,
    vy: (Math.random() - 0.5) * 0.12,
    a: Math.random() * 0.05 + 0.025,
  });

  const spawnRing = (i) => ({
    x: window.innerWidth * (0.55 + (i - 1) * 0.08),
    y: window.innerHeight * (0.42 + i * 0.04),
    r: 70 + i * 55,
    rot: Math.random() * Math.PI * 2,
    v: 0.0012 + i * 0.0003,
  });

  const tick = () => {
    const w = window.innerWidth;
    const h = window.innerHeight;
    ctx.clearRect(0, 0, w, h);

    wisps.forEach((p) => {
      p.x += p.vx;
      p.y += p.vy;
      if (p.x < -120) p.x = w + 80;
      if (p.x > w + 120) p.x = -80;
      if (p.y < -120) p.y = h + 80;
      if (p.y > h + 120) p.y = -80;
      const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r);
      g.addColorStop(0, `rgba(255, 106, 0, ${p.a})`);
      g.addColorStop(1, "rgba(255, 106, 0, 0)");
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fill();
    });

    ctx.strokeStyle = "rgba(255, 140, 42, 0.14)";
    ctx.lineWidth = 1;
    rings.forEach((ring) => {
      ring.rot += ring.v;
      ctx.save();
      ctx.translate(ring.x, ring.y);
      ctx.rotate(ring.rot);
      ctx.beginPath();
      ctx.arc(0, 0, ring.r, 0.2, Math.PI * 1.4);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(0, 0, ring.r * 0.72, Math.PI * 0.7, Math.PI * 1.9);
      ctx.stroke();
      ctx.restore();
    });

    sparks.forEach((p) => {
      p.y -= p.v;
      p.x += p.drift;
      p.a -= 0.00085;
      if (p.y < -10 || p.a <= 0) Object.assign(p, spawnSpark(), { y: h + 4 });
      ctx.beginPath();
      ctx.fillStyle = p.gold
        ? `rgba(255, 168, 56, ${p.a})`
        : `rgba(255, 106, 0, ${p.a})`;
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fill();
    });

    requestAnimationFrame(tick);
  };

  if (!reduced) {
    resize();
    sparks = Array.from({ length: 78 }, spawnSpark);
    wisps = Array.from({ length: 7 }, spawnWisp);
    rings = [0, 1, 2].map(spawnRing);
    window.addEventListener("resize", () => {
      resize();
      rings = [0, 1, 2].map(spawnRing);
    });
    tick();
  }

  const audio = $("#siteAudio");
  const playBtn = $("#playBtn");
  const coverBtn = $("#coverBtn");
  const dockPlay = $("#dockPlay");
  const dockCover = $("#dockCover");
  const seek = $("#seek");
  const vol = $("#vol");
  const timeCur = $("#timeCur");
  const timeDur = $("#timeDur");
  const statusEl = $("#playerStatus");
  const TRACK_DUR = 30;
  const DEEZER = "https://api.deezer.com/track/3152690101";
  let srcReady = false;
  let seeking = false;

  // api.deezer.com не отдаёт CORS-заголовки, поэтому fetch() из браузера падает — грузим через JSONP.
  const jsonp = (url) => new Promise((resolve, reject) => {
    const cb = `dz_cb_${Date.now().toString(36)}`;
    const s = document.createElement("script");
    const timer = setTimeout(() => {
      cleanup();
      reject(new Error("jsonp timeout"));
    }, 8000);
    const cleanup = () => {
      clearTimeout(timer);
      delete window[cb];
      s.remove();
    };
    window[cb] = (data) => {
      cleanup();
      resolve(data);
    };
    s.onerror = () => {
      cleanup();
      reject(new Error("jsonp failed"));
    };
    s.src = `${url}?output=jsonp&callback=${cb}`;
    document.head.appendChild(s);
  });

  const fmt = (sec) => {
    if (!Number.isFinite(sec) || sec < 0) return "0:00";
    const s = Math.floor(sec);
    return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
  };

  const setPlaying = (on) => {
    document.body.classList.toggle("is-playing", on);
    const label = on ? "Пауза" : "Играть";
    [playBtn, coverBtn, dockCover].forEach((btn) => btn && btn.setAttribute("aria-label", label));
    if (dockPlay) dockPlay.textContent = on ? "Пауза" : "Слушать";
  };

  const setStatus = (text) => {
    if (statusEl) statusEl.textContent = text;
  };

  const ensureSrc = async () => {
    if (srcReady && audio.src) return true;
    setStatus("Подключаю трек…");
    const data = await jsonp(DEEZER);
    if (!data || data.error || !data.preview) throw new Error("no preview");
    audio.src = data.preview;
    srcReady = true;
    return true;
  };

  const toggle = async () => {
    try {
      if (!audio.paused && audio.src) {
        audio.pause();
        return;
      }
      await ensureSrc();
      audio.volume = vol ? Number(vol.value) : 0.85;
      await audio.play();
      setStatus("Играет на сайте.");
    } catch (err) {
      setStatus("Трек не загрузился — проверь соединение и нажми Play ещё раз.");
    }
  };

  audio.addEventListener("play", () => setPlaying(true));
  audio.addEventListener("pause", () => setPlaying(false));
  audio.addEventListener("ended", () => {
    setPlaying(false);
    audio.currentTime = 0;
    setStatus("Превью закончилось — Play, чтобы послушать снова.");
  });
  audio.addEventListener("loadedmetadata", () => {
    timeDur.textContent = fmt(audio.duration || TRACK_DUR);
  });
  audio.addEventListener("timeupdate", () => {
    if (seeking) return;
    const dur = audio.duration || TRACK_DUR;
    timeCur.textContent = fmt(audio.currentTime);
    seek.value = String(Math.round((audio.currentTime / dur) * 1000));
  });

  seek.addEventListener("pointerdown", () => { seeking = true; });
  seek.addEventListener("pointerup", () => { seeking = false; });
  seek.addEventListener("input", () => {
    const dur = audio.duration || TRACK_DUR;
    audio.currentTime = (Number(seek.value) / 1000) * dur;
    timeCur.textContent = fmt(audio.currentTime);
  });
  vol.addEventListener("input", () => {
    audio.volume = Number(vol.value);
  });

  [playBtn, coverBtn, dockPlay, dockCover].forEach((btn) => {
    if (btn) btn.addEventListener("click", toggle);
  });
})();
