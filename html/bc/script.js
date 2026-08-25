const WEBHOOKS = [
  // Впиши сюда свои вебхуки (имя + ссылка). В самом сайте их добавить или удалить нельзя.
  { name: "Основной", url: "" },
  { name: "Резервный", url: "" },
];

const RIGHTS = ["text", "image", "admin"];

const EMOJI_GROUPS = [
  {
    name: "Смайлы",
    list: "😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 😉 😊 😇 🥰 😍 🤩 😘 😗 😚 😙 🥲 😋 😛 😜 🤪 😝 🤑 🤗 🤭 🤫 🤔 🤐 🤨 😐 😑 😶 😏 😒 🙄 😬 😮‍💨 🤥 😌 😔 😪 🤤 😴 😷 🤒 🤕 🤢 🤮 🤧 🥵 🥶 🥴 😵 🤯 🤠 🥳 🥸 😎 🤓 🧐 😕 😟 🙁 😮 😯 😲 😳 🥺 😦 😧 😨 😰 😥 😢 😭 😱 😖 😣 😞 😓 😩 😫 🥱 😤 😡 😠 🤬 😈 👿 💀 ☠️ 💩 🤡 👹 👺 👻 👽 🤖".split(" "),
  },
  {
    name: "Жесты",
    list: "👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✍️ 💅 🤳 💪 🦾 🦿 🦵 🦶 👂 🦻 👃 🧠 🫀 🫁 🦷 🦴 👀 👁️ 👅 👄 💋 🩸".split(" "),
  },
  {
    name: "Животные",
    list: "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐒 🐔 🐧 🐦 🐤 🐣 🐥 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🕷️ 🦂 🐢 🐍 🦎 🦖 🦕 🐙 🦑 🦐 🦞 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍 🦧 🐘 🦛 🦏 🐪 🦒 🦘 🐃 🐂 🐄 🐎 🐖 🐏 🐑 🦙 🐐 🦌 🐕 🐩 🦮 🐈 🐓 🦃 🦚 🦜 🦢 🕊️ 🐇 🦝 🦨 🦡 🦫 🦦 🦥 🐁 🐀 🦔".split(" "),
  },
  {
    name: "Еда",
    list: "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🌽 🥕 🧄 🧅 🥔 🍠 🥐 🥯 🍞 🥖 🥨 🧀 🥚 🍳 🧈 🥞 🧇 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🥙 🧆 🌮 🌯 🥗 🥘 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🦪 🍤 🍙 🍚 🍘 🍥 🥠 🥮 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🌰 🥜 🍯 🥛 🍼 ☕ 🍵 🧃 🥤 🧋 🍶 🍺 🍻 🥂 🍷 🥃 🍸 🍹 🧉 🍾 🧊".split(" "),
  },
  {
    name: "Спорт",
    list: "⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🪀 🏓 🏸 🏒 🏑 🥍 🏏 🥊 🥋 🎽 🛹 🛼 🛷 ⛸️ 🥌 🎿 ⛷️ 🏂 🏋️ 🤼 🤸 ⛹️ 🤺 🤾 🏌️ 🏇 🧘 🏄 🏊 🤽 🚣 🧗 🚵 🚴 🏆 🥇 🥈 🥉 🏅 🎖️ 🏵️ 🎗️ 🎫 🎟️ 🎪".split(" "),
  },
  {
    name: "Путешествия",
    list: "🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🚚 🚛 🚜 🛴 🚲 🛵 🏍️ 🛺 🚨 🚔 🚍 🚘 🚖 🚡 🚠 🚟 🚃 🚋 🚞 🚝 🚄 🚅 🚈 🚂 🚆 🚇 🚊 🚉 ✈️ 🛫 🛬 🛩️ 💺 🛰️ 🚀 🛸 🚁 🛶 ⛵ 🚤 🛥️ 🛳️ ⛴️ 🚢 ⚓ 🪝 ⛽ 🚧 🚦 🚥 🗺️ 🗿 🗽 🗼 🏰 🏯 🏟️ 🎡 🎢 🎠 ⛲ ⛱️ 🏖️ 🏝️ 🏜️ 🌋 ⛰️ 🏔️ 🗻 🏕️ ⛺ 🏠 🏡 🏘️ 🏗️ 🏭 🏢 🏬 🏣 🏤 🏥 🏦 🏨 🏪 🏫 🏩 💒 🏛️ ⛪ 🕌 🛕 ⛩️ 🌁 🌃 🏙️ 🌄 🌅 🌆 🌇 🌉".split(" "),
  },
  {
    name: "Объекты",
    list: "⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💽 💾 💿 📀 📼 📷 📸 📹 🎥 📞 ☎️ 📟 📠 📺 📻 🎙️ 🎚️ 🎛️ 🧭 ⏱️ ⏲️ ⏰ 🕰️ ⌛ ⏳ 📡 🔋 🔌 💡 🔦 🕯️ 🧯 🛢️ 💸 💵 💴 💶 💷 🪙 💰 💳 💎 ⚖️ 🪜 🧰 🔧 🔨 ⚒️ 🛠️ ⛏️ 🔩 ⚙️ 🧱 ⛓️ 🧲 🔫 💣 🧨 🪓 🔪 🗡️ ⚔️ 🛡️ 🚬 ⚰️ 🪦 ⚱️ 🏺 🔮 📿 🧿 ⚗️ 🔭 🔬 🕳️ 💊 💉 🧬 🦠 🧫 🧪 🌡️ 🧹 🧺 🧻 🚽 🚰 🚿 🛁 🛀 🧼 🪒 🧽 🧴 🛎️ 🔑 🗝️ 🚪 🪑 🛋️ 🛏️ 🛌 🧸 🖼️ 🛍️ 🛒 🎁 🎈 🎏 🎀 🎊 🎉 🪄 🪅".split(" "),
  },
  {
    name: "Символы",
    list: "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ✝️ ☪️ 🕉️ ☸️ ✡️ 🔯 🕎 ☯️ ☦️ ⛎ ♈ ♉ ♊ ♋ ♌ ♍ ♎ ♏ ♐ ♑ ♒ ♓ 🆔 ⚛️ 🉑 ☢️ ☣️ 📴 📳 ✴️ 🆚 💯 🔥 ✨ 🌟 ⭐ 💫 💥 💢 ❄️ 🌈 ☀️ ⛅ ☁️ 🌧️ ⛈️ 🌩️ 🌨️ ☃️ ⛄ 🌬️ 💨 🌪️ 🌫️ 🌊 💧 💦 ☔ ✅ ❌ ❓ ❗ ‼️ ⁉️ 💬 🗯️ 💭 🔔 🔕 ♻️ 🔱 📴 🆕 🆗 🆒 🆙 🔝 🈁 ⚠️ 🚸 🔰 ⛔ 🚫 💹 🅰️ 🅱️ 🆎 🅾️ 🆘".split(" "),
  },
];

