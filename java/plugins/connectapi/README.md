# ConnectApi

Paper-плагин (1.21.x): WebSocket-мост между веб-консолью/панелью и сервером.

## Сборка

```bash
mvn -q package
```

Jar: `target/connectapi.jar`. Положить в `plugins/`, перезапустить сервер.

## Настройка

`plugins/ConnectApi/config.yml`:

- `host` / `port` — адрес и порт WebSocket (по умолчанию `127.0.0.1:25570`). Если нужен доступ извне — поставьте `0.0.0.0` и откройте порт, либо используйте Cloudflare Tunnel (без открытых портов).
- `token` — секрет для входа с сайта. **Поменяйте.**
- `allowed-commands` — префиксы команд, разрешённых с сайта. Пустой список = всё.
- `forward-chat` — слать игровой чат на сайт (`op: chat`).

## Возможности

- Поток консольных логов сервера (`op: log`) — через Log4J-appender.
- Выполнение команд от консоли (`op: command`).
- Статус сервера: онлайн, TPS, версия, игроки, миры (`op: status`, рассылается каждые 5 сек).
- Игровой чат (`op: chat`).
- Анонс для всех игроков (`op: broadcast`).
- Авторизация по токену (`op: auth`).

## Протокол

Клиент → сервер:

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
{"op":"status","online":3,"max":20,"tps":19.8,"players":["a"],"worlds":["world"]}
{"op":"chat","player":"a","message":"привет"}
{"op":"cmd_result","cmd":"...","ok":true}
{"op":"error","message":"..."}
{"op":"pong"}
```
