# Duels Web Console & Control Panel

Веб-консоль и панель управления сервером через плагин **ConnectApi** (`java/plugins/connectapi`).

| Файл | Назначение |
| --- | --- |
| `index.html` | Консоль: логи сервера + ввод команд |
| `panel/index.html` | Панель управления (для `cp.spacetrace.sryze.cc`) с простым входом по токену |
| `CNAME` | Привязка GitHub Pages к домену `spacetrace.sryze.cc` |

Сайт — статический (чистый HTML/JS), хостится бесплатно на GitHub Pages, подключается к серверу по **WebSocket** (`ws://`).

---

## 1. Сборка и установка плагина на сервер

```bash
cd java/plugins/connectapi
mvn -q package
```

Результат: `target/connectapi.jar`. Положите его в папку `plugins` сервера и перезапустите сервер (или используйте PlReloader).

Настройки — `plugins/ConnectApi/config.yml`:

```yaml
host: 0.0.0.0          # слушать на всех интерфейсах
port: 25570            # порт WebSocket
token: change-me       # ОБЯЗАТЕЛЬНО поменяйте!
allowed-commands:
  - say
  - broadcast
  - whitelist
  - kick
  - ban
  - duels
```

- **Порт 25570 нужно открыть** в фаерволе сервера и пробросить в роутере.
- В `allowed-commands` — команды, которые можно запускать с сайта. Пустой список = разрешены все (опасно!).

---

## 2. Загрузка сайта на GitHub Pages

Рекомендуется **отдельный репозиторий** под сайт (GitHub Pages отдаёт корень репозитория, а не подпапку `html/duels`):

```bash
cd html/duels
git init
git add -A
git commit -m "Duels console + control panel"
git branch -M main
git remote add origin git@github.com:ВАШ_ЛОГИН/duels-site.git
git push -u origin main
```

В GitHub: **Settings → Pages → Source: Deploy from a branch → main → / (root) → Save**.

Через пару минут сайт откроется по адресу `https://ВАШ_ЛОГИН.github.io/duels-site/`.

> Если оставляете сайт в этом репозитории `mycode`, он будет доступен по пути `/html/duels/`. Для чистого домена удобнее отдельный репозиторий.

---

## 3. Привязка домена через Cloudflare DNS

Вы хостите домен `spacetrace.sryze.cc` на Cloudflare. Нужно связать GitHub Pages и домен.

### 3.1 spacetrace.sryze.cc → консоль

В Cloudflare создайте CNAME-запись:

```
Тип:  CNAME
Имя:  spacetrace
Цель: <ВАШ_ЛОГИН>.github.io
```

> Облако (proxy) можно включить: GitHub Pages отдаёт страницу по HTTP/HTTPS, Cloudflare пропустит.

Затем в GitHub: **Settings → Pages → Custom domain → `spacetrace.sryze.cc` → Save** (GitHub сам проверит DNS и выпустит SSL).

Файл `CNAME` в корне сайта тоже можно добавить — GitHub подхватит его автоматически:

```
spacetrace.sryze.cc
```

### 3.2 cp.spacetrace.sryze.cc → панель управления

Два варианта:

**Вариант А — отдельный репозиторий (рекомендую):**
1. Создайте репозиторий `cp` и залейте в него содержимое `panel/` (только `index.html`).
2. Включите Pages, укажите Custom domain `cp.spacetrace.sryze.cc`.
3. В Cloudflare: `CNAME  cp  →  <ВАШ_ЛОГИН>.github.io`.

**Вариант Б — редирект через Cloudflare:**
1. Один репозиторий `duels-site` отдаёт и консоль, и панель: панель доступна как `spacetrace.sryze.cc/panel/`.
2. В Cloudflare **Rules → Redirect Rules** добавьте правило: Hostname `cp.spacetrace.sryze.cc` → redirect `https://spacetrace.sryze.cc/panel/` (301).
3. В DNS создайте запись `CNAME cp → spacetrace.sryze.cc` (или A-запись на Cloudflare) без проксирования.

### 3.3 WebSocket (важно!)

Сайт подключается к серверу по `ws://`. Способы зависят от того, где крутится сервер.

**Pterodactyl (панель, без SSH) — рекомендую:**

Хостинг уже даёт публичные порты, пробрасывать роутер не нужно.

1. В панели Pterodactyl: **Settings (Network) → Ports → выделите дополнительный порт**, например `25570` (сохранить + перезапустить сервер).
2. В `plugins/ConnectApi/config.yml`: `host: 0.0.0.0`, `port: 25570`.
3. Узнайте IP ноды — он показан в панели рядом с портом (обычно `IP-ноды:25565`).
4. В консоли/панели указывайте: `ws://IP-ноды:25570`.

Для домена `play.spacetrace.sryze.cc`: в Cloudflare создайте `A  play  →  IP-ноды` (облако **серое**, DNS only — прокси Cloudflare не пропускает нестандартные порты), и подключайтесь как `ws://play.spacetrace.sryze.cc:25570`.

**Вариант 2 — Cloudflare Tunnel (если есть SSH на сервере):**

Плагин слушает только на `127.0.0.1` (`host: 127.0.0.1`), наружу сервер сам ходит к Cloudflare — портов открывать не нужно вообще.

1. Установите `cloudflared` на сервер: `cloudflared tunnel login`, затем `cloudflared tunnel create play`.
2. `~/.cloudflared/config.yml`:
   ```yaml
   tunnel: play
   credentials-file: /root/.cloudflared/<ID-туннеля>.json
   ingress:
     - hostname: play.spacetrace.sryze.cc
       service: ws://127.0.0.1:25570
     - service: http_status:404
   ```
3. В DNS: `CNAME  play  →  <ID-туннеля>.cfargotunnel.com`.
4. Запуск: `cloudflared tunnel run play`.
5. В консоли/панели указывайте: `wss://play.spacetrace.sryze.cc` (без порта, TLS даёт Cloudflare).

**Вариант 3 — обычный сервер у себя дома:**

`host: 0.0.0.0`, открыть порт 25570 в фаерволе и пробросить в роутере; `A  play  →  домашний_IP` (серое облако).

---

## 4. Проверка

1. Запустите сервер с плагином ConnectApi.
2. Откройте консоль и укажите `ws://play.spacetrace.sryze.cc:25570` + токен.
3. Панель управления — на `cp.spacetrace.sryze.cc`, вход по токену.

## 5. Протокол (коротко)

Клиент → сервер (JSON):

```json
{"op":"auth","token":"..."}
{"op":"command","cmd":"say hi"}
{"op":"status"}
{"op":"broadcast","message":"text"}
{"op":"ping"}
```

Сервер → клиент:

```json
{"op":"hello",...}
{"op":"auth","ok":true}
{"op":"log","level":"INFO","line":"..."}
{"op":"status","online":3,"max":20,"tps":19.8,"players":["a","b"],"worlds":["world"],...}
{"op":"chat","player":"a","message":"привет"}
{"op":"cmd_result","cmd":"...","ok":true}
{"op":"error","message":"..."}
{"op":"pong"}
```