const LS = { admin: "bc_admin_code", codes: "bc_codes", version: "bc_session_version" };
const SS = { session: "bc_session", version: "bc_session_version" };

const $ = (id) => document.getElementById(id);

const loginCard = $("loginCard");
const messageCard = $("messageCard");
const adminCard = $("adminCard");
const codeInput = $("codeInput");
const loginBtn = $("loginBtn");
const loginStatus = $("loginStatus");
const webhookSelect = $("webhookSelect");
const textInput = $("textInput");
const sendTextBtn = $("sendText");
const sendImageBtn = $("sendImage");
const imageInput = $("imageInput");
const statusEl = $("status");
const newCodeInput = $("newCodeInput");
const newRightsInput = $("newRightsInput");
const addCodeBtn = $("addCodeBtn");
const codesList = $("codesList");
const kickAllBtn = $("kickAllBtn");
const adminStatus = $("adminStatus");
const whoami = $("whoami");
const logoutBtn = $("logoutBtn");
const emojiToggle = $("emojiToggle");
const emojiPanel = $("emojiPanel");

let adminCode = localStorage.getItem(LS.admin);
let session = null;

function randomCode(len = 8) {
  const abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const arr = crypto.getRandomValues(new Uint32Array(len));
  return Array.from(arr, (n) => abc[n % abc.length]).join("");
}

function getCodes() {
  try {
    return JSON.parse(localStorage.getItem(LS.codes)) || [];
  } catch {
    return [];
  }
}

function setCodes(list) {
  localStorage.setItem(LS.codes, JSON.stringify(list));
}

function parseRights(str) {
  return String(str || "")
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter((r) => RIGHTS.includes(r));
}

function hasRight(r) {
  if (!session) return false;
  if (session.admin) return true;
  return session.rights.includes(r);
}

function setStatus(el, text, type) {
  el.textContent = text;
  el.className = "status" + (type ? " " + type : "");
}

function startSession(s) {
  session = s;
  sessionStorage.setItem(SS.session, JSON.stringify(s));
  sessionStorage.setItem(SS.version, localStorage.getItem(LS.version) || "0");
  applySession();
}

