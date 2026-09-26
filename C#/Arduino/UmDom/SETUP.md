# Установка и первая настройка (Arduino IDE)

Инструкция для двух плат проекта «Умный дом»:

| Плата | Роль | Скетч |
|---|---|---|
| **ESP-12F** (ESP8266EX) | сервер: точка доступа, веб-панель | `esp12f_server/esp12f_server.ino` |
| **ESP8266MOD** | устройство с реле | `esp8266_relay/esp8266_relay.ino` |

Подробности схемы подключения реле — в `WIRING.md`.

---

## 1. Что скачать

| Что | Ссылка | Зачем |
|---|---|---|
| **Arduino IDE 2.x** | <https://www.arduino.cc/en/software> | сама среда |
| **Ядро ESP8266 (3.1.2)** | <https://arduino.esp8266.com/stable/package_esp8266com_index.json> | платы ESP8266 / ESP-12F / ESP8266MOD |
| **Драйвер USB-UART** | CP210x: <https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers><br>CH340: <https://www.wch-ic.com/downloads/CH341SER_EXE.html> | если на плате нет USB или стоит CP2102 / CH340 |

Дополнительные библиотеки **не нужны**: `ESP8266WiFi`, `ESP8266WebServer`,
`ESP8266HTTPClient` и `DNSServer` входят в ядро ESP8266.

---

## 2. Установка (один раз на компьютер)

1. Установи Arduino IDE.
2. **File → Preferences → Additional boards manager URLs** → вставь строку с ядром ESP8266
   из таблицы выше → **OK**.
3. **Tools → Boards Manager** → найди `esp8266` → **Install** → жди 3–5 минут.
4. **Tools → Port** → выбери COM-порт платы (в Windows может называться
   `USB-SERIAL CH340 (COM7)`).
5. Перезапусти Arduino IDE.

---

## 3. Настройки плат (Tools)

### ESP-12F — сервер

ESP-12F — голый модуль, USB на нём нет: нужен **USB-TTL адаптер 3.3 В** и отдельное
питание 5 В / 1 А.

| Параметр | Значение |
|---|---|
| Board | `Generic ESP8266` |
| Flash mode | `DIO` |
| Flash size | `4MB (1MB SPIFFS)` |
| Upload speed | `115200` (если не прошивается — `57600`, затем `9600`) |
| Erase Flash | `All` — только при первой прошивке |

### ESP8266MOD — устройство с реле

Если это NodeMCU v2/v3 (с USB и преобразователем на плате):

| Параметр | Значение |
|---|---|
| Board | `NodeMCU 1.0 (ESP-12E Module)` |
| Flash mode | `DIO` |
| Flash size | `4MB (1MB SPIFFS)` |
| Upload speed | `115200` |
| Erase Flash | `All` — только при первой прошивке |

Serial Monitor (кнопка внизу справа) — **115200 бод**.

---

## 4. Прошивка

1. **File → Open** → `esp12f_server/esp12f_server.ino` → **Upload** (кнопка →).
2. Тем же способом прошей `esp8266_relay/esp8266_relay.ino` на вторую плату.
3. Открой Serial Monitor, нажми Enter — в сервере должно быть:
   `точка доступа: UmDom` и `панель: http://192.168.4.1/`.
4. Телефон или ПК подключается к WiFi-сети `UmDom` (пароль `umdom1234`),
   браузер открывает `http://192.168.4.1` → кнопка ВКЛ/ВЫКЛ.

Интернет через эту сеть не раздаётся — только управление устройствами.

---

## 5. Если не прошивается или не работает

| Симптом | Причина и решение |
|---|---|
| `Port not found` | не установлен драйвер CP2102/CH340, либо кабель только для зарядки — нужен data-кабель |
| `Connecting...` и обрыв | зажать кнопку **FLASH**, нажать Upload, отпустить когда начнётся запись |
| `brownout detector` | слабое питание: нужен БП 5 В / 1 А (с реле — 2 А), не питание от USB-хаба |
| `Fatal error: Flash size` | Tools → Flash size = `4MB`, Flash mode = `DIO` |
| Устройство не находит сеть | в `esp8266_relay.ino` те же `AP_SSID` и `AP_PASS`, что и на сервере; пароль точки доступа ≥ 8 символов |
| Реле работает наоборот | в `esp8266_relay.ino` поменять `ACTIVE_LOW` на `false` |
| Скетч не компилируется на ESP32 | эти скетчи только под ESP8266; для симуляции есть проекты в `wokwi/` |

---

## 6. Симуляция без железа

Проекты для <https://wokwi.com> лежат в `wokwi/`: `esp12f_server` и `esp8266mod_device`.
Как открыть — в `wokwi/README.md`.
