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

# ─── Cheat Database ───
CHEAT_PROCESSES = [
    "cheatengine", "cheatengine-x86_64", "cheatengine-x86", "CE.exe",
    "wpepro", "wpe_pro", "wpe", "wpe-0.9a",
    "everyping", "eeping",
    "artmoney", "artmoney7", "am.exe",
    "tsearch", "tsearch.exe",
    "multihack", "multihack.exe",
    "extreme_trainer", "extreme trainer",
    "processhacker", "procxp", "processhacker.exe",
    "ollydbg", "ollydbg.exe",
    "x64dbg", "x32dbg", "x64dbg.exe",
    "ida64", "ida.exe", "ida64.exe",
    "ghidra",
    "reclass", "reclass.exe",
    "injector.exe", "dllinject",
    "deceiver", "deceiver.exe",
    "fraps", "fraps.exe",
    "bandicam", "bandicam.exe",
    "obs64", "obs32",
    "mirillis_action", "action.exe",
    "rivastatistics", "rtss.exe",
    "msiafterburner", "afterburner.exe",
    "autohotkey", "autohotkey.exe",
    "wireshark", "dumpcap",
    "pixelpick", "colorpix",
]

CHEAT_FILES = [
    "cheatengine.exe", "cheatengine-x86_64.exe", "CE.exe",
    "wpepro.exe", "wpe_pro.exe",
    "everyping.exe",
    "artmoney.exe", "artmoney7.exe",
    "tsearch.exe",
    "multihack.exe",
    "extreme_trainer.exe",
    "processhacker.exe", "procxp.exe",
    "ollydbg.exe", "ollydbg110.exe",
    "x64dbg.exe", "x32dbg.exe",
    "reclass.exe",
    "injector.exe",
    "deceiver.exe",
    "fraps.exe",
    "bandicam.exe",
    "obs64.exe",
    "action.exe",
    "rtss.exe",
    "afterburner.exe",
]

CHEAT_REGISTRY = [
    (winreg.HKEY_CURRENT_USER, r"Software\Cheat Engine"),
    (winreg.HKEY_CURRENT_USER, r"Software\ArtMoney"),
    (winreg.HKEY_CURRENT_USER, r"Software\WPE Pro"),
    (winreg.HKEY_LOCAL_MACHINE, r"Software\Cheat Engine"),
    (winreg.HKEY_CURRENT_USER, r"Software\Classes\cheatengine"),
]

CHEAT_DIRS = [
    os.path.expandvars(r"%ProgramFiles%\Cheat Engine"),
    os.path.expandvars(r"%ProgramFiles(x86)%\Cheat Engine"),
    os.path.expandvars(r"%ProgramFiles%\WPE Pro"),
    os.path.expandvars(r"%ProgramFiles%\ArtMoney"),
    os.path.expandvars(r"%ProgramFiles%\Process Hacker"),
    os.path.expandvars(r"%ProgramFiles%\Process Hacker 2"),
]

