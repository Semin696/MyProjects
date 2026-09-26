#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClient.h>

const char *AP_SSID     = "UmDom";
const char *AP_PASS     = "umdom1234";
const char *SERVER_IP   = "192.168.4.1";
const char *DEVICE_NAME = "Relay-1";
const bool  USE_LINK    = false;

const uint8_t  RELAY_PIN  = 5;
const uint8_t  BTN_PIN    = 27;
const bool     ACTIVE_LOW = false;
const uint32_t POLL_MS    = 2000;

bool     relayOn = false;
String   devId;
uint32_t pollTs = 0;
bool     btnPrev = true;

void applyRelay() {
  digitalWrite(RELAY_PIN, ACTIVE_LOW ? !relayOn : relayOn);
  Serial.print(F("реле "));
  Serial.print(DEVICE_NAME);
  Serial.print(F(": "));
  Serial.println(relayOn ? "ВКЛ" : "ВЫКЛ");
}

void setRelay(bool on) {
  if (relayOn == on) return;
  relayOn = on;
  applyRelay();
}

bool sendToServer(const char *path, const String &extra, String &answer) {
  HTTPClient http;
  String url = String("http://") + SERVER_IP + path + "?id=" + devId + "&name=" +
               DEVICE_NAME + "&type=relay" + (extra.length() ? "&" + extra : "");
  WiFiClient client;
  if (!http.begin(client, url)) return false;
  int code = http.GET();
  bool ok = (code == 200);
  if (ok) answer = http.getString();
  http.end();
  return ok;
}

void pollServer() {
  String answer;
  if (sendToServer("/api/dev", "", answer)) {
    setRelay(answer == "1");
  } else {
    Serial.println(F("сервер недоступен, продолжаем искать сеть"));
  }
}

void help() {
  Serial.println(F("--- устройство-реле (симуляция Wokwi) ---"));
  Serial.println(F("on / off / toggle   управление реле"));
  Serial.println(F("status              состояние"));
  Serial.println(F("link                включить связь с сервером"));
  Serial.println(F("Кнопка зелёная = команда с сайта, клавиша 1"));
}

void command(char *line) {
  String cmd(line);
  cmd.toLowerCase();

  if (cmd == "on") { setRelay(true); return; }
  if (cmd == "off") { setRelay(false); return; }
  if (cmd == "toggle") { setRelay(!relayOn); return; }
  if (cmd == "status") {
    Serial.print(F("id:  "));
    Serial.println(devId);
    Serial.print(F("сеть: "));
    Serial.print(WiFi.status() == WL_CONNECTED ? WiFi.SSID() : String(F("-")));
    Serial.print(F("  сервер: "));
    Serial.println(USE_LINK && WiFi.status() == WL_CONNECTED ? SERVER_IP : String(F("нет")));
    Serial.print(F("реле: "));
    Serial.println(relayOn ? "ВКЛ" : "ВЫКЛ");
    return;
  }
  if (cmd == "link") {
    Serial.println(F("в симуляции сервера нет, USE_LINK = false"));
    return;
  }
  help();
}

void serialLoop() {
  static char buf[48];
  static size_t len = 0;
  while (Serial.available()) {
    char c = (char)Serial.read();
    if (c == '\r' || c == '\n') {
      if (len) { buf[len] = 0; command(buf); len = 0; }
    } else if (len < sizeof(buf) - 1) {
      buf[len++] = c;
    }
  }
}

void buttonLoop() {
  bool now = digitalRead(BTN_PIN);
  if (btnPrev && !now) setRelay(!relayOn);
  btnPrev = now;
}

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);
  pinMode(BTN_PIN, INPUT_PULLUP);
  devId = String((uint32_t)ESP.getEfuseMac());
  WiFi.mode(WIFI_STA);
  if (USE_LINK) WiFi.begin(AP_SSID, AP_PASS);
  applyRelay();
  help();
}

void loop() {
  serialLoop();
  buttonLoop();
  if (USE_LINK && (uint32_t)(millis() - pollTs) >= POLL_MS) {
    pollTs = millis();
    pollServer();
  }
}
