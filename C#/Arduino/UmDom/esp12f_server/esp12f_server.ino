#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <DNSServer.h>

const char *AP_SSID = "UmDom";
const char *AP_PASS = "umdom1234";
const char *AP_IP   = "192.168.4.1";

const uint8_t  MAX_DEVICES = 8;
const uint32_t OFFLINE_MS  = 7000;
const uint16_t HTTP_PORT   = 80;

struct Device {
  char     id[12];
  char     name[18];
  bool     isRelay;
  bool     state;
  uint32_t seen;
};

Device devs[MAX_DEVICES];
uint8_t devCount = 0;

ESP8266WebServer server(HTTP_PORT);
DNSServer        dns;

const char PAGE[] PROGMEM =
"<!DOCTYPE html><html lang='ru'><head><meta charset='utf-8'>"
"<meta name='viewport' content='width=device-width,initial-scale=1'>"
"<title>Умный дом</title><style>"
"*{box-sizing:border-box}"
"body{margin:0;background:#14161a;color:#e8eaed;font:16px/1.4 system-ui,-apple-system,Segoe UI,Roboto,sans-serif}"
"header{padding:18px 16px 10px;border-bottom:1px solid #262a31}"
"h1{margin:0;font-size:20px}"
".sub{color:#8a919b;font-size:13px;margin-top:4px;word-break:break-all}"
"#list{padding:12px 16px 40px;max-width:640px;margin:0 auto}"
".card{display:flex;align-items:center;gap:12px;background:#1e2229;border:1px solid #2b3038;"
"border-radius:14px;padding:14px;margin-bottom:12px}"
".card.off{opacity:.5}"
".info{flex:1;min-width:0}"
".info b{display:block;font-size:17px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}"
".meta{color:#8a919b;font-size:12px;word-break:break-all}"
"button{border:0;border-radius:12px;padding:12px 20px;font:600 15px system-ui;color:#fff;"
"background:#2b3038;cursor:pointer;min-width:96px;flex:none}"
"button.on{background:#1f9d55}"
"button:active{filter:brightness(.85)}"
".empty{color:#8a919b;text-align:center;padding:30px 0}"
"</style></head><body>"
"<header><h1>Умный дом</h1><div class='sub' id='sub'>подключение...</div></header>"
"<div id='list'></div>"
"<script>"
"var list=document.getElementById('list');"
"function esc(s){return String(s).replace(/[&<>]/g,function(c){"
"return {'&':'&amp;','<':'&lt;','>':'&gt;'}[c];});}"
"function render(d){"
"document.getElementById('sub').textContent='точка доступа '+d.server+' · аптайм '+d.time+' с';"
"if(!d.devices.length){list.innerHTML=\"<p class='empty'>Устройств пока нет</p>\";return;}"
"var h='';"
"d.devices.forEach(function(v){"
"h+=\"<div class='card'+(v.online?'':' off')+'>\""
"+\"<div class='info'><b>\"+esc(v.name)+\"</b><span class='meta'>\"+esc(v.id)+\" · \""
"+(v.online?'в сети':'нет связи')+\"</span></div>\""
"+\"<button class='\"+(v.state?'on':'off')+\"' data-id='\"+esc(v.id)+\"'>\""
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

void cleanStr(char *dst, const String &src, size_t cap) {
  size_t j = 0;
  for (size_t i = 0; i < src.length() && j < cap - 1; i++) {
    char c = src.charAt(i);
    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
        (c >= '0' && c <= '9') || c == '-' || c == '_' || c == ' ') {
      dst[j++] = (c == ' ') ? '-' : c;
    }
  }
  dst[j] = 0;
}

int findId(const String &id) {
  for (uint8_t i = 0; i < devCount; i++)
    if (id == devs[i].id) return i;
  return -1;
}

bool isOnline(uint8_t i) { return (uint32_t)(millis() - devs[i].seen) < OFFLINE_MS; }

String stateJson() {
  String j = "{\"server\":\"";
  j += AP_SSID;
  j += "\",\"time\":";
  j += (unsigned long)(millis() / 1000);
  j += ",\"devices\":[";
  for (uint8_t i = 0; i < devCount; i++) {
    if (i) j += ',';
    j += "{\"id\":\"";
    j += devs[i].id;
    j += "\",\"name\":\"";
    j += devs[i].name;
    j += "\",\"type\":\"";
    j += devs[i].isRelay ? "relay" : "input";
    j += "\",\"state\":";
    j += devs[i].state ? 1 : 0;
    j += ",\"online\":";
    j += isOnline(i) ? 1 : 0;
    j += "}";
  }
  j += "]}";
  return j;
}

void logDev(const char *action, uint8_t i) {
  Serial.print(F("["));
  Serial.print(action);
  Serial.print(F("] "));
  Serial.print(devs[i].name);
  Serial.print(F(" ("));
  Serial.print(devs[i].id);
  Serial.print(F(") -> "));
  Serial.println(devs[i].state ? F("ВКЛ") : F("ВЫКЛ"));
}

void handleRoot() {
  server.send_P(200, "text/html; charset=utf-8", PAGE);
}

void handleNotFound() {
  server.sendHeader("Location", String("http://") + AP_IP + "/", true);
  server.send(302, "text/plain", "");
}

