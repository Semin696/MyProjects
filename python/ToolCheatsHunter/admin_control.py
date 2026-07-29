import flet as ft
import socket
import json
import threading
import time
from datetime import datetime

I = ft.icons.Icons
B = ft.border

class AdminClient:
    def __init__(self):
        self.sock = None
        self.connected = False
        self.authenticated = False
        self.running = False

    def connect(self, ip, port, code):
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.settimeout(5)
            self.sock.connect((ip, port))
            auth = json.dumps({"type":"auth","code":code}).encode()
            self.sock.sendall(auth)
            resp = json.loads(self.sock.recv(1024).decode())
            if resp.get("status") == "waiting_confirmation":
                self.authenticated = True
                self.connected = True
                self.running = True
                threading.Thread(target=self._recv, daemon=True).start()
                return "waiting", "Ожидание подтверждения..."
            elif resp.get("status") == "confirmed":
                self.authenticated = True; self.connected = True; self.running = True
                threading.Thread(target=self._recv, daemon=True).start()
                return "connected", "Подключено!"
            elif resp.get("status") == "rejected":
                self.disconnect(); return "rejected", "Отклонено пользователем"
            else:
                self.disconnect(); return "error", resp.get("message","Ошибка")
        except socket.timeout: return "error", "Таймаут"
        except ConnectionRefusedError: return "error", "Соединение отклонено"
        except Exception as e: return "error", str(e)

    def _recv(self):
        self.sock.settimeout(0.5)
        while self.running and self.connected:
            try:
                d = self.sock.recv(4096)
                if not d: break
            except socket.timeout: continue
            except: break

    def send(self, cmd):
        if not self.connected or not self.sock: return False
        try: self.sock.sendall(json.dumps(cmd).encode()); return True
        except: self.connected = False; return False

    def mouse_move(self, dx, dy): self.send({"type":"mouse_move","dx":dx,"dy":dy})
    def mouse_click(self, b="left"): self.send({"type":"mouse_click","button":b})
    def mouse_double(self): self.send({"type":"mouse_double_click"})
    def mouse_scroll(self, n): self.send({"type":"mouse_scroll","clicks":n})
    def key_press(self, t): self.send({"type":"key_press","text":t})
    def key_hotkey(self, k): self.send({"type":"key_hotkey","keys":k})

    def disconnect(self):
        self.running = False; self.connected = False; self.authenticated = False
        try:
            if self.sock: self.sock.close()
        except: pass

