#include <WiFi.h>
#include <WebServer.h>
#include <HTTPClient.h>
#include <WiFiClient.h>

const char *AP_SSID = "UmDom";
const char *AP_PASS = "umdom1234";
const char *AP_IP  = "192.168.4.1";

const uint8_t  LED_PIN  = 2;
const uint8_t  BTN_PIN  = 27;
const uint32_t OFFLINE_MS = 7000;
const uint32_t SELFTEST_MS = 5000;

struct Device {
  String   id;
  String   name;
  bool     state;
  uint32_t seen;
};

Device  devs[4];
uint8_t devCount = 0;
uint32_t selfTs = 0;
uint32_t blinkTs = 0;
bool     btnPrev = true;

WebServer server(80);

const char PAGE[] PROGMEM =
"<!DOCTYPE html><html lang='ru'><head><meta charset='utf-8'>"
"<meta name='viewport' content='width=device-width,initial-scale=1'>"
"<title>Умный дом</title><style>"
"*{box-sizing:border-box}"
"body{margin:0;background:#14161a;color:#e8eaed;font:16px/1.4 system-ui,sans-serif}"
"header{padding:18px 16px 10px;border-bottom:1px solid #262a31}"
"h1{margin:0;font-size:20px}"
".sub{color:#8a919b;font-size:13px;margin-top:4px}"
"#list{padding:12px 16px 40px;max-width:640px;margin:0 auto}"
".card{display:flex;align-items:center;gap:12px;background:#1e2229;border:1px solid #2b3038;"
"border-radius:14px;padding:14px;margin-bottom:12px}"
".info{flex:1;min-width:0}"
".info b{display:block;font-size:17px}"
".meta{color:#8a919b;font-size:12px}"
"button{border:0;border-radius:12px;padding:12px 20px;font:600 15px system-ui;color:#fff;"
"background:#2b3038;cursor:pointer;min-width:96px;flex:none}"
"button.on{background:#1f9d55}"
".empty{color:#8a919b;text-align:center;padding:30px 0}"
"</style></head><body>"
"<header><h1>Умный дом</h1><div class='sub' id='sub'>панель управления</div></header>"
"<div id='list'></div>"
"<script>"
"var list=document.getElementById('list');"
"function render(d){"
"if(!d.devices.length){list.innerHTML=\"<p class='empty'>Устройств пока нет</p>\";return;}"
"var h='';"
"d.devices.forEach(function(v){"
"h+=\"<div class='card'><div class='info'><b>\"+v.name+\"</b><span class='meta'>\"+v.id"
"+\" · \"+(v.online?'в сети':'нет связи')+\"</span></div>\""
"+\"<button class='\"+(v.state?'on':'off')+\"' data-id='\"+v.id+\"'>\""
"+(v.state?'ВЫКЛ':'ВКЛ')+\"</button></div>\";});"
"list.innerHTML=h;}"
"function refresh(){fetch('/api/state',{cache:'no-store'}).then(function(r){return r.json();})"
".then(render).catch(function(){});}"
"document.addEventListener('click',function(e){"
"var b=e.target.closest?e.target.closest('button[data-id]'):null;if(!b)return;"
"fetch('/api/toggle?id='+encodeURIComponent(b.getAttribute('data-id')),{cache:'no-store'})"
".then(function(){setTimeout(refresh,200);});});"
"setInterval(refresh,1200);refresh();"
"</script></body></html>";

int findId(const String &id) {
  for (uint8_t i = 0; i < devCount; i++)
    if (id == devs[i].id) return i;
  return -1;
}

String stateJson() {
  String j = "{\"server\":\"";
  j += AP_SSID;
  j += "\",\"time\":";
  j += (unsigned long)(millis() / 1000);
  j += ",\"devices\":[";
  for (uint8_t i = 0; i < devCount; i++) {
    if (i) j += ',';
    bool online = (uint32_t)(millis() - devs[i].seen) < OFFLINE_MS;
    j += "{\"id\":\"";
    j += devs[i].id;
    j += "\",\"name\":\"";
    j += devs[i].name;
    j += "\",\"type\":\"relay\",\"state\":";
    j += devs[i].state ? 1 : 0;
    j += ",\"online\":";
    j += online ? 1 : 0;
    j += "}";
  }
  j += "]}";
  return j;
}

void handleRoot() { server.send_P(200, "text/html; charset=utf-8", PAGE); }

void handleNotFound() {
  server.sendHeader("Location", String("http://") + AP_IP + "/", true);
  server.send(302, "text/plain", "");
}

