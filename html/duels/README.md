# Duels Web Console & Control Panel

Главная страница сервера, веб-консоль и панель управления через плагин **ConnectApi** (`java/plugins/connectapi`).

| Файл | Назначение |
| --- | --- |
| `index.html` | Главная страница сервера в стиле SpaceTrace (анимированный космос, Discord-виджет) |
| `panel/index.html` | Панель управления (для `cp.spacetrace.sryze.cc`) с входом по нику+паролю |
| `panel/console/index.html` | Консоль сервера (для `cp.spacetrace.sryze.cc/console/`) |
| `config.js` | Настройки сайта (адрес WebSocket) |
| `cloudflare-dns.txt` | DNS-записи для импорта в Cloudflare |
| `CNAME` | Привязка GitHub Pages к домену `spacetrace.sryze.cc` |

Сайт — статический (чистый HTML/JS), хостится бесплатно на GitHub Pages, подключается к серверу по **WebSocket** (`ws://`).

---

## 0. Аккаунты (вход по нику)

Вход на панель/консоль идёт по нику. Аккаунты создаются **на сервере**:

```bash
/connectapi adduser <ник> <пароль>     # создать аккаунт
/connectapi passwd <ник> <пароль>      # сменить пароль
/connectapi removeuser <ник>           # удалить
/connectapi list                       # список
```

- Ники `bogdemon` и `ZAYZER` уже есть в `config.yml` с пустым паролем — входят без пароля.
- Пароль в `config.js` НЕ храните — сайт публичный.

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

Панель лежит в **отдельном репозитории** `Semin696/spacetrace-cp` (создан автоматически, Pages включены).

1. В репозитории уже лежит `CNAME` с `cp.spacetrace.sryze.cc`.
2. В Cloudflare DNS: `CNAME  cp  →  Semin696.github.io`.
3. После проверки DNS панель откроется по адресу `https://cp.spacetrace.sryze.cc`.

> Если когда-нибудь захотите отдавать панель в том же репозитории — включите в Cloudflare Redirect Rule: `cp.spacetrace.sryze.cc` → `https://spacetrace.sryze.cc/panel/`.

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
{"op":"auth","nick":"bogdemon","password":"..."}
{"op":"command","cmd":"say hi"}
{"op":"status"}
{"op":"broadcast","message":"text"}
{"op":"ping"}
```

Сервер → клиент:

```json
{"op":"hello",...}
{"op":"auth","ok":true,"nick":"bogdemon"}
{"op":"log","level":"INFO","line":"..."}
{"op":"status","online":3,"max":20,"tps":19.8,"players":["a","b"],"worlds":["world"],...}
{"op":"chat","player":"a","message":"привет"}
{"op":"cmd_result","cmd":"...","ok":true}
{"op":"error","message":"..."}
{"op":"pong"}
```

---

## 6. Почему сайт не работает?

**Сайт открывается, но консоль пишет «Не удаётся подключиться»:**
1. Проверьте `config.js`: адрес должен быть `ws://f1.rustix.me:38710` (или IP:порт), а не `localhost`.
2. Порт 38710 должен быть выделен в панели Pterodactyl и открыт на TCP.
3. Токен в плагине (`plugins/ConnectApi/config.yml`) должен совпадать с вводимым.
4. Браузер должен открывать сайт по `https://` — WebSocket из https-страницы обязан быть `wss://` или `ws://` (некоторые браузеры блокируют смешанный контент `ws://` со страницы `https://`). Если так — в `config.js` укажите `wss://`, а перед плагином поставьте прокси с TLS (Cloudflare Tunnel), либо откройте консоль по HTTP.

**Страница «404 / сайт не собирается»:**
1. Проверьте, что в GitHub включён Pages: **Settings → Pages → Deploy from a branch → main → / (root)**.
2. Если в репозитории лежит `CNAME`, но DNS на домен ещё не настроен — GitHub может не собрать сайт. Временно удалите `CNAME`, дождитесь публикации на `Semin696.github.io/mainsite/`, а домен добавляйте после настройки DNS.