class CheatScanner:
    def __init__(self):
        self.results = {"processes": [], "files": [], "registry": [], "directories": [], "network": []}

    def scan_processes(self):
        found = []
        for proc in psutil.process_iter(['pid', 'name', 'exe', 'create_time']):
            try:
                name = proc.info['name'] or ""
                name_lower = name.lower().replace(" ", "").replace("-", "").replace("_", "")
                for cheat in CHEAT_PROCESSES:
                    if cheat.lower() in name_lower:
                        found.append({
                            "name": proc.info['name'],
                            "pid": proc.info['pid'],
                            "exe": proc.info['exe'] or "",
                            "created": datetime.fromtimestamp(proc.info['create_time']).strftime('%H:%M:%S') if proc.info['create_time'] else "N/A",
                            "match": cheat
                        })
                        break
            except:
                pass
        self.results["processes"] = found
        return found

    def scan_files(self, paths=None):
        found = []
        if not paths:
            paths = [
                os.environ.get("TEMP", "C:\\Temp"),
                os.path.expandvars(r"%UserProfile%\Desktop"),
                os.path.expandvars(r"%UserProfile%\Downloads"),
                os.path.expandvars(r"%UserProfile%\Documents"),
            ]
        for path in paths:
            if not os.path.exists(path):
                continue
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
        found = []
        for d in CHEAT_DIRS:
            if os.path.exists(d):
                found.append({"path": d, "exists": True})
        self.results["directories"] = found
        return found

    def scan_registry(self):
        found = []
        for hive, key in CHEAT_REGISTRY:
            try:
                with winreg.OpenKey(hive, key, 0, winreg.KEY_READ) as k:
                    val_count = winreg.QueryInfoKey(k)[1]
                    found.append({"hive": "HKCU" if hive == winreg.HKEY_CURRENT_USER else "HKLM", "key": key, "values": val_count})
            except:
                pass
        self.results["registry"] = found
        return found

    def scan_network(self):
        found = []
        suspicious_ports = {1337, 4444, 5555, 6666, 6667, 7000, 31337, 12345, 2000}
        try:
            for conn in psutil.net_connections():
                if conn.status == "LISTEN" and conn.laddr.port in suspicious_ports:
                    try:
                        proc = psutil.Process(conn.pid) if conn.pid else None
                        pname = proc.name() if proc else "Unknown"
                    except:
                        pname = "Unknown"
                    found.append({"port": conn.laddr.port, "pid": conn.pid, "process": pname, "status": conn.status})
        except:
            pass
        self.results["network"] = found
        return found

    def full_scan(self, progress_callback=None):
        steps = [
            (self.scan_processes, "Сканирование процессов..."),
            (self.scan_files, "Сканирование файлов..."),
            (self.scan_directories, "Проверка директорий..."),
            (self.scan_registry, "Проверка реестра..."),
            (self.scan_network, "Проверка сети..."),
        ]
        for i, (func, msg) in enumerate(steps):
            if progress_callback:
                progress_callback(msg, (i + 1) / len(steps))
            func()
        if progress_callback:
            progress_callback("Сканирование завершено!", 1.0)
        return self.results

    def get_summary(self):
        total = sum(len(v) for v in self.results.values())
        return total, {k: len(v) for k, v in self.results.items()}

# ─── Remote Access Server ───
class RemoteServer:
    def __init__(self):
        self.code = self.generate_code()
        self.server_socket = None
        self.running = False
        self.connected = False
        self.connection_request = None
        self.client_addr = None
        self.confirmed = False
        self.denied = False
        self.listener_thread = None
        self.handler_thread = None
        self.lock = threading.Lock()

    def generate_code(self):
        p1 = ''.join(random.choices(string.ascii_uppercase + string.digits, k=4))
        p2 = ''.join(random.choices(string.ascii_uppercase + string.digits, k=4))
        return f"{p1}-{p2}"

    def refresh_code(self):
        self.code = self.generate_code()
        return self.code

    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return "127.0.0.1"

    def start_server(self, port=9090):
        if self.running:
            return False
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(("0.0.0.0", port))
            self.server_socket.listen(1)
            self.server_socket.settimeout(1)
            self.running = True
            self.port = port
            self.listener_thread = threading.Thread(target=self._accept_loop, daemon=True)
            self.listener_thread.start()
            return True
        except Exception as e:
            print(f"Server start error: {e}")
            return False

    def stop_server(self):
        self.running = False
        self.connected = False
        self.confirmed = False
        self.denied = False
        self.connection_request = None
        try:
            if self.server_socket:
                self.server_socket.close()
        except:
            pass

    def _accept_loop(self):
        while self.running:
            try:
                client, addr = self.server_socket.accept()
                client.settimeout(10)
                try:
                    data = client.recv(4096)
                    if not data:
                        client.close()
                        continue
                    msg = json.loads(data.decode())
                except:
                    client.close()
                    continue
                if msg.get("type") != "auth" or msg.get("code") != self.code:
                    try:
                        client.sendall(json.dumps({"status": "error", "message": "Неверный код"}).encode())
                    except:
                        pass
                    client.close()
                    continue
                try:
                    client.sendall(json.dumps({"status": "waiting_confirmation"}).encode())
                except:
                    client.close()
                    continue
                with self.lock:
                    self.connection_request = client
                    self.client_addr = addr
                    self.confirmed = False
                    self.denied = False
                timeout = 30
                start = time.time()
                while time.time() - start < timeout:
                    with self.lock:
                        if self.confirmed:
                            self.connected = True
                            try:
                                client.sendall(json.dumps({"status": "confirmed"}).encode())
                            except:
                                self.connected = False
                                break
                            self.handler_thread = threading.Thread(target=self._handle_client, args=(client,), daemon=True)
                            self.handler_thread.start()
                            return
                        if self.denied:
                            try:
                                client.sendall(json.dumps({"status": "rejected"}).encode())
                            except:
                                pass
                            client.close()
                            with self.lock:
                                self.connection_request = None
                                self.confirmed = False
                                self.denied = False
                            break
                    time.sleep(0.1)
                else:
                    try:
                        client.close()
                    except:
                        pass
                    with self.lock:
                        self.connection_request = None
                        self.confirmed = False
                        self.denied = False
            except socket.timeout:
                continue
            except:
                if self.running:
                    time.sleep(0.1)

    def confirm_connection(self):
        with self.lock:
            self.confirmed = True

    def deny_connection(self):
        with self.lock:
            self.denied = True

    def _handle_client(self, client):
        try:
            client.settimeout(0.5)
            while self.running and self.connected:
                try:
                    data = client.recv(4096)
                    if not data:
                        break
                    cmd = json.loads(data.decode())
                    self._execute_command(cmd)
                except socket.timeout:
                    continue
                except:
                    break
        except:
            pass
        finally:
            self.connected = False
            self.confirmed = False
            self.denied = False
            try:
                client.close()
            except:
                pass

    def _execute_command(self, cmd):
        cmd_type = cmd.get("type")
        try:
            if cmd_type == "mouse_move":
                import pyautogui
                pyautogui.moveRel(cmd["dx"], cmd["dy"], duration=0.05)
            elif cmd_type == "mouse_click":
                import pyautogui
                pyautogui.click(button=cmd.get("button", "left"))
            elif cmd_type == "mouse_double_click":
                import pyautogui
                pyautogui.doubleClick()
            elif cmd_type == "mouse_scroll":
                import pyautogui
                pyautogui.scroll(cmd.get("clicks", 0))
            elif cmd_type == "key_press":
                import pyautogui
                pyautogui.write(cmd["text"])
            elif cmd_type == "key_hotkey":
                import pyautogui
                pyautogui.hotkey(*cmd["keys"])
        except Exception as e:
            print(f"Exec error: {e}")