function endSession(message) {
  session = null;
  sessionStorage.removeItem(SS.session);
  sessionStorage.removeItem(SS.version);
  applySession();
  if (message) setStatus(loginStatus, message, "err");
}

function applySession() {
  loginCard.classList.toggle("hidden", !!session);
  messageCard.classList.toggle("hidden", !session);
  adminCard.classList.toggle("hidden", !(session && session.admin));
  logoutBtn.classList.toggle("hidden", !session);
  whoami.textContent = session
    ? session.code + " · " + (session.admin ? "админ" : session.rights.join(", "))
    : "";
  sendTextBtn.classList.toggle("hidden", !hasRight("text"));
  sendImageBtn.classList.toggle("hidden", !hasRight("image"));
  if (session) {
    fillWebhooks();
    textInput.value = "";
    setStatus(statusEl, "");
  }
  if (session && session.admin) {
    renderCodes();
    setStatus(adminStatus, "");
  }
}

function login() {
  const code = codeInput.value.trim().toUpperCase();
  if (!code) return setStatus(loginStatus, "Введите код", "err");
  if (code === adminCode) {
    codeInput.value = "";
    return startSession({ code, admin: true, rights: RIGHTS });
  }
  const found = getCodes().find((c) => c.code === code);
  if (!found) return setStatus(loginStatus, "Неверный код", "err");
  codeInput.value = "";
  startSession({ code: found.code, admin: false, rights: parseRights(found.rights) });
}

function fillWebhooks() {
  webhookSelect.innerHTML = "";
  WEBHOOKS.forEach((w, i) => {
    const o = document.createElement("option");
    o.value = String(i);
    o.textContent = w.name;
    webhookSelect.appendChild(o);
  });
}

function currentWebhook() {
  return WEBHOOKS[Number(webhookSelect.value)] || WEBHOOKS[0];
}

function renderCodes() {
  codesList.innerHTML = "";
  const list = getCodes();
  if (!list.length) {
    const empty = document.createElement("div");
    empty.className = "codes-empty";
    empty.textContent = "Кодов пока нет";
    codesList.appendChild(empty);
    return;
  }
  list.forEach((c) => {
    const row = document.createElement("div");
    row.className = "code-row";

    const codeEl = document.createElement("span");
    codeEl.className = "code";
    codeEl.textContent = c.code;

    const rightsEl = document.createElement("span");
    rightsEl.className = "rights";
    rightsEl.textContent = parseRights(c.rights).join(", ") || "без прав";

    const del = document.createElement("button");
    del.className = "button danger tiny";
    del.textContent = "удалить";
    del.addEventListener("click", () => deleteCode(c.code));

    row.append(codeEl, rightsEl, del);
    codesList.appendChild(row);
  });
}

function addCode() {
  const code = newCodeInput.value.trim().toUpperCase();
  const rights = parseRights(newRightsInput.value);
  if (!code) return setStatus(adminStatus, "Введите код", "err");
  if (!rights.length) return setStatus(adminStatus, "Укажите права: text, image", "err");
  if (code === adminCode || getCodes().some((c) => c.code === code))
    return setStatus(adminStatus, "Такой код уже есть", "err");
  const list = getCodes();
  list.push({ code, rights: rights.join(",") });
  setCodes(list);
  newCodeInput.value = "";
  newRightsInput.value = "";
  renderCodes();
  setStatus(adminStatus, "Код добавлен", "ok");
}

function deleteCode(code) {
  setCodes(getCodes().filter((c) => c.code !== code));
  if (session && !session.admin && session.code === code) {
    endSession("Код удалён администратором");
  } else {
    renderCodes();
    setStatus(adminStatus, "Код удалён", "ok");
  }
}

function kickAll() {
  localStorage.setItem(LS.version, String(Date.now()));
  endSession("Все сессии отключены");
}

function checkVersion() {
  if (!session) return;
  if ((localStorage.getItem(LS.version) || "0") !== sessionStorage.getItem(SS.version)) {
    return endSession("Сессия отключена администратором");
  }
  if (!session.admin && !getCodes().some((c) => c.code === session.code)) {
    endSession("Код удалён администратором");
  }
}

function setBusy(busy) {
  sendTextBtn.disabled = busy;
  sendImageBtn.disabled = busy;
}

let activeEmojiGroup = 0;

