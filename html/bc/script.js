const WEBHOOK_KEY = "bc_webhook_url";

const webhookCard = document.getElementById("webhookCard");
const messageCard = document.getElementById("messageCard");
const webhookInput = document.getElementById("webhookInput");
const webhookNext = document.getElementById("webhookNext");
const textInput = document.getElementById("textInput");
const sendTextBtn = document.getElementById("sendText");
const sendImageBtn = document.getElementById("sendImage");
const imageInput = document.getElementById("imageInput");
const statusEl = document.getElementById("status");
const changeWebhookBtn = document.getElementById("changeWebhook");

let webhookUrl = localStorage.getItem(WEBHOOK_KEY) || "";

function isValidWebhook(url) {
  return /^https:\/\/(canary\.|ptb\.)?(discord|discordapp)\.com\/api\/webhooks\/\d+\/[\w-]+$/.test(
    url.trim()
  );
}

function showWebhookScreen() {
  webhookCard.classList.remove("hidden");
  messageCard.classList.add("hidden");
  webhookInput.value = webhookUrl;
  webhookInput.focus();
}

function showMessageScreen() {
  webhookCard.classList.add("hidden");
  messageCard.classList.remove("hidden");
  setStatus("");
}

function setStatus(text, type) {
  statusEl.textContent = text;
  statusEl.className = "status" + (type ? " " + type : "");
}

function saveWebhook() {
  const url = webhookInput.value.trim();
  if (!isValidWebhook(url)) {
    setStatus("Неверная ссылка на вебхук", "err");
    return;
  }
  webhookUrl = url;
  localStorage.setItem(WEBHOOK_KEY, webhookUrl);
  setStatus("");
  showMessageScreen();
}

async function sendWebhook(options) {
  const res = await fetch(webhookUrl, { method: "POST", ...options });
  if (res.status === 429) {
    throw new Error("Слишком часто. Подожди немного");
  }
  if (!res.ok) {
    throw new Error("Ошибка отправки (" + res.status + ")");
  }
}

async function sendText() {
  const content = textInput.value.trim();
  if (!content) {
    setStatus("Введите текст", "err");
    return;
  }
  setBusy(true);
  setStatus("Отправка...");
  try {
    await sendWebhook({
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
    });
    textInput.value = "";
    setStatus("Отправлено", "ok");
  } catch (e) {
    setStatus(e.message.includes("Failed to fetch") ? "Нет соединения" : e.message, "err");
  }
  setBusy(false);
}

async function sendWithImage(file) {
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    setStatus("Это не картинка", "err");
    return;
  }
  if (file.size > 8 * 1024 * 1024) {
    setStatus("Файл больше 8 МБ", "err");
    return;
  }
  setBusy(true);
  setStatus("Отправка...");
  try {
    const fd = new FormData();
    fd.append(
      "payload_json",
      new Blob([JSON.stringify({ content: textInput.value.trim() })], {
        type: "application/json",
      })
    );
    fd.append("files[0]", file, file.name);
    await sendWebhook({ body: fd });
    textInput.value = "";
    setStatus("Отправлено с картинкой", "ok");
  } catch (e) {
    setStatus(e.message.includes("Failed to fetch") ? "Нет соединения" : e.message, "err");
  }
  setBusy(false);
}

function setBusy(busy) {
  sendTextBtn.disabled = busy;
  sendImageBtn.disabled = busy;
}

webhookNext.addEventListener("click", saveWebhook);
webhookInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") saveWebhook();
});

sendTextBtn.addEventListener("click", sendText);

textInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendText();
  }
});

sendImageBtn.addEventListener("click", () => imageInput.click());

imageInput.addEventListener("change", () => {
  const file = imageInput.files[0];
  imageInput.value = "";
  if (file) sendWithImage(file);
});

changeWebhookBtn.addEventListener("click", () => {
  localStorage.removeItem(WEBHOOK_KEY);
  webhookUrl = "";
  showWebhookScreen();
});

if (webhookUrl && isValidWebhook(webhookUrl)) {
  showMessageScreen();
} else {
  showWebhookScreen();
}