void handleState() {
  server.send(200, "application/json", stateJson());
}

void handleDev() {
  String id = server.arg("id");
  if (id.length() == 0 || id.length() > 11) { server.send(400, "text/plain", "bad id"); return; }

  String name = server.arg("name");
  String type = server.arg("type");

  int i = findId(id);
  if (i < 0) {
    if (devCount >= MAX_DEVICES) { server.send(507, "text/plain", "full"); return; }
    i = devCount;
    devCount++;
    cleanStr(devs[i].id, id, sizeof(devs[i].id));
    devs[i].state = false;
    devs[i].isRelay = (type == "relay");
    Serial.print(F("[+] "));
    Serial.println(devs[i].id);
  }
  devs[i].seen = millis();
  if (name.length()) cleanStr(devs[i].name, name, sizeof(devs[i].name));
  if (!devs[i].name[0]) cleanStr(devs[i].name, devs[i].id, sizeof(devs[i].name));

  server.send(200, "text/plain", devs[i].state ? "1" : "0");
}

void handleToggle() {
  int i = findId(server.arg("id"));
  if (i < 0) { server.send(404, "text/plain", "no device"); return; }
  devs[i].seen = millis();
  devs[i].state = !devs[i].state;
  logDev("toggle", (uint8_t)i);
  server.send(200, "text/plain", devs[i].state ? "1" : "0");
}

void handleSet() {
  int i = findId(server.arg("id"));
  if (i < 0) { server.send(404, "text/plain", "no device"); return; }
  devs[i].seen = millis();
  devs[i].state = (server.arg("state") == "1");
  logDev("set", (uint8_t)i);
  server.send(200, "text/plain", devs[i].state ? "1" : "0");
}

void serverHelp() {
  Serial.println(F("--- Умный дом: сервер ---"));
  Serial.println(F("help              эта справка"));
  Serial.println(F("list              список устройств"));
  Serial.println(F("on N / off N      включить/выключить устройство N"));
  Serial.println(F("toggle N          переключить устройство N"));
  Serial.println(F("info              состояние точки доступа"));
  Serial.println(F("reset             перезагрузка"));
}

void serverCommand(char *line) {
  char *arg = strchr(line, ' ');
  if (arg) { *arg = 0; arg++; }
  String cmd(line);
  cmd.toLowerCase();

  if (cmd == "help" || cmd == "?") { serverHelp(); return; }
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
      Serial.print(isOnline(i) ? F("в сети  ") : F("оффлайн "));
      Serial.println(devs[i].state ? F("ВКЛ") : F("ВЫКЛ"));
    }
    return;
  }
  if (cmd == "info") {
    Serial.print(F("SSID: "));
    Serial.println(AP_SSID);
    Serial.print(F("IP:   http://"));
    Serial.println(AP_IP);
    Serial.print(F("аптайм: "));
    Serial.print(millis() / 1000);
    Serial.println(F(" с"));
    return;
  }
  if (cmd == "reset") { Serial.println(F("reset")); ESP.restart(); return; }

  if (cmd == "on" || cmd == "off" || cmd == "toggle") {
    if (!arg) { Serial.println(F("нужен номер: e.g. on 0")); return; }
    int i = atoi(arg);
    if (i < 0 || i >= (int)devCount) { Serial.println(F("нет такого устройства")); return; }
    if (cmd == "on") devs[i].state = true;
    else if (cmd == "off") devs[i].state = false;
    else devs[i].state = !devs[i].state;
    logDev("console", (uint8_t)i);
    return;
  }
  Serial.println(F("неизвестная команда, help"));
}

void serialLoop() {
  static char buf[64];
  static size_t len = 0;
  while (Serial.available()) {
    char c = (char)Serial.read();
    if (c == '\r' || c == '\n') {
      if (len) { buf[len] = 0; serverCommand(buf); len = 0; }
    } else if (len < sizeof(buf) - 1) {
      buf[len++] = c;
    }
  }
}

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(50);
  Serial.println();
  Serial.println(F("=== Умный дом: сервер (ESP-12F) ==="));

  WiFi.persistent(false);
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(IPAddress(192, 168, 4, 1), IPAddress(192, 168, 4, 1), IPAddress(255, 255, 255, 0));
  bool ok = (*AP_PASS && strlen(AP_PASS) >= 8)
                ? WiFi.softAP(AP_SSID, AP_PASS)
                : WiFi.softAP(AP_SSID);
  if (ok) {
    Serial.print(F("точка доступа: "));
    Serial.println(AP_SSID);
  } else {
    Serial.println(F("ошибка запуска точки доступа"));
  }

  dns.start(53, "*", IPAddress(192, 168, 4, 1));

  server.on("/", HTTP_GET, handleRoot);
  server.on("/api/state", HTTP_GET, handleState);
  server.on("/api/dev", HTTP_GET, handleDev);
  server.on("/api/toggle", HTTP_GET, handleToggle);
  server.on("/api/set", HTTP_GET, handleSet);
  server.onNotFound(handleNotFound);
  server.begin();

  Serial.println(F("панель: http://192.168.4.1/"));
  serverHelp();
}

void loop() {
  dns.processNextRequest();
  server.handleClient();
  serialLoop();
}