function renderEmojiPanel() {
  emojiPanel.innerHTML = "";

  const tabs = document.createElement("div");
  tabs.className = "emoji-tabs";
  EMOJI_GROUPS.forEach((g, i) => {
    const tab = document.createElement("button");
    tab.className = "emoji-tab" + (i === activeEmojiGroup ? " active" : "");
    tab.textContent = g.name;
    tab.addEventListener("click", () => {
      activeEmojiGroup = i;
      renderEmojiPanel();
    });
    tabs.appendChild(tab);
  });
  emojiPanel.appendChild(tabs);

  const grid = document.createElement("div");
  grid.className = "emoji-grid";
  EMOJI_GROUPS[activeEmojiGroup].list.forEach((em) => {
    const btn = document.createElement("button");
    btn.className = "emoji-btn";
    btn.textContent = em;
    btn.addEventListener("click", () => insertEmoji(em));
    grid.appendChild(btn);
  });
  emojiPanel.appendChild(grid);
}

function insertEmoji(em) {
  const start = textInput.selectionStart ?? textInput.value.length;
  const end = textInput.selectionEnd ?? start;
  textInput.value = textInput.value.slice(0, start) + em + textInput.value.slice(end);
  const pos = start + em.length;
  textInput.focus();
  textInput.setSelectionRange(pos, pos);
}

emojiToggle.addEventListener("click", () => {
  emojiPanel.classList.toggle("hidden");
  if (!emojiPanel.classList.contains("hidden")) renderEmojiPanel();
});

async function sendWebhook(url, options) {
  const res = await fetch(url, { method: "POST", ...options });
  if (res.status === 429) throw new Error("Слишком часто. Подожди немного");
  if (!res.ok) throw new Error("Ошибка отправки (" + res.status + ")");
}

async function sendText() {
  const content = textInput.value.trim();
  if (!content) return setStatus(statusEl, "Введите текст", "err");
  const url = currentWebhook().url;
  if (!url) return setStatus(statusEl, "Вебхук не настроен", "err");
  setBusy(true);
  setStatus(statusEl, "Отправка...");
  try {
    await sendWebhook(url, {
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
    });
    textInput.value = "";
    setStatus(statusEl, "Отправлено", "ok");
  } catch (e) {
    setStatus(statusEl, e.message.includes("Failed to fetch") ? "Нет соединения" : e.message, "err");
  }
  setBusy(false);
}

async function sendWithImage(file) {
  if (!file) return;
  if (!file.type.startsWith("image/")) return setStatus(statusEl, "Это не картинка", "err");
  if (file.size > 8 * 1024 * 1024) return setStatus(statusEl, "Файл больше 8 МБ", "err");
  const url = currentWebhook().url;
  if (!url) return setStatus(statusEl, "Вебхук не настроен", "err");
  setBusy(true);
  setStatus(statusEl, "Отправка...");
  try {
    const fd = new FormData();
    fd.append("payload_json", JSON.stringify({ content: textInput.value.trim() }));
    fd.append("files[0]", file, file.name);
    await sendWebhook(url, { body: fd });
    textInput.value = "";
    setStatus(statusEl, "Отправлено с картинкой", "ok");
  } catch (e) {
    setStatus(statusEl, e.message.includes("Failed to fetch") ? "Нет соединения" : e.message, "err");
  }
  setBusy(false);
}

loginBtn.addEventListener("click", login);
codeInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") login();
});

logoutBtn.addEventListener("click", () => endSession());

sendTextBtn.addEventListener("click", sendText);
textInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendText();
  }
});

sendImageBtn.addEventListener("click", () => imageInput.click());
imageInput.addEventListener("change", () => {
  const f = imageInput.files[0];
  imageInput.value = "";
  if (f) sendWithImage(f);
});

addCodeBtn.addEventListener("click", addCode);
newCodeInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") addCode();
});
newRightsInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") addCode();
});
kickAllBtn.addEventListener("click", kickAll);

window.addEventListener("storage", (e) => {
  if (e.key === LS.version || e.key === LS.codes) checkVersion();
});
window.addEventListener("focus", checkVersion);

if (!adminCode) {
  adminCode = randomCode();
  localStorage.setItem(LS.admin, adminCode);
}

console.log(
  "%c MCRLSMP %c Код администратора: " + adminCode + " ",
  "background:linear-gradient(135deg,#37d45e,#0f7a2b);color:#fff;font-size:18px;font-weight:bold;padding:6px 14px;border-radius:10px 0 0 10px;",
  "background:#222;color:#7dff9a;font-size:18px;font-weight:bold;padding:6px 14px;border-radius:0 10px 10px 0;"
);

const saved = sessionStorage.getItem(SS.session);
if (saved) {
  try {
    session = JSON.parse(saved);
    checkVersion();
  } catch {
    session = null;
  }
}
applySession();