def main(page: ft.Page):
    page.title = "ToolCheats Hunter - Админ"
    page.theme_mode = ft.ThemeMode.DARK
    page.bgcolor = "#0f0f1a"
    page.padding = 0
    page.window_width = 1100
    page.window_height = 750
    page.window_resizable = True
    page.theme = ft.Theme(color_scheme=ft.ColorScheme(primary="#7c3aed", secondary="#00d4ff", error="#ef4444"), font_family="Segoe UI", use_material3=True)

    admin = AdminClient()

    def card(content, title=None, h=None, width=None):
        items = []
        if title:
            items.append(ft.Text(title, size=16, weight=ft.FontWeight.BOLD, color="#7c3aed"))
            items.append(ft.Container(height=12))
        items.append(content)
        return ft.Container(content=ft.Column(items, spacing=0), border_radius=12, bgcolor="#1a1a2e", border=ft.border.Border(left=B.BorderSide(1,"#2d2d4a"),right=B.BorderSide(1,"#2d2d4a"),top=B.BorderSide(1,"#2d2d4a"),bottom=B.BorderSide(1,"#2d2d4a")), padding=20, height=h, width=width)

    # ─── Connection UI ───
    ip_f = ft.TextField(label="IP-адрес", hint_text="192.168.1.100", width=250, border_color="#334155", focused_border_color="#7c3aed", bgcolor="#1a1a2e", color="#e2e8f0", label_style=ft.TextStyle(color="#94a3b8"))
    port_f = ft.TextField(label="Порт", value="9090", width=120, border_color="#334155", focused_border_color="#7c3aed", bgcolor="#1a1a2e", color="#e2e8f0", label_style=ft.TextStyle(color="#94a3b8"))
    code_f = ft.TextField(label="Код", hint_text="XXXX-XXXX", width=250, border_color="#334155", focused_border_color="#7c3aed", bgcolor="#1a1a2e", color="#e2e8f0", label_style=ft.TextStyle(color="#94a3b8"), password=True, can_reveal_password=True)
    conn_stat = ft.Text("", size=14, color="#94a3b8")

    conn_ind = ft.Container(content=ft.Row([ft.Container(width=10,height=10,border_radius=5,bgcolor="#64748b"),ft.Text("Не подключено",size=13,color="#64748b")],spacing=8),padding=ft.padding.Padding.symmetric(horizontal=12,vertical=6),border_radius=20,border=ft.border.Border(left=B.BorderSide(1,"#334155"),right=B.BorderSide(1,"#334155"),top=B.BorderSide(1,"#334155"),bottom=B.BorderSide(1,"#334155")))

    conn_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)
    ctrl_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)

    def build_conn():
        conn_content.controls.clear()
        conn_btn = ft.Button(content=ft.Row([ft.Icon(I.CAST_CONNECTED,color="#ffffff"),ft.Text("Подключиться",color="#ffffff",weight=ft.FontWeight.BOLD)]),style=ft.ButtonStyle(bgcolor="#7c3aed",padding=ft.padding.Padding.symmetric(horizontal=24,vertical=14),shape=ft.RoundedRectangleBorder(10)),on_click=lambda e: do_conn())
        conn_content.controls.extend([ft.Text("Подключение к клиенту",size=24,weight=ft.FontWeight.BOLD,color="#e2e8f0"),ft.Container(height=4,border_radius=2,gradient=ft.LinearGradient(colors=["#7c3aed","#00d4ff"])),ft.Container(height=8),card(ft.Column([ft.Row([ip_f,port_f],spacing=12),ft.Container(height=8),code_f,ft.Container(height=12),conn_btn,ft.Container(height=12),conn_stat]))])

    def build_ctrl():
        ctrl_content.controls.clear()
        mouse_stat = ft.Text("", size=12, color="#64748b")

        mp = ft.Container(
            content=ft.Column([ft.Icon(I.TOUCH_APP,color="#4a4a6a",size=48),ft.Text("Область мыши",size=14,color="#4a4a6a"),ft.Text("Перетаскивайте для перемещения",size=11,color="#3a3a5a")],horizontal_alignment=ft.CrossAxisAlignment.CENTER,alignment=ft.MainAxisAlignment.CENTER),
            width=500, height=350, border_radius=12,
            border=ft.border.Border(left=B.BorderSide(2,"#2d2d4a"),right=B.BorderSide(2,"#2d2d4a"),top=B.BorderSide(2,"#2d2d4a"),bottom=B.BorderSide(2,"#2d2d4a")),
            bgcolor="#12121e",
        )
        md = [False, (0,0)]

        def on_down(e):
            md[0] = True; md[1] = (e.local_x, e.local_y)
            mp.border = ft.border.Border(left=B.BorderSide(2,"#7c3aed"),right=B.BorderSide(2,"#7c3aed"),top=B.BorderSide(2,"#7c3aed"),bottom=B.BorderSide(2,"#7c3aed"))
            mp.bgcolor = "#1a1a30"; mouse_stat.value = "Мышь активна"; mouse_stat.color = "#7c3aed"
            if admin.connected: admin.mouse_click("left")
            page.update()

        def on_up(e):
            md[0] = False
            mp.border = ft.border.Border(left=B.BorderSide(2,"#2d2d4a"),right=B.BorderSide(2,"#2d2d4a"),top=B.BorderSide(2,"#2d2d4a"),bottom=B.BorderSide(2,"#2d2d4a"))
            mp.bgcolor = "#12121e"; mouse_stat.value = ""; page.update()

        def on_move(e):
            if md[0]:
                dx = int(e.local_x - md[1][0]); dy = int(e.local_y - md[1][1])
                if admin.connected and (abs(dx)>2 or abs(dy)>2):
                    admin.mouse_move(dx*2, dy*2); md[1] = (e.local_x, e.local_y)
                    mouse_stat.value = f"x:{int(e.local_x)} y:{int(e.local_y)}"; page.update()

        mp.on_pointer_down = on_down; mp.on_pointer_up = on_up; mp.on_pan_update = on_move

        keys_inp = ft.TextField(label="Ввод текста",hint_text="Текст и Enter...",width=500,border_color="#334155",focused_border_color="#7c3aed",bgcolor="#1a1a2e",color="#e2e8f0",label_style=ft.TextStyle(color="#94a3b8"),on_submit=lambda e: send_k())

        hk_inp = ft.TextField(label="Горячие клавиши (через +)",hint_text="ctrl+shift+esc",width=300,border_color="#334155",focused_border_color="#7c3aed",bgcolor="#1a1a2e",color="#e2e8f0",label_style=ft.TextStyle(color="#94a3b8"),on_submit=lambda e: send_hk())

        def send_k():
            if admin.connected and keys_inp.value: admin.key_press(keys_inp.value); keys_inp.value = ""; page.update()
        def send_hk():
            if admin.connected and hk_inp.value:
                admin.key_hotkey([k.strip().lower() for k in hk_inp.value.split("+")]); hk_inp.value = ""; page.update()

        ctrl_content.controls.extend([
            ft.Text("Управление",size=24,weight=ft.FontWeight.BOLD,color="#e2e8f0"),ft.Container(height=4,border_radius=2,gradient=ft.LinearGradient(colors=["#7c3aed","#00d4ff"])),ft.Container(height=8),
            card(ft.Column([
                ft.Text("Мышь",size=16,weight=ft.FontWeight.BOLD,color="#7c3aed"),ft.Container(height=8),mp,ft.Container(height=8),mouse_stat,ft.Container(height=8),
                ft.Row([ft.Button("Левый",icon=I.MOUSE,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8),padding=ft.padding.Padding.symmetric(horizontal=16,vertical=10)),on_click=lambda e:admin.mouse_click("left") if admin.connected else None),ft.Button("Правый",icon=I.MOUSE,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8),padding=ft.padding.Padding.symmetric(horizontal=16,vertical=10)),on_click=lambda e:admin.mouse_click("right") if admin.connected else None),ft.Button("Двойной",icon=I.ADS_CLICK,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8),padding=ft.padding.Padding.symmetric(horizontal=16,vertical=10)),on_click=lambda e:admin.mouse_double() if admin.connected else None),ft.Button("Вверх",icon=I.KEYBOARD_ARROW_UP,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8),padding=ft.padding.Padding.symmetric(horizontal=16,vertical=10)),on_click=lambda e:admin.mouse_scroll(3) if admin.connected else None),ft.Button("Вниз",icon=I.KEYBOARD_ARROW_DOWN,style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8),padding=ft.padding.Padding.symmetric(horizontal=16,vertical=10)),on_click=lambda e:admin.mouse_scroll(-3) if admin.connected else None)],wrap=True,spacing=8),
            ])),
            card(ft.Column([ft.Text("Клавиатура",size=16,weight=ft.FontWeight.BOLD,color="#7c3aed"),ft.Container(height=8),keys_inp,ft.Container(height=4),ft.Text("Enter для отправки",size=11,color="#64748b")])),
            card(ft.Column([
                ft.Text("Горячие клавиши",size=16,weight=ft.FontWeight.BOLD,color="#7c3aed"),ft.Container(height=8),
                ft.Row([ft.Button("Ctrl+Alt+Del",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["ctrl","alt","delete"]) if admin.connected else None),ft.Button("Ctrl+Shift+Esc",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["ctrl","shift","esc"]) if admin.connected else None),ft.Button("Win+R",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["win","r"]) if admin.connected else None),ft.Button("Win+D",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["win","d"]) if admin.connected else None),ft.Button("Alt+F4",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["alt","f4"]) if admin.connected else None),ft.Button("Ctrl+C",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["ctrl","c"]) if admin.connected else None),ft.Button("Ctrl+V",style=ft.ButtonStyle(bgcolor="#1e293b",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["ctrl","v"]) if admin.connected else None)],wrap=True,spacing=8),
                ft.Container(height=12), hk_inp, ft.Container(height=4), ft.Text("Пример: ctrl+shift+esc, win+r",size=11,color="#64748b"),
            ])),
            ft.Container(height=8),
            ft.Button(content=ft.Row([ft.Icon(I.LINK_OFF,color="#ffffff"),ft.Text("Отключиться",color="#ffffff",weight=ft.FontWeight.BOLD)]),style=ft.ButtonStyle(bgcolor="#ef4444",padding=ft.padding.Padding.symmetric(horizontal=24,vertical=14),shape=ft.RoundedRectangleBorder(10)),on_click=lambda e: do_disc()),
        ])

    def do_conn():
        ip = ip_f.value.strip(); port = port_f.value.strip(); code = code_f.value.strip()
        if not ip: conn_stat.value = "Введите IP"; conn_stat.color = "#ef4444"; page.update(); return
        if not code: conn_stat.value = "Введите код"; conn_stat.color = "#ef4444"; page.update(); return
        try: p = int(port)
        except: conn_stat.value = "Некорректный порт"; conn_stat.color = "#ef4444"; page.update(); return
        conn_stat.value = "Подключение..."; conn_stat.color = "#f59e0b"; page.update()

        def t():
            status, msg = admin.connect(ip, p, code)
            page.run_thread(lambda: res(status, msg))

        threading.Thread(target=t, daemon=True).start()

    def res(status, msg):
        if status == "waiting":
            conn_stat.value = msg; conn_stat.color = "#f59e0b"
            conn_ind.content.controls[0].bgcolor = "#f59e0b"; conn_ind.content.controls[1].value = "Ожидание..."; conn_ind.content.controls[1].color = "#f59e0b"
            def wait_confirm():
                for _ in range(60):
                    if admin.connected and admin.authenticated: page.run_thread(on_conn); return
                    if not admin.connected: page.run_thread(lambda: res("error", "Отклонено")); return
                    time.sleep(0.5)
                page.run_thread(lambda: res("error", "Таймаут"))
            threading.Thread(target=wait_confirm, daemon=True).start()
        elif status == "connected": on_conn()
        elif status == "rejected": conn_stat.value = msg; conn_stat.color = "#ef4444"
        else: conn_stat.value = f"Ошибка: {msg}"; conn_stat.color = "#ef4444"
        page.update()

    nav_index = [0]

    def on_conn():
        conn_stat.value = "Подключено!"; conn_stat.color = "#22c55e"
        conn_ind.content.controls[0].bgcolor = "#22c55e"; conn_ind.content.controls[1].value = "Подключено"; conn_ind.content.controls[1].color = "#22c55e"
        nav_index[0] = 1; admin_content.content = ctrl_pages[1]; page.update()

    def do_disc():
        admin.disconnect()
        conn_stat.value = "Отключено"; conn_stat.color = "#64748b"
        conn_ind.content.controls[0].bgcolor = "#64748b"; conn_ind.content.controls[1].value = "Не подключено"; conn_ind.content.controls[1].color = "#64748b"
        nav_index[0] = 0; admin_content.content = ctrl_pages[0]; page.update()

    build_conn(); build_ctrl()

    ctrl_pages = [ft.Container(conn_content, padding=20), ft.Container(ctrl_content, padding=20)]
    admin_content = ft.Container(content=ctrl_pages[0], expand=True)

    def switch_admin(i):
        nav_index[0] = i; admin_content.content = ctrl_pages[i]; page.update()

    page.appbar = ft.AppBar(title=ft.Row([ft.Icon(I.SHIELD_MOON,color="#7c3aed",size=28),ft.Text("ToolCheats Hunter",size=20,weight=ft.FontWeight.BOLD,color="#e2e8f0")],spacing=10),bgcolor="#0f0f1a",actions=[conn_ind,ft.Container(width=10)])
    page.add(admin_content)
    page.navigation_bar = ft.NavigationBar(
        destinations=[
            ft.NavigationBarDestination(icon=I.CAST, label="Подключение"),
            ft.NavigationBarDestination(icon=I.GAMEPAD, label="Управление"),
        ],
        selected_index=0, bgcolor="#1a1a2e", on_change=lambda e: switch_admin(e.control.selected_index),
    )
    page.update()

if __name__ == "__main__":
    ft.run(main)


