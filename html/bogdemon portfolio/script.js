(() => {
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const DURATION = 32;

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

  const vis = $("#vis");
  if (vis) {
    vis.innerHTML = Array.from({ length: 28 }, () => "<i></i>").join("");
  }
  const visBars = vis ? $$("#vis i") : [];

  const fmt = (s) => {
    const m = Math.floor(s / 60);
    const sec = Math.floor(s % 60);
    return `${m}:${String(sec).padStart(2, "0")}`;
  };

  const player = {
    ctx: null,
    gain: null,
    analyser: null,
    source: null,
    buffer: null,
    playing: false,
    offset: 0,
    startedAt: 0,
    duration: DURATION,
    seeking: false,

    buildBuffer(ctx) {
      const sr = ctx.sampleRate;
      const len = Math.floor(sr * DURATION);
      const buffer = ctx.createBuffer(2, len, sr);
      for (let ch = 0; ch < 2; ch += 1) {
        const data = buffer.getChannelData(ch);
        let brown = 0;
        const pan = ch === 0 ? -0.15 : 0.15;
        for (let i = 0; i < len; i += 1) {
          const t = i / sr;
          const fade = Math.min(t / 1.4, (DURATION - t) / 1.4, 1);
          const pulse = 0.62 + 0.38 * Math.sin((Math.PI * 2 * t) / 11);
          const drone =
            Math.sin(Math.PI * 2 * (54.8 + pan) * t) * 0.2 +
            Math.sin(Math.PI * 2 * 82.4 * t + ch) * 0.1 +
            Math.sin(Math.PI * 2 * 110 * t) * 0.045 +
            Math.sin(Math.PI * 2 * 36.7 * t) * 0.12;
          brown = (brown + (Math.random() * 2 - 1) * 0.018) * 0.995;
          const crackle = Math.random() < 0.0009 ? (Math.random() - 0.5) * 0.09 : 0;
          data[i] = (drone * pulse + brown * 0.32 + crackle) * fade * 0.72;
        }
      }
      return buffer;
    },

    async ensure() {
      if (this.ctx) return;
      const Ctx = window.AudioContext || window.webkitAudioContext;
      this.ctx = new Ctx();
      this.gain = this.ctx.createGain();
      this.gain.gain.value = 0.55;
      this.analyser = this.ctx.createAnalyser();
      this.analyser.fftSize = 64;
      this.gain.connect(this.analyser);
      this.analyser.connect(this.ctx.destination);
      this.buffer = this.buildBuffer(this.ctx);
    },

    current() {
      if (!this.playing || !this.ctx) return this.offset;
      return (this.offset + (this.ctx.currentTime - this.startedAt)) % this.duration;
    },

    stopSource() {
      if (this.source) {
        try {
          this.source.stop();
        } catch {
          /* already stopped */
        }
        this.source.disconnect();
        this.source = null;
      }
    },

    startSource(at) {
      this.stopSource();
      const src = this.ctx.createBufferSource();
      src.buffer = this.buffer;
      src.loop = true;
      src.connect(this.gain);
      const offset = ((at % this.duration) + this.duration) % this.duration;
      src.start(0, offset);
      this.source = src;
      this.offset = offset;
      this.startedAt = this.ctx.currentTime;
    },

    async toggle() {
      await this.ensure();
      await this.ctx.resume();
      if (this.playing) {
        this.offset = this.current();
        this.stopSource();
        this.playing = false;
      } else {
        this.startSource(this.offset);
        this.playing = true;
      }
      document.body.classList.toggle("is-playing", this.playing);
      $$("[data-play]").forEach((btn) => {
        btn.setAttribute("aria-pressed", String(this.playing));
        btn.setAttribute("aria-label", this.playing ? "Пауза Ember Veil" : "Играть Ember Veil");
      });
    },

    async seek(at) {
      this.offset = Math.max(0, Math.min(this.duration, at));
      if (this.playing) {
        await this.ensure();
        this.startSource(this.offset);
      }
      this.paint(this.offset);
    },

    setVolume(v) {
      const val = Number(v) / 100;
      if (this.gain) this.gain.gain.value = val;
      $$("[data-vol]").forEach((el) => {
        el.value = String(v);
        el.style.setProperty("--pos", `${v}%`);
      });
    },

    paint(t) {
      const pos = `${(t / this.duration) * 100}%`;
      $$("[data-seek]").forEach((el) => {
        if (!this.seeking) el.value = String(t);
        el.style.setProperty("--pos", pos);
      });
      $$("[data-now]").forEach((el) => {
        el.textContent = fmt(t);
      });
    },
  };

  $$("[data-play]").forEach((btn) => {
    btn.addEventListener("click", () => player.toggle());
  });

  $$("[data-seek]").forEach((el) => {
    el.addEventListener("pointerdown", () => {
      player.seeking = true;
    });
    el.addEventListener("input", () => {
      el.style.setProperty("--pos", `${(Number(el.value) / DURATION) * 100}%`);
    });
    el.addEventListener("change", () => {
      player.seeking = false;
      player.seek(Number(el.value));
    });
  });

  $$("[data-vol]").forEach((el) => {
    el.addEventListener("input", () => player.setVolume(el.value));
  });
  player.setVolume(55);

  const freq = new Uint8Array(32);

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

    const t = player.seeking ? Number($("[data-seek]").value) : player.current();
    player.paint(t);

    if (player.analyser && visBars.length) {
      player.analyser.getByteFrequencyData(freq);
      visBars.forEach((bar, i) => {
        const n = freq[i] || 0;
        const hgt = player.playing ? Math.max(12, (n / 255) * 100) : 16;
        bar.style.height = `${hgt}%`;
      });
    }

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
  } else {
    const loop = () => {
      player.paint(player.seeking ? Number($("[data-seek]").value) : player.current());
      requestAnimationFrame(loop);
    };
    loop();
  }
})();
