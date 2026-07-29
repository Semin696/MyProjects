import flet as ft
import psutil
import socket
import random
import string
import threading
import time
import os
import winreg
import json
from datetime import datetime

I = ft.icons.Icons
B = ft.border

CHEAT_PROCESSES = [
    "cheatengine", "ce.exe", "wpepro", "wpe_pro", "wpe",
    "everyping", "eeping", "artmoney", "artmoney7",
    "tsearch", "multihack", "extreme_trainer",
    "processhacker", "procxp", "ollydbg",
    "x64dbg", "x32dbg", "reclass",
    "injector.exe", "fraps", "bandicam",
    "autohotkey", "autohotkey.exe",
]

CHEAT_FILES = [
    "cheatengine.exe", "CE.exe", "wpepro.exe", "everyping.exe",
    "artmoney.exe", "tsearch.exe", "multihack.exe",
    "processhacker.exe", "x64dbg.exe", "reclass.exe",
    "injector.exe", "fraps.exe", "bandicam.exe",
]

CHEAT_REGISTRY = [
    (winreg.HKEY_CURRENT_USER, r"Software\Cheat Engine"),
    (winreg.HKEY_CURRENT_USER, r"Software\ArtMoney"),
    (winreg.HKEY_LOCAL_MACHINE, r"Software\Cheat Engine"),
]

CHEAT_DIRS = [
    os.path.expandvars(r"%ProgramFiles%\Cheat Engine"),
    os.path.expandvars(r"%ProgramFiles(x86)%\Cheat Engine"),
    os.path.expandvars(r"%ProgramFiles%\WPE Pro"),
    os.path.expandvars(r"%ProgramFiles%\Process Hacker 2"),
]

class CheatScanner:
    def __init__(self):
        self.results = {"processes": [], "files": [], "registry": [], "directories": [], "network": []}

    def scan_processes(self):
        found = []
        for proc in psutil.process_iter(['pid', 'name', 'exe', 'create_time']):
            try:
                name = (proc.info['name'] or "").lower().replace(" ", "").replace("-", "").replace("_", "")
                for cheat in CHEAT_PROCESSES:
                    if cheat.lower() in name:
                        found.append({"name": proc.info['name'], "pid": proc.info['pid'], "exe": proc.info['exe'] or "", "match": cheat})
                        break
            except:
                pass
        self.results["processes"] = found
        return found

    def scan_files(self, paths=None):
        found = []
        if not paths:
            paths = [os.environ.get("TEMP", "C:\\Temp"), os.path.expandvars(r"%UserProfile%\Desktop"), os.path.expandvars(r"%UserProfile%\Downloads")]
        for path in paths:
            if not os.path.exists(path): continue
            try:
                for f in os.listdir(path):
                    f_lower = f.lower()
                    for cheat in CHEAT_FILES:
                        if cheat.lower() in f_lower or f_lower.endswith(cheat.lower()):
                            found.append({"path": os.path.join(path, f), "name": f, "match": cheat})
                            break
            except:
                pass
        self.results["files"] = found
        return found

    def scan_directories(self):
        found = [{"path": d} for d in CHEAT_DIRS if os.path.exists(d)]
        self.results["directories"] = found
        return found

    def scan_registry(self):
        found = []
        for hive, key in CHEAT_REGISTRY:
            try:
                with winreg.OpenKey(hive, key, 0, winreg.KEY_READ) as k:
                    found.append({"hive": "HKCU" if hive == winreg.HKEY_CURRENT_USER else "HKLM", "key": key, "values": winreg.QueryInfoKey(k)[1]})
            except:
                pass
        self.results["registry"] = found
        return found

    def scan_network(self):
        found = []
        ports = {1337, 4444, 5555, 6666, 7000, 31337, 12345}
        try:
            for conn in psutil.net_connections():
                if conn.status == "LISTEN" and conn.laddr.port in ports:
                    try:
                        pname = psutil.Process(conn.pid).name() if conn.pid else "Unknown"
                    except:
                        pname = "Unknown"
                    found.append({"port": conn.laddr.port, "pid": conn.pid, "process": pname})
        except:
            pass
        self.results["network"] = found
        return found

    def full_scan(self, cb=None):
        steps = [(self.scan_processes, "Процессы..."), (self.scan_files, "Файлы..."), (self.scan_directories, "Директории..."), (self.scan_registry, "Реестр..."), (self.scan_network, "Сеть...")]
        for i, (fn, msg) in enumerate(steps):
            if cb: cb(msg, (i+1)/len(steps))
            fn()
        if cb: cb("Готово!", 1.0)

    def get_summary(self):
        return sum(len(v) for v in self.results.values()), {k: len(v) for k, v in self.results.items()}

