# ConnectApi

Paper-плагин (1.21.x): WebSocket-мост между веб-консолью/панелью и сервером.

## Сборка

```bash
mvn -q package
```

Jar: `target/connectapi.jar`. Положить в `plugins/`, перезапустить сервер.

## Настройка

`plugins/ConnectApi/config.yml`:

- `host` / `port` — адрес и порт WebSocket (по умолчанию `0.0.0.0:38710`). Порт должен быть открыт на хосте (Pterodactyl: выделить порт в панели).
- `accounts` — аккаунты панели/консоли: `ник: пароль`. Пустой пароль = вход без пароля.
- `allowed-commands` — префиксы команд, разрешённых с сайта. Пустой список = всё.
- `forward-chat` — слать игровой чат на сайт (`op: chat`).

## Аккаунты (на сервере)

```bash
/connectapi adduser <ник> <пароль>     # создать аккаунт
/connectapi passwd <ник> <пароль>      # сменить пароль
/connectapi removeuser <ник>           # удалить
/connectapi list                       # список аккаунтов
```

Вход на сайте идёт по нику. Ники `bogdemon` и `ZAYZER` по умолчанию без пароля.

## Возможности

- Поток консольных логов сервера (`op: log`) — через Log4J-appender.
- Выполнение команд от консоли (`op: command`).
- Статус сервера: онлайн, TPS, версия, игроки, миры (`op: status`, рассылается каждые 5 сек).
- Игровой чат (`op: chat`).
- Анонс для всех игроков (`op: broadcast`).
- Авторизация по нику+паролю (аккаунты создаются на сервере, `op: auth`).

## Протокол

Клиент → сервер:

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
{"op":"status","online":3,"max":20,"tps":19.8,"players":["a"],"worlds":["world"]}
{"op":"chat","player":"a","message":"привет"}
{"op":"cmd_result","cmd":"...","ok":true}
{"op":"error","message":"..."}
{"op":"pong"}
```
