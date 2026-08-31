# MoonLight Visual — сайт с бэкендом Node.js + MySQL

Сайт полностью переведён с localStorage на облачную базу данных MySQL
(SQL Free Database: `sql7.freesqldatabase.com`).

## Структура

```
MoonLight Site/
├── server/                  # бэкенд
│   ├── server.js            # Express-сервер + API-роуты
│   ├── db.js                # пул подключения MySQL
│   ├── db.sql               # схема базы данных
│   ├── apply-db.js          # применяет схему к БД (node server/apply-db.js)
│   └── seed.js              # заполняет стартовые промокоды
├── api.js                   # фронтенд-клиент API (fetch + токен)
├── index.html / profile.html
├── script.js / codes.js / profile.js / starfield.js / styles.css
├── package.json
└── .env                     # конфигурация БД (НЕ коммитится)
```

## Локальный запуск

1. Установите зависимости:
   ```bash
   npm install
   ```

2. Создайте файл `.env` в корне со своими данными:
   ```
   DB_HOST=sql7.freesqldatabase.com
   DB_PORT=3306
   DB_NAME=sql7836246
   DB_USER=sql7836246
   DB_PASSWORD=ваш_пароль
   PORT=3000
   ```

3. Примените схему базы данных (один раз):
   ```bash
   node server/apply-db.js
   ```
   Это создаст таблицы `users`, `sessions`, `codes`, `redeemed_codes`, `owners`
   и зальёт стартовые промокоды `MOONLIGHT2026`, `VISUALPRO`, `MINECRAFT`,
   `STARLIGHT`, `BLOCKCRAFT`, `NIGHTOWL`.

4. Запустите сервер:
   ```bash
   npm start
   ```
   Сайт будет доступен на http://localhost:3000

## Коды (мастер-код владельца)

- Промокод владельца: `MOONLIGHTOWNER` — даёт доступ к панели управления кодами.
- Владелец может создавать и удалять промокоды через профиль.

## Развёртывание на Node.js-хостинге (Render/Railway/VPS)

1. Задайте переменные окружения на хостинге (команды запуска/окружение):
   `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `PORT`.
2. Команда запуска: `npm start`
3. Примените схему БД вручную через phpMyAdmin хоста или local (см. выше),
   либо выполните содержимое `server/db.sql`.

## API-методы

| Метод  | Путь                  | Описание                          |
|--------|-----------------------|-----------------------------------|
| POST   | /api/auth/register    | Регистрация                       |
| POST   | /api/auth/login       | Вход                              |
| POST   | /api/auth/logout      | Выход                             |
| GET    | /api/auth/me          | Текущий пользователь              |
| GET    | /api/user/profile     | Профиль: пользователь + подписка + история |
| PUT    | /api/user/name        | Сменить имя                       |
| PUT    | /api/user/password    | Сменить пароль                    |
| DELETE | /api/user             | Удалить аккаунт                   |
| GET    | /api/codes            | Все коды (владелец)               |
| POST   | /api/codes            | Создать код (владелец)            |
| DELETE | /api/codes/:code      | Удалить код (владелец)            |
| POST   | /api/redeem           | Активировать код                  |

Авторизация — через заголовок `Authorization: Bearer <token>`.

## Примечания

FreeSQLDatabase разрывает простаивающие соединения, поэтому в `db.js`
добавлены автоматический retry при сетевых сбоях и keep-alive пинг.
При первом запросе после простоя БД возможна задержка ~20 секунд — это
особенность бесплатного хостинга.