class RemoteServer:
    def __init__(self):
        self.code = self._gen()
        self.sock = None
        self.running = False
        self.connected = False
        self.pending = None
        self.client_addr = None
        self.confirmed = False
        self.denied = False
        self.lock = threading.Lock()

    def _gen(self):
        return f"{''.join(random.choices(string.ascii_uppercase+string.digits,k=4))}-{''.join(random.choices(string.ascii_uppercase+string.digits,k=4))}"

    def refresh_code(self):
        self.code = self._gen(); return self.code

    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect(("8.8.8.8",80))
            ip = s.getsockname()[0]; s.close(); return ip
        except: return "127.0.0.1"

    def start(self, port=9090):
        if self.running: return False
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.sock.bind(("0.0.0.0", port)); self.sock.listen(1); self.sock.settimeout(1)
            self.running = True; self.port = port
            threading.Thread(target=self._listen, daemon=True).start()
            return True
        except: return False

    def stop(self):
        self.running = False; self.connected = False; self.confirmed = False; self.denied = False; self.pending = None
        try:
            if self.sock: self.sock.close()
        except: pass

    def _listen(self):
        while self.running:
            try:
                c, addr = self.sock.accept(); c.settimeout(10)
                try:
                    d = c.recv(4096)
                    if not d: c.close(); continue
                    msg = json.loads(d.decode())
                except: c.close(); continue
                if msg.get("type") != "auth" or msg.get("code") != self.code:
                    try: c.sendall(json.dumps({"status":"error","message":"Неверный код"}).encode())
                    except: pass
                    c.close(); continue
                try: c.sendall(json.dumps({"status":"waiting_confirmation"}).encode())
                except: c.close(); continue
                with self.lock: self.pending = c; self.client_addr = addr; self.confirmed = False; self.denied = False
                t0 = time.time()
                while time.time()-t0 < 30:
                    with self.lock:
                        if self.confirmed:
                            self.connected = True
                            try: c.sendall(json.dumps({"status":"confirmed"}).encode())
                            except: self.connected = False; break
                            threading.Thread(target=self._handler, args=(c,), daemon=True).start(); return
                        if self.denied:
                            try: c.sendall(json.dumps({"status":"rejected"}).encode())
                            except: pass
                            c.close()
                            with self.lock: self.pending = None; self.confirmed = False; self.denied = False
                            break
                    time.sleep(0.1)
                else:
                    try: c.close()
                    except: pass
                    with self.lock: self.pending = None; self.confirmed = False; self.denied = False
            except socket.timeout: continue
            except:
                if self.running: time.sleep(0.1)

    def confirm(self):
        with self.lock: self.confirmed = True

    def deny(self):
        with self.lock: self.denied = True

    def _handler(self, c):
        try:
            c.settimeout(0.5)
            while self.running and self.connected:
                try:
                    d = c.recv(4096)
                    if not d: break
                    self._exec(json.loads(d.decode()))
                except socket.timeout: continue
                except: break
        except: pass
        finally:
            self.connected = False; self.confirmed = False; self.denied = False
            try: c.close()
            except: pass

    def _exec(self, cmd):
        t = cmd.get("type")
        try:
            import pyautogui
            if t == "mouse_move": pyautogui.moveRel(cmd["dx"], cmd["dy"], duration=0.05)
            elif t == "mouse_click": pyautogui.click(button=cmd.get("button","left"))
            elif t == "mouse_double_click": pyautogui.doubleClick()
            elif t == "mouse_scroll": pyautogui.scroll(cmd.get("clicks",0))
            elif t == "key_press": pyautogui.write(cmd["text"])
            elif t == "key_hotkey": pyautogui.hotkey(*cmd["keys"])
        except: pass