# ═══════════════════════════════════════
# MAIN APPLICATION
# ═══════════════════════════════════════
def main(page: ft.Page):
    page.title = "ToolCheats Hunter — Клиент"
    page.theme_mode = ft.ThemeMode.DARK
    page.bgcolor = "#0f0f1a"
    page.padding = 0
    page.window_width = 1100
    page.window_height = 750
    page.window_resizable = True

    scanner = CheatScanner()
    server = RemoteServer()

    page.theme = ft.Theme(
        color_scheme=ft.ColorScheme(
            primary="#00d4ff", primary_container="#003d4d",
            secondary="#7c3aed", secondary_container="#2d1b69",
            surface="#1a1a2e", surface_variant="#1e293b",
            background="#0f0f1a", error="#ef4444",
            on_primary="#000000", on_surface="#e2e8f0",
            on_surface_variant="#94a3b8", on_background="#e2e8f0",
            outline="#334155",
        ),
        font_family="Segoe UI", use_material3=True,
    )

    def make_card(content, title=None, width=None, height=None):
        items = []
        if title:
            items.append(ft.Text(title, size=16, weight=ft.FontWeight.BOLD, color="#00d4ff"))
            items.append(ft.Container(height=12))
        items.append(content)
        return ft.Container(
            content=ft.Column(items, spacing=0),
            width=width, height=height,
            border_radius=12, bgcolor="#1a1a2e",
            border=ft.border.all(1, "#2d2d4a"), padding=20,
        )

    def status_badge(text, color="#00d4ff"):
        return ft.Container(
            content=ft.Row([
                ft.Container(width=8, height=8, border_radius=4, bgcolor=color),
                ft.Text(text, size=13, color=color),
            ], spacing=8),
            padding=ft.padding.only(left=4),
        )

    # ═══════════════════════════════════════
    # TAB 1 — DASHBOARD
    # ═══════════════════════════════════════
    dash_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)
    dash_scan_progress = ft.ProgressBar(width=None, value=0, color="#00d4ff", bgcolor="#1e293b", visible=False)
    dash_scan_status = ft.Text("", size=13, color="#94a3b8")

    def build_dashboard():
        dash_content.controls.clear()
        cpu_count = psutil.cpu_count(logical=True)
        mem = psutil.virtual_memory()

        sys_row = ft.Row([
            ft.Container(
                content=ft.Column([ft.Icon(ft.icons.MEMORY, color="#00d4ff", size=28), ft.Text("Процессор", size=12, color="#94a3b8"), ft.Text(f"{cpu_count} ядер", size=16, weight=ft.FontWeight.BOLD, color="#e2e8f0")], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=4),
                padding=15, border_radius=10, bgcolor="#16213e", border=ft.border.all(1, "#2d2d4a"), expand=1,
            ),
            ft.Container(
                content=ft.Column([ft.Icon(ft.icons.MEMORY_CHIP, color="#7c3aed", size=28), ft.Text("ОЗУ", size=12, color="#94a3b8"), ft.Text(f"{mem.total / (1024**3):.1f} GB", size=16, weight=ft.FontWeight.BOLD, color="#e2e8f0")], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=4),
                padding=15, border_radius=10, bgcolor="#16213e", border=ft.border.all(1, "#2d2d4a"), expand=1,
            ),
            ft.Container(
                content=ft.Column([ft.Icon(ft.icons.DNS, color="#f59e0b", size=28), ft.Text("ОС", size=12, color="#94a3b8"), ft.Text("Windows", size=14, weight=ft.FontWeight.BOLD, color="#e2e8f0")], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=4),
                padding=15, border_radius=10, bgcolor="#16213e", border=ft.border.all(1, "#2d2d4a"), expand=1,
            ),
        ], spacing=12)

        scan_btn = ft.ElevatedButton(
            content=ft.Row([ft.Icon(ft.icons.SHIELD, color="#000000"), ft.Text("Быстрое сканирование", color="#000000", weight=ft.FontWeight.BOLD)]),
            style=ft.ButtonStyle(bgcolor="#00d4ff", padding=ft.padding.symmetric(horizontal=24, vertical=14), shape=ft.RoundedRectangleBorder(10)),
            on_click=lambda e: start_scan(),
        )

        def start_scan():
            dash_scan_progress.visible = True
            dash_scan_progress.value = 0
            dash_scan_status.value = "Сканирование..."
            dash_scan_status.color = "#f59e0b"
            page.update()

            def progress(msg, value):
                dash_scan_progress.value = value
                dash_scan_status.value = msg
                page.update()

            def scan_thread():
                results = scanner.full_scan(progress_callback=progress)
                total, details = scanner.get_summary()
                page.after(0, lambda: finish_scan(total, details))

            def finish_scan(total, details):
                dash_scan_progress.visible = False
                dash_scan_status.value = "Чисто" if total == 0 else f"Найдено {total}"
                dash_scan_status.color = "#22c55e" if total == 0 else "#ef4444"
                page.update()

            threading.Thread(target=scan_thread, daemon=True).start()

        # Remote access panel on dashboard
        conn_panel = ft.Column(spacing=8)

        def update_conn_panel():
            if server.connected:
                color = "#22c55e"; text = "Подключён"
            elif server.running:
                color = "#f59e0b"; text = "Ожидание..."
            else:
                color = "#ef4444"; text = "Не активен"
            conn_panel.controls.clear()
            conn_panel.controls.extend([
                ft.Row([ft.Text("Код подключения:", size=14, color="#94a3b8")]),
                ft.Row([
                    ft.Container(
                        content=ft.Text(server.code, size=24, weight=ft.FontWeight.BOLD, color="#00d4ff", letter_spacing=4),
                        padding=12, border_radius=8, bgcolor="#0d0d1a", border=ft.border.all(1, "#00d4ff"), expand=True,
                    ),
                    ft.IconButton(icon=ft.icons.REFRESH, icon_color="#00d4ff", icon_size=24, tooltip="Обновить код", on_click=lambda e: (server.refresh_code(), update_conn_panel())),
                ], spacing=8),
                ft.Row([ft.Text(f"IP: {server.get_local_ip()}", size=13, color="#94a3b8"), ft.Text(f"Порт: 9090", size=13, color="#94a3b8")], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                status_badge(text, color),
            ])
            page.update()

        dash_content.controls.extend([
            ft.Text("Панель управления", size=24, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ft.Container(height=4, border_radius=2, gradient=ft.LinearGradient(colors=["#00d4ff", "#7c3aed"])),
            ft.Container(height=8),
            sys_row,
            ft.Container(height=4),
            ft.Row([
                make_card(ft.Column([scan_btn, ft.Container(height=16), dash_scan_progress, dash_scan_status], horizontal_alignment=ft.CrossAxisAlignment.CENTER), title="Сканер системы", height=200),
                make_card(conn_panel, title="Удалённый доступ", height=200),
            ], spacing=16, expand=1),
        ])
        update_conn_panel()

    # ═══════════════════════════════════════
    # TAB 2 — CHEAT SCANNER
    # ═══════════════════════════════════════
    scan_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)
    scan_progress = ft.ProgressBar(width=None, value=0, color="#00d4ff", bgcolor="#1e293b", visible=False)
    scan_status = ft.Text("", size=13, color="#94a3b8")
    scan_results = ft.Column(spacing=4, scroll=ft.ScrollMode.AUTO)

    def build_scanner_tab():
        scan_content.controls.clear()
        scan_results.controls.clear()

        def show_scan_results(results):
            scan_results.controls.clear()
            for scan_type, items, icon, color in [
                ("Процессы", results.get("processes", []), ft.icons.MEMORY, "#ef4444"),
                ("Файлы", results.get("files", []), ft.icons.DESCRIPTION, "#f59e0b"),
                ("Директории", results.get("directories", []), ft.icons.FOLDER, "#7c3aed"),
                ("Реестр", results.get("registry", []), ft.icons.REGISTRY, "#00d4ff"),
                ("Сеть", results.get("network", []), ft.icons.WIFI, "#22c55e"),
            ]:
                cards = ft.Column(spacing=4)
                if not items:
                    cards.controls.append(ft.Container(
                        content=ft.Row([ft.Icon(ft.icons.CHECK_CIRCLE, color="#22c55e", size=18), ft.Text(f"{scan_type}: чисто", size=13, color="#22c55e")]),
                        padding=8,
                    ))
                else:
                    for item in items[:10]:
                        name = item.get("name") or item.get("path", "").split("\\")[-1] or item.get("key", "")
                        cards.controls.append(ft.Container(
                            content=ft.Row([ft.Icon(icon, color=color, size=16), ft.Text(str(name), size=13, color="#e2e8f0", expand=True)]),
                            padding=ft.padding.symmetric(vertical=3, horizontal=8), border_radius=6, bgcolor="#1e293b",
                        ))
                    if len(items) > 10:
                        cards.controls.append(ft.Text(f"... и ещё {len(items) - 10}", size=12, color="#64748b"))
                scan_results.controls.append(make_card(cards, title=f"{scan_type} ({len(items)})"))
            page.update()

        def full_scan():
            scan_btn.disabled = True
            scan_btn.content.controls[1].value = "Сканирование..."
            scan_progress.visible = True
            scan_progress.value = 0
            scan_status.value = ""
            page.update()

            def progress(msg, value):
                scan_progress.value = value
                scan_status.value = msg
                page.update()

            def scan_thread():
                results = scanner.full_scan(progress_callback=progress)
                total, _ = scanner.get_summary()
                page.after(0, lambda: done(total, results))

            def done(total, results):
                scan_btn.disabled = False
                scan_btn.content.controls[1].value = "Запустить полное сканирование"
                scan_progress.visible = False
                scan_status.value = f"Найдено {total}" if total else "Всё чисто"
                scan_status.color = "#ef4444" if total else "#22c55e"
                show_scan_results(results)

            threading.Thread(target=scan_thread, daemon=True).start()

        scan_btn = ft.ElevatedButton(
            content=ft.Row([ft.Icon(ft.icons.SEARCH, color="#000000"), ft.Text("Запустить полное сканирование", color="#000000", weight=ft.FontWeight.BOLD)]),
            style=ft.ButtonStyle(bgcolor="#00d4ff", padding=ft.padding.symmetric(horizontal=24, vertical=14), shape=ft.RoundedRectangleBorder(10)),
            on_click=lambda e: full_scan(),
        )

        def quick_scan(scan_type):
            func_map = {"processes": scanner.scan_processes, "files": scanner.scan_files,
                        "registry": scanner.scan_registry, "network": scanner.scan_network}
            if scan_type in func_map:
                items = func_map[scan_type]()
                results = {k: (items if k == scan_type else scanner.results.get(k, [])) for k in scanner.results}
                show_scan_results(results)
                scan_status.value = f"Найдено {len(items)}" if items else "Всё чисто"
                scan_status.color = "#ef4444" if items else "#22c55e"
                page.update()

        quick_row = ft.Row([
            ft.ElevatedButton("Процессы", icon=ft.icons.MEMORY, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: quick_scan("processes")),
            ft.ElevatedButton("Файлы", icon=ft.icons.DESCRIPTION, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: quick_scan("files")),
            ft.ElevatedButton("Реестр", icon=ft.icons.REGISTRY, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: quick_scan("registry")),
            ft.ElevatedButton("Сеть", icon=ft.icons.WIFI, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: quick_scan("network")),
        ], wrap=True, spacing=8)

        scan_content.controls.extend([
            ft.Text("Сканер читов", size=24, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ft.Container(height=4, border_radius=2, gradient=ft.LinearGradient(colors=["#00d4ff", "#7c3aed"])),
            ft.Container(height=8),
            scan_btn, quick_row, scan_progress, scan_status,
            ft.Divider(color="#2d2d4a"),
            scan_results,
        ])

    # ═══════════════════════════════════════
    # TAB 3 — REMOTE ACCESS
    # ═══════════════════════════════════════
    remote_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)

    def build_remote_tab():
        remote_content.controls.clear()
        code_display = ft.Text(server.code, size=36, weight=ft.FontWeight.BOLD, color="#00d4ff", letter_spacing=8)
        ip_display = ft.Text(f"IP: {server.get_local_ip()}", size=15, color="#94a3b8")
        status_display = status_badge("Сервер не запущен", "#64748b")
        log_container = ft.Column(spacing=4, scroll=ft.ScrollMode.AUTO)

        def add_log(msg):
            ts = datetime.now().strftime("%H:%M:%S")
            log_container.controls.append(ft.Container(
                content=ft.Row([ft.Text(ts, size=11, color="#64748b"), ft.Text(msg, size=13, color="#e2e8f0")], spacing=10),
                padding=ft.padding.symmetric(vertical=4),
            ))
            if len(log_container.controls) > 50:
                log_container.controls.pop(0)
            page.update()

        def update_ui():
            code_display.value = server.code
            if not server.running:
                status_display.content.controls[0].bgcolor = "#64748b"
                status_display.content.controls[1].value = "Сервер не запущен"
            elif server.connected:
                status_display.content.controls[0].bgcolor = "#22c55e"
                status_display.content.controls[1].value = "Админ подключён"
            elif server.connection_request:
                status_display.content.controls[0].bgcolor = "#f59e0b"
                status_display.content.controls[1].value = "Запрос на подключение..."
            else:
                status_display.content.controls[0].bgcolor = "#00d4ff"
                status_display.content.controls[1].value = "Ожидание подключения..."
            page.update()

        def start_server_click(e):
            if not server.running:
                server.start_server(9090)
                start_btn.content.controls[0].name = ft.icons.STOP_CIRCLE
                start_btn.content.controls[1].value = "Остановить сервер"
                start_btn.style.bgcolor = "#ef4444"
                add_log("Сервер запущен")
            else:
                server.stop_server()
                start_btn.content.controls[0].name = ft.icons.PLAY_CIRCLE
                start_btn.content.controls[1].value = "Запустить сервер"
                start_btn.style.bgcolor = "#22c55e"
                add_log("Сервер остановлен")
            update_ui()

        start_btn = ft.ElevatedButton(
            content=ft.Row([ft.Icon(ft.icons.PLAY_CIRCLE, color="#000000"), ft.Text("Запустить сервер", color="#000000", weight=ft.FontWeight.BOLD)]),
            style=ft.ButtonStyle(bgcolor="#22c55e", padding=ft.padding.symmetric(horizontal=24, vertical=14), shape=ft.RoundedRectangleBorder(10)),
            on_click=start_server_click,
        )

        # Confirmation dialog
        req_addr = ft.Text("", size=14, color="#e2e8f0")
        request_dialog = ft.AlertDialog(
            modal=True,
            title=ft.Text("Запрос на подключение", size=18, weight=ft.FontWeight.BOLD),
            content=ft.Column([
                ft.Text("Администратор хочет подключиться к вашему ПК", size=14, color="#94a3b8"),
                ft.Container(height=8),
                req_addr,
            ], tight=True, spacing=0, width=300),
            actions=[
                ft.ElevatedButton("Отклонить", style=ft.ButtonStyle(bgcolor="#ef4444", color="#ffffff"), on_click=lambda e: deny()),
                ft.ElevatedButton("Разрешить", style=ft.ButtonStyle(bgcolor="#22c55e", color="#ffffff"), on_click=lambda e: accept()),
            ],
        )

        def accept():
            server.confirm_connection()
            request_dialog.open = False
            if request_dialog in page.overlay:
                page.overlay.remove(request_dialog)
            add_log("Подключение разрешено")
            update_ui()

        def deny():
            server.deny_connection()
            request_dialog.open = False
            if request_dialog in page.overlay:
                page.overlay.remove(request_dialog)
            add_log("Подключение отклонено")
            update_ui()

        def check_requests():
            if server.connection_request and not server.confirmed and not server.denied:
                if request_dialog not in page.overlay:
                    req_addr.value = f"IP: {server.client_addr[0]}:{server.client_addr[1]}"
                    page.overlay.append(request_dialog)
                    request_dialog.open = True
                    add_log(f"Входящий запрос от {server.client_addr[0]}")
                    page.update()

        def monitor_loop():
            while True:
                time.sleep(0.5)
                try:
                    page.after(0, check_requests)
                except:
                    pass

        threading.Thread(target=monitor_loop, daemon=True).start()

        remote_content.controls.extend([
            ft.Text("Удалённый доступ", size=24, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ft.Container(height=4, border_radius=2, gradient=ft.LinearGradient(colors=["#00d4ff", "#7c3aed"])),
            ft.Container(height=8),
            start_btn,
            ft.Row([
                make_card(ft.Column([
                    ft.Row([ft.Text("Код подключения:", size=14, color="#94a3b8")]),
                    ft.Container(height=8),
                    ft.Container(
                        content=ft.Row([code_display], alignment=ft.MainAxisAlignment.CENTER),
                        padding=20, border_radius=10, bgcolor="#0d0d1a", border=ft.border.all(2, "#00d4ff"),
                    ),
                    ft.Container(height=12),
                    ft.Row([
                        ft.ElevatedButton("Обновить код", icon=ft.icons.REFRESH, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: (server.refresh_code(), update_ui())),
                        ft.ElevatedButton("Скопировать", icon=ft.icons.COPY, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: page.set_clipboard(server.code)),
                    ], alignment=ft.MainAxisAlignment.CENTER),
                ]), width=None),
                make_card(ft.Column([
                    ip_display, ft.Container(height=8),
                    ft.Text("Порт: 9090", size=14, color="#94a3b8"), ft.Container(height=8),
                    status_display, ft.Container(height=8),
                    ft.Text("Запустите сервер и передайте код администратору.\nАдминистратор введёт код и получит доступ к управлению.", size=12, color="#64748b"),
                ]), width=None),
            ], spacing=16, vertical_alignment=ft.CrossAxisAlignment.START),
            make_card(log_container, title="Журнал подключений", height=250),
        ])
        update_ui()

    # ═══════════════════════════════════════
    # BUILD
    # ═══════════════════════════════════════
    build_dashboard()
    build_scanner_tab()
    build_remote_tab()

    tabs = ft.Tabs(selected_index=0, animation_duration=300, tabs=[
        ft.Tab(text="Панель", icon=ft.icons.DASHBOARD, content=ft.Container(dash_content, padding=20)),
        ft.Tab(text="Сканер", icon=ft.icons.SHIELD, content=ft.Container(scan_content, padding=20)),
        ft.Tab(text="Удалённый доступ", icon=ft.icons.CAST_CONNECTED, content=ft.Container(remote_content, padding=20)),
    ], expand=1)

    page.appbar = ft.AppBar(
        title=ft.Row([ft.Icon(ft.icons.SHIELD_MOON, color="#00d4ff", size=28), ft.Text("ToolCheats Hunter", size=20, weight=ft.FontWeight.BOLD, color="#e2e8f0")], spacing=10),
        bgcolor="#0f0f1a",
        actions=[ft.Container(content=ft.Text("Клиент", size=13, color="#00d4ff", weight=ft.FontWeight.BOLD), padding=ft.padding.symmetric(horizontal=12, vertical=6), border_radius=20, border=ft.border.all(1, "#00d4ff")), ft.Container(width=10)],
    )
    page.add(tabs)
    page.update()


if __name__ == "__main__":
    ft.app(target=main)