void handleState() { server.send(200, "application/json", stateJson()); }

void handleToggle() {
  int i = findId(server.arg("id"));
  if (i < 0) { server.send(404, "text/plain", "no device"); return; }
  devs[i].seen = millis();
  devs[i].state = !devs[i].state;
  Serial.print(F("[toggle] "));
  Serial.print(devs[i].name);
  Serial.println(devs[i].state ? F(" -> ВКЛ") : F(" -> ВЫКЛ"));
  server.send(200, "text/plain", devs[i].state ? "1" : "0");
}

void setDevice(uint8_t i, bool on) {
  if (i >= devCount) return;
  devs[i].state = on;
  devs[i].seen = millis();
  Serial.print(F("[set] "));
  Serial.print(devs[i].name);
  Serial.println(on ? F(" -> ВКЛ") : F(" -> ВЫКЛ"));
}

void help() {
  Serial.println(F("--- сервер (симуляция Wokwi) ---"));
  Serial.println(F("on N / off N / toggle N   устройство N (нумерация с 0)"));
  Serial.println(F("list                     список устройств"));
  Serial.println(F("Кнопка зелёная = клик по кнопке ВКЛ/ВЫКЛ на сайте"));
}

void command(char *line) {
  char *arg = strchr(line, ' ');
  if (arg) { *arg = 0; arg++; }
  String cmd(line);
  cmd.toLowerCase();

  if (cmd == "list") {
    Serial.print(F("устройств: "));
    Serial.println(devCount);
    for (uint8_t i = 0; i < devCount; i++) {
      Serial.print(F("  "));
      Serial.print(i);
      Serial.print(F(") "));
      Serial.print(devs[i].name);
      Serial.print(F(" ["));
      Serial.print(devs[i].id);
      Serial.print(F("] "));
      Serial.println(devs[i].state ? F("ВКЛ") : F("ВЫКЛ"));
    }
    return;
  }
  if (cmd == "on" || cmd == "off" || cmd == "toggle") {
    if (!arg) { Serial.println(F("нужен номер: on 0")); return; }
    int i = atoi(arg);
    if (i < 0 || i >= (int)devCount) { Serial.println(F("нет такого устройства")); return; }
    if (cmd == "on") setDevice(i, true);
    else if (cmd == "off") setDevice(i, false);
    else setDevice(i, !devs[i].state);
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

void selfTest() {
  HTTPClient http;
  WiFiClient client;
  if (http.begin(client, String("http://") + AP_IP + "/api/state")) {
    if (http.GET() == 200) {
      Serial.print(F("[self] /api/state -> "));
      Serial.println(http.getString());
    } else {
      Serial.println(F("[self] веб-сервер не ответил"));
    }
    http.end();
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BTN_PIN, INPUT_PULLUP);

  devCount = 1;
  devs[0].id = "123456";
  devs[0].name = "Relay-1";
  devs[0].state = false;
  devs[0].seen = millis();

  WiFi.mode(WIFI_AP);
  bool ok = (*AP_PASS && strlen(AP_PASS) >= 8) ? WiFi.softAP(AP_SSID, AP_PASS) : WiFi.softAP(AP_SSID);
  Serial.println(ok ? String(F("точка доступа ")) + AP_SSID : F("ошибка SoftAP"));

  server.on("/", HTTP_GET, handleRoot);
  server.on("/api/state", HTTP_GET, handleState);
  server.on("/api/toggle", HTTP_GET, handleToggle);
  server.on("/api/dev", HTTP_GET, handleState);
  server.on("/api/set", HTTP_GET, handleToggle);
  server.onNotFound(handleNotFound);
  server.begin();

  Serial.println(String(F("панель: http://")) + AP_IP + "/");
  help();
}

void loop() {
  server.handleClient();
  serialLoop();

  if (digitalRead(BTN_PIN) == LOW && btnPrev) setDevice(0, !devs[0].state);
  btnPrev = digitalRead(BTN_PIN) == LOW;

  for (uint8_t i = 0; i < devCount; i++) {
    if ((uint32_t)(millis() - devs[i].seen) >= 1000) devs[i].seen = millis();
  }

  if ((uint32_t)(millis() - blinkTs) >= 500) {
    blinkTs = millis();
    digitalWrite(LED_PIN, !digitalRead(LED_PIN));
  }
  if ((uint32_t)(millis() - selfTs) >= SELFTEST_MS) {
    selfTs = millis();
    selfTest();
  }
}
