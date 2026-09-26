#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <WiFiClient.h>

const char *AP_SSID    = "UmDom";
const char *AP_PASS    = "umdom1234";
const char *SERVER_IP  = "192.168.4.1";
const uint16_t SERVER_PORT = 80;

const char *DEVICE_NAME = "Relay-1";
const char *DEVICE_TYPE = "relay";

const uint8_t  RELAY_PIN  = D1;
const bool     ACTIVE_LOW = true;

const uint32_t POLL_MS     = 2000;
const uint32_t RESCAN_MS   = 10000;
const uint32_t IP_WAIT_MS  = 15000;
const uint8_t  MAX_FAIL    = 5;

enum Link { LINK_IDLE, LINK_SCAN, LINK_WAIT_IP, LINK_RUN };

Link      link = LINK_IDLE;
uint32_t  linkTs = 0;
uint8_t   fails = 0;
bool      relayOn = false;
String    devId;
String    devName = DEVICE_NAME;
String    devType = DEVICE_TYPE;

void applyRelay() {
  digitalWrite(RELAY_PIN, (ACTIVE_LOW ? !relayOn : relayOn));
  Serial.print(F("реле "));
  Serial.print(devName);
  Serial.print(F(": "));
  Serial.println(relayOn ? F("ВКЛ") : F("ВЫКЛ"));
}

void setRelay(bool on) {
  relayOn = on;
  applyRelay();
}

void startLink() {
  link = LINK_SCAN;
  linkTs = millis();
}

bool sendToServer(const char *path, const String &extra, String &answer) {
  if (WiFi.status() != WL_CONNECTED) return false;
  HTTPClient http;
  String url = String("http://") + SERVER_IP + ":" + SERVER_PORT + path +
               "?id=" + devId + "&name=" + devName + "&type=" + devType +
               (extra.length() ? "&" + extra : "");
  WiFiClient client;
  if (!http.begin(client, url)) return false;
  int code = http.GET();
  bool ok = (code == 200);
  if (ok) answer = http.getString();
  http.end();
  return ok;
}

bool pollServer() {
  String answer;
  if (!sendToServer("/api/dev", "", answer)) {
    fails++;
    Serial.println(F("нет связи с сервером"));
    if (fails >= MAX_FAIL) {
      Serial.println(F("переподключение к точке доступа"));
      WiFi.disconnect();
      fails = 0;
      link = LINK_IDLE;
      linkTs = millis();
    }
    return false;
  }
  fails = 0;
  bool want = (answer == "1");
  if (want != relayOn) {
    setRelay(want);
  }
  return true;
}

void deviceHelp() {
  Serial.println(F("--- Умный дом: устройство ---"));
  Serial.println(F("help / status    справка и состояние"));
  Serial.println(F("on / off         включить/выключить реле (и отправить на сервер)"));
  Serial.println(F("toggle           переключить реле"));
  Serial.println(F("name ТЕКСТ       задать имя устройства"));
  Serial.println(F("scan             повторный поиск точки доступа"));
  Serial.println(F("reset            перезагрузка"));
}

void deviceCommand(char *line) {
  char *arg = strchr(line, ' ');
  if (arg) { *arg = 0; arg++; }
  String cmd(line);
  cmd.toLowerCase();

  if (cmd == "help" || cmd == "status") {
    Serial.print(F("id:   ")); Serial.println(devId);
    Serial.print(F("имя:  ")); Serial.println(devName);
    Serial.print(F("сеть: ")); Serial.print(AP_SSID);
    Serial.print(F("  клиент: "));
    Serial.println(WiFi.status() == WL_CONNECTED ? WiFi.localIP().toString() : String(F("нет")));
    Serial.print(F("реле: ")); Serial.println(relayOn ? F("ВКЛ") : F("ВЫКЛ"));
    Serial.println(F("--------------------"));
    deviceHelp();
    return;
  }
  if (cmd == "on" || cmd == "off" || cmd == "toggle") {
    bool want = (cmd == "on") ? true : (cmd == "off") ? false : !relayOn;
    setRelay(want);
    String answer;
    if (!sendToServer("/api/set", (want ? "state=1" : "state=0"), answer))
      Serial.println(F("сервер не подтвердил, состояние только локально"));
    return;
  }
  if (cmd == "name") {
    if (!arg || !*arg) { Serial.println(F("usage: name Relay-1")); return; }
    String n;
    for (char *p = arg; *p; p++) n += (*p == ' ') ? '-' : *p;
    devName = n;
    Serial.print(F("имя: "));
    Serial.println(devName);
    return;
  }
  if (cmd == "scan") { startLink(); return; }
  if (cmd == "reset") { ESP.restart(); return; }
  Serial.println(F("неизвестная команда, help"));
}

void serialLoop() {
  static char buf[64];
  static size_t len = 0;
  while (Serial.available()) {
    char c = (char)Serial.read();
    if (c == '\r' || c == '\n') {
      if (len) { buf[len] = 0; deviceCommand(buf); len = 0; }
    } else if (len < sizeof(buf) - 1) {
      buf[len++] = c;
    }
  }
}

void linkTask() {
  uint32_t now = millis();
  switch (link) {
    case LINK_IDLE:
      if ((uint32_t)(now - linkTs) >= RESCAN_MS) startLink();
      break;

    case LINK_SCAN: {
      int n = WiFi.scanNetworks();
      bool found = false;
      for (int i = 0; i < n; i++) {
        if (WiFi.SSID(i) == AP_SSID) { found = true; break; }
      }
      WiFi.scanDelete();
      if (found) {
        Serial.print(F("сеть найдена, подключение к "));
        Serial.print(AP_SSID);
        Serial.println(F("..."));
        WiFi.mode(WIFI_STA);
        WiFi.begin(AP_SSID, *AP_PASS ? AP_PASS : "");
        link = LINK_WAIT_IP;
        linkTs = now;
      } else {
        Serial.print(F("сети "));
        Serial.print(AP_SSID);
        Serial.println(F(" нет, ждём"));
        link = LINK_IDLE;
        linkTs = now;
      }
      break;
    }

    case LINK_WAIT_IP:
      if (WiFi.status() == WL_CONNECTED) {
        Serial.print(F("подключено, IP "));
        Serial.println(WiFi.localIP().toString());
        link = LINK_RUN;
        linkTs = now;
        fails = 0;
      } else if ((uint32_t)(now - linkTs) >= IP_WAIT_MS) {
        Serial.println(F("не дождались IP, пробуем снова"));
        WiFi.disconnect();
        link = LINK_IDLE;
        linkTs = now;
      }
      break;

    case LINK_RUN:
      if (WiFi.status() != WL_CONNECTED) {
        Serial.println(F("WiFi потерян"));
        link = LINK_IDLE;
        linkTs = now;
      } else if ((uint32_t)(now - linkTs) >= POLL_MS) {
        linkTs = now;
        pollServer();
      }
      break;
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println();
  Serial.println(F("=== Умный дом: устройство-реле (ESP8266MOD) ==="));

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, ACTIVE_LOW ? HIGH : LOW);

  devId = String((unsigned long)ESP.getChipId());
  devName = DEVICE_NAME;
  devType = DEVICE_TYPE;

  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);

  applyRelay();
  deviceHelp();
  startLink();
}

void loop() {
  serialLoop();
  linkTask();
}