def main(page: ft.Page):
    page.title = "ToolCheats Hunter - Клиент"
    page.theme_mode = ft.ThemeMode.DARK
    page.bgcolor = "#0f0f1a"
    page.padding = 0
    page.window_width = 1100
    page.window_height = 750
    page.window_resizable = True
    page.theme = ft.Theme(color_scheme=ft.ColorScheme(primary="#00d4ff", secondary="#7c3aed", error="#ef4444"), font_family="Segoe UI", use_material3=True)

    scanner = CheatScanner()
    server = RemoteServer()

    def card(content, title=None, h=None, width=None):
        items = []
        if title:
            items.append(ft.Text(title, size=16, weight=ft.FontWeight.BOLD, color="#00d4ff"))
            items.append(ft.Container(height=12))
        items.append(content)
        return ft.Container(content=ft.Column(items, spacing=0), border_radius=12, bgcolor="#1a1a2e", border=ft.border.Border(left=B.BorderSide(1,"#2d2d4a"), right=B.BorderSide(1,"#2d2d4a"), top=B.BorderSide(1,"#2d2d4a"), bottom=B.BorderSide(1,"#2d2d4a")), padding=20, height=h, width=width)

    # ─── DASHBOARD ───
    dash = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)
    dash_prog = ft.ProgressBar(width=None, value=0, color="#00d4ff", bgcolor="#1e293b", visible=False)
    dash_stat = ft.Text("", size=13, color="#94a3b8")

    def build_dash():
        dash.controls.clear()
        cpu = psutil.cpu_count(logical=True)
        mem = psutil.virtual_memory()
        sys = ft.Row([
            ft.Container(content=ft.Column([ft.Icon(I.MEMORY, color="#00d4ff", size=28), ft.Text("CPU", size=12, color="#94a3b8"), ft.Text(f"{cpu} ядер", size=16, weight=ft.FontWeight.BOLD, color="#e2e8f0")], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=4), padding=15, border_radius=10, bgcolor="#16213e", border=ft.border.Border(left=B.BorderSide(1,"#2d2d4a"), right=B.BorderSide(1,"#2d2d4a"), top=B.BorderSide(1,"#2d2d4a"), bottom=B.BorderSide(1,"#2d2d4a")), expand=1),
            ft.Container(content=ft.Column([ft.Icon(I.MEMORY, color="#7c3aed", size=28), ft.Text("RAM", size=12, color="#94a3b8"), ft.Text(f"{mem.total/(1024**3):.1f} GB", size=16, weight=ft.FontWeight.BOLD, color="#e2e8f0")], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=4), padding=15, border_radius=10, bgcolor="#16213e", border=ft.border.Border(left=B.BorderSide(1,"#2d2d4a"), right=B.BorderSide(1,"#2d2d4a"), top=B.BorderSide(1,"#2d2d4a"), bottom=B.BorderSide(1,"#2d2d4a")), expand=1),
            ft.Container(content=ft.Column([ft.Icon(I.COMPUTER, color="#f59e0b", size=28), ft.Text("ОС", size=12, color="#94a3b8"), ft.Text("Windows", size=14, weight=ft.FontWeight.BOLD, color="#e2e8f0")], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=4), padding=15, border_radius=10, bgcolor="#16213e", border=ft.border.Border(left=B.BorderSide(1,"#2d2d4a"), right=B.BorderSide(1,"#2d2d4a"), top=B.BorderSide(1,"#2d2d4a"), bottom=B.BorderSide(1,"#2d2d4a")), expand=1),
        ], spacing=12)

        def do_scan():
            dash_prog.visible = True; dash_prog.value = 0; dash_stat.value = "Сканирование..."; dash_stat.color = "#f59e0b"; page.update()
            def cb(msg, v): dash_prog.value = v; dash_stat.value = msg; page.update()
            def t():
                scanner.full_scan(cb)
                total, _ = scanner.get_summary()
                page.run_thread(lambda: (setattr(dash_prog,'visible',False), setattr(dash_stat,'value','Чисто' if total==0 else f"Найдено {total}"), setattr(dash_stat,'color','#22c55e' if total==0 else '#ef4444'), page.update()))
            threading.Thread(target=t, daemon=True).start()

        sbtn = ft.Button(content=ft.Row([ft.Icon(I.SHIELD, color="#000000"), ft.Text("Быстрое сканирование", color="#000000", weight=ft.FontWeight.BOLD)]), style=ft.ButtonStyle(bgcolor="#00d4ff", padding=ft.padding.Padding.symmetric(horizontal=24, vertical=14), shape=ft.RoundedRectangleBorder(10)), on_click=lambda e: do_scan())

        # Connection panel
        cpanel = ft.Column(spacing=8)
        def up_conn():
            if server.connected: col="#22c55e"; txt="Подключён"
            elif server.running: col="#f59e0b"; txt="Ожидание..."
            else: col="#ef4444"; txt="Не активен"
            cpanel.controls.clear()
            cpanel.controls.extend([
                ft.Row([ft.Text("Код:", size=14, color="#94a3b8")]),
                ft.Row([ft.Container(content=ft.Text(server.code, size=24, weight=ft.FontWeight.BOLD, color="#00d4ff"), padding=12, border_radius=8, bgcolor="#0d0d1a", border=ft.border.Border(left=B.BorderSide(1,"#00d4ff"),right=B.BorderSide(1,"#00d4ff"),top=B.BorderSide(1,"#00d4ff"),bottom=B.BorderSide(1,"#00d4ff")), expand=1), ft.IconButton(I.REFRESH, icon_color="#00d4ff", icon_size=24, tooltip="Обновить", on_click=lambda e: (server.refresh_code(), up_conn()))], spacing=8),
                ft.Row([ft.Text(f"IP: {server.get_local_ip()}", size=13, color="#94a3b8"), ft.Text("Порт: 9090", size=13, color="#94a3b8")], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                ft.Container(content=ft.Row([ft.Container(width=8,height=8,border_radius=4,bgcolor=col), ft.Text(txt,size=13,color=col)],spacing=8)),
            ]); page.update()

        dash.controls.extend([
            ft.Text("Панель управления", size=24, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ft.Container(height=4, border_radius=2, gradient=ft.LinearGradient(colors=["#00d4ff","#7c3aed"])),
            ft.Container(height=8), sys, ft.Container(height=4),
            ft.Row([card(ft.Column([sbtn, ft.Container(height=16), dash_prog, dash_stat], horizontal_alignment=ft.CrossAxisAlignment.CENTER),"Сканер",200), card(cpanel,"Удалённый доступ",200)], spacing=16, expand=1),
        ])
        up_conn()

    # ─── SCANNER TAB ───
    stab = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)
    sprog = ft.ProgressBar(width=None, value=0, color="#00d4ff", bgcolor="#1e293b", visible=False)
    sstat = ft.Text("", size=13, color="#94a3b8")
    sres = ft.Column(spacing=4, scroll=ft.ScrollMode.AUTO)

    def build_scanner():
        stab.controls.clear(); sres.controls.clear()

        def show_res(results):
            sres.controls.clear()
            for st, items, icon, col in [("Процессы",results.get("processes",[]),I.MEMORY,"#ef4444"),("Файлы",results.get("files",[]),I.DESCRIPTION,"#f59e0b"),("Директории",results.get("directories",[]),I.FOLDER,"#7c3aed"),("Реестр",results.get("registry",[]),I.SETTINGS,"#00d4ff"),("Сеть",results.get("network",[]),I.WIFI,"#22c55e")]:
                col_items = ft.Column(spacing=4)
                if not items:
                    col_items.controls.append(ft.Container(content=ft.Row([ft.Icon(I.CHECK_CIRCLE,color="#22c55e",size=18),ft.Text(f"{st}: чисто",size=13,color="#22c55e")]),padding=8))
                else:
                    for item in items[:10]:
                        n = item.get("name") or item.get("path","").split("\\")[-1] or item.get("key","")
                        col_items.controls.append(ft.Container(content=ft.Row([ft.Icon(icon,color=col,size=16),ft.Text(str(n),size=13,color="#e2e8f0",expand=True)]),padding=ft.padding.Padding.symmetric(vertical=3,horizontal=8),border_radius=6,bgcolor="#1e293b"))
                    if len(items)>10: col_items.controls.append(ft.Text(f"...+{len(items)-10}",size=12,color="#64748b"))
                sres.controls.append(card(col_items, f"{st} ({len(items)})"))
            page.update()

        def full_scan():
            nonlocal sbtn
            sbtn.disabled = True; sbtn.content.controls[1].value = "Сканирование..."; sprog.visible = True; sprog.value = 0; sstat.value = ""; page.update()
            def cb(msg, v): sprog.value = v; sstat.value = msg; page.update()
            def t():
                scanner.full_scan(cb)
                total, _ = scanner.get_summary()
                page.run_thread(lambda: (setattr(sbtn,'disabled',False), setattr(sbtn.content.controls[1],'value',"Запустить полное сканирование"), setattr(sprog,'visible',False), setattr(sstat,'value',f"Найдено {total}" if total else "Всё чисто"), setattr(sstat,'color','#ef4444' if total else '#22c55e'), show_res(scanner.results)))
            threading.Thread(target=t, daemon=True).start()

        sbtn = ft.Button(content=ft.Row([ft.Icon(I.SEARCH,color="#000000"),ft.Text("Запустить полное сканирование",color="#000000",weight=ft.FontWeight.BOLD)]),style=ft.ButtonStyle(bgcolor="#00d4ff",padding=ft.padding.Padding.symmetric(horizontal=24,vertical=14),shape=ft.RoundedRectangleBorder(10)),on_click=lambda e: full_scan())

        def qs(typ):
            m = {"processes":scanner.scan_processes,"files":scanner.scan_files,"registry":scanner.scan_registry,"network":scanner.scan_network}
            if typ in m:
                items = m[typ]()
                r = {k:(items if k==typ else scanner.results.get(k,[])) for k in scanner.results}
                show_res(r)
                sstat.value = f"Найдено {len(items)}" if items else "Всё чисто"
                sstat.color = "#ef4444" if items else "#22c55e"; page.update()

        qr = ft.Row([ft.Button("Процессы",icon=I.MEMORY,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:qs("processes")),ft.Button("Файлы",icon=I.DESCRIPTION,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:qs("files")),ft.Button("Реестр",icon=I.SETTINGS,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:qs("registry")),ft.Button("Сеть",icon=I.WIFI,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:qs("network"))],wrap=True,spacing=8)

        stab.controls.extend([ft.Text("Сканер читов",size=24,weight=ft.FontWeight.BOLD,color="#e2e8f0"),ft.Container(height=4,border_radius=2,gradient=ft.LinearGradient(colors=["#00d4ff","#7c3aed"])),ft.Container(height=8),sbtn,qr,sprog,sstat,ft.Divider(color="#2d2d4a"),sres])

    # ─── REMOTE TAB ───
    rtab = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)

    def build_remote():
        rtab.controls.clear()
        cdisp = ft.Text(server.code, size=36, weight=ft.FontWeight.BOLD, color="#00d4ff")
        ipdisp = ft.Text(f"IP: {server.get_local_ip()}", size=15, color="#94a3b8")
        stat_disp = ft.Container(content=ft.Row([ft.Container(width=8,height=8,border_radius=4,bgcolor="#64748b"),ft.Text("Сервер не запущен",size=13,color="#64748b")],spacing=8))
        logc = ft.Column(spacing=4, scroll=ft.ScrollMode.AUTO)

        def log(msg):
            ts = datetime.now().strftime("%H:%M:%S")
            logc.controls.append(ft.Container(content=ft.Row([ft.Text(ts,size=11,color="#64748b"),ft.Text(msg,size=13,color="#e2e8f0")],spacing=10),padding=ft.padding.Padding.symmetric(vertical=4)))
            if len(logc.controls)>50: logc.controls.pop(0)
            page.update()

        def up():
            cdisp.value = server.code
            if not server.running: stat_disp.content.controls[0].bgcolor="#64748b"; stat_disp.content.controls[1].value="Сервер не запущен"
            elif server.connected: stat_disp.content.controls[0].bgcolor="#22c55e"; stat_disp.content.controls[1].value="Админ подключён"
            elif server.pending: stat_disp.content.controls[0].bgcolor="#f59e0b"; stat_disp.content.controls[1].value="Запрос..."
            else: stat_disp.content.controls[0].bgcolor="#00d4ff"; stat_disp.content.controls[1].value="Ожидание..."
            page.update()

        def toggle(e):
            if not server.running:
                server.start(9090)
                sbtn.content.controls[0].name = I.STOP_CIRCLE; sbtn.content.controls[1].value = "Остановить"; sbtn.style.bgcolor = "#ef4444"; log("Сервер запущен")
            else:
                server.stop()
                sbtn.content.controls[0].name = I.PLAY_CIRCLE; sbtn.content.controls[1].value = "Запустить"; sbtn.style.bgcolor = "#22c55e"; log("Сервер остановлен")
            up()

        sbtn = ft.Button(content=ft.Row([ft.Icon(I.PLAY_CIRCLE,color="#000000"),ft.Text("Запустить сервер",color="#000000",weight=ft.FontWeight.BOLD)]),style=ft.ButtonStyle(bgcolor="#22c55e",padding=ft.padding.Padding.symmetric(horizontal=24,vertical=14),shape=ft.RoundedRectangleBorder(10)),on_click=toggle)

        req_addr = ft.Text("", size=14, color="#e2e8f0")
        dlg = ft.AlertDialog(modal=True,title=ft.Text("Запрос на подключение",size=18,weight=ft.FontWeight.BOLD),content=ft.Column([ft.Text("Администратор хочет подключиться",size=14,color="#94a3b8"),ft.Container(height=8),req_addr],tight=True,spacing=0,width=300),actions=[ft.Button("Отклонить",style=ft.ButtonStyle(bgcolor="#ef4444",color="#ffffff"),on_click=lambda e: deny()),ft.Button("Разрешить",style=ft.ButtonStyle(bgcolor="#22c55e",color="#ffffff"),on_click=lambda e: accept())])

        def accept(): server.confirm(); dlg.open=False
        if dlg in page.overlay: page.overlay.remove(dlg); log("Подключение разрешено"); up()

        def deny(): server.deny(); dlg.open=False
        if dlg in page.overlay: page.overlay.remove(dlg); log("Подключение отклонено"); up()

        def check():
            if server.pending and not server.confirmed and not server.denied and dlg not in page.overlay:
                req_addr.value = f"IP: {server.client_addr[0]}:{server.client_addr[1]}"
                page.overlay.append(dlg); dlg.open = True; log(f"Входящий запрос от {server.client_addr[0]}"); page.update()

        def monitor():
            while True:
                time.sleep(0.5)
                try: page.run_thread(check)
                except: pass
        threading.Thread(target=monitor, daemon=True).start()

        rtab.controls.extend([
            ft.Text("Удалённый доступ",size=24,weight=ft.FontWeight.BOLD,color="#e2e8f0"),ft.Container(height=4,border_radius=2,gradient=ft.LinearGradient(colors=["#00d4ff","#7c3aed"])),ft.Container(height=8),sbtn,
            ft.Row([card(ft.Column([ft.Row([ft.Text("Код подключения:",size=14,color="#94a3b8")]),ft.Container(height=8),ft.Container(content=ft.Row([cdisp],alignment=ft.MainAxisAlignment.CENTER),padding=20,border_radius=10,bgcolor="#0d0d1a",border=ft.border.Border(left=B.BorderSide(2,"#00d4ff"),right=B.BorderSide(2,"#00d4ff"),top=B.BorderSide(2,"#00d4ff"),bottom=B.BorderSide(2,"#00d4ff"))),ft.Container(height=12),ft.Row([ft.Button("Обновить",icon=I.REFRESH,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:(server.refresh_code(),up())),ft.Button("Копировать",icon=I.COPY,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:page.set_clipboard(server.code))],alignment=ft.MainAxisAlignment.CENTER)]),width=None),card(ft.Column([ipdisp,ft.Container(height=8),ft.Text("Порт: 9090",size=14,color="#94a3b8"),ft.Container(height=8),stat_disp,ft.Container(height=8),ft.Text("Запустите сервер и передайте код администратору",size=12,color="#64748b")]),width=None)],spacing=16,vertical_alignment=ft.CrossAxisAlignment.START),
            card(logc,"Журнал подключений",250),
        ])
        up()

    build_dash(); build_scanner(); build_remote()

    pages = [ft.Container(dash, padding=20), ft.Container(stab, padding=20), ft.Container(rtab, padding=20)]
    content_stack = ft.Container(content=pages[0], expand=True)
    def switch_tab(i):
        content_stack.content = pages[i]; page.update()

    page.appbar = ft.AppBar(title=ft.Row([ft.Icon(I.SHIELD_MOON,color="#00d4ff",size=28),ft.Text("ToolCheats Hunter",size=20,weight=ft.FontWeight.BOLD,color="#e2e8f0")],spacing=10),bgcolor="#0f0f1a",actions=[ft.Container(content=ft.Text("Клиент",size=13,color="#00d4ff",weight=ft.FontWeight.BOLD),padding=ft.padding.Padding.symmetric(horizontal=12,vertical=6),border_radius=20,border=ft.border.Border(left=B.BorderSide(1,"#00d4ff"),right=B.BorderSide(1,"#00d4ff"),top=B.BorderSide(1,"#00d4ff"),bottom=B.BorderSide(1,"#00d4ff"))),ft.Container(width=10)])
    page.add(content_stack)
    page.navigation_bar = ft.NavigationBar(
        destinations=[
            ft.NavigationBarDestination(icon=I.DASHBOARD, label="Панель"),
            ft.NavigationBarDestination(icon=I.SHIELD, label="Сканер"),
            ft.NavigationBarDestination(icon=I.CAST_CONNECTED, label="Доступ"),
        ],
        selected_index=0, bgcolor="#1a1a2e", on_change=lambda e: switch_tab(e.control.selected_index),
    )
    page.update()

if __name__ == "__main__":
    ft.run(main)


