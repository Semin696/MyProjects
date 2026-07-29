import flet as ft
import socket
import json
import threading
import time
import struct
from datetime import datetime

class AdminClient:
    def __init__(self):
        self.sock = None
        self.connected = False
        self.authenticated = False
        self.running = False
        self.recv_thread = None

    def connect(self, ip, port, code):
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.settimeout(5)
            self.sock.connect((ip, port))
            # Send auth code
            auth_msg = json.dumps({"type": "auth", "code": code}).encode()
            self.sock.sendall(auth_msg)
            # Wait for response
            response = self.sock.recv(1024)
            resp = json.loads(response.decode())
            if resp.get("status") == "waiting_confirmation":
                self.authenticated = True
                self.connected = True
                self.running = True
                self.recv_thread = threading.Thread(target=self._recv_loop, daemon=True)
                self.recv_thread.start()
                return "waiting", "Ожидание подтверждения пользователя..."
            elif resp.get("status") == "confirmed":
                self.authenticated = True
                self.connected = True
                self.running = True
                self.recv_thread = threading.Thread(target=self._recv_loop, daemon=True)
                self.recv_thread.start()
                return "connected", "Подключение установлено!"
            elif resp.get("status") == "rejected":
                self.disconnect()
                return "rejected", "Подключение отклонено пользователем"
            else:
                self.disconnect()
                return "error", resp.get("message", "Неизвестная ошибка")
        except socket.timeout:
            return "error", "Таймаут подключения"
        except ConnectionRefusedError:
            return "error", "Соединение отклонено. Проверьте IP и порт"
        except Exception as e:
            return "error", str(e)

    def _recv_loop(self):
        self.sock.settimeout(0.5)
        while self.running and self.connected:
            try:
                data = self.sock.recv(4096)
                if not data:
                    break
            except socket.timeout:
                continue
            except:
                break

    def send_command(self, cmd):
        if not self.connected or not self.sock:
            return False
        try:
            self.sock.sendall(json.dumps(cmd).encode())
            return True
        except:
            self.connected = False
            return False

    def mouse_move(self, dx, dy):
        self.send_command({"type": "mouse_move", "dx": dx, "dy": dy})

    def mouse_click(self, button="left"):
        self.send_command({"type": "mouse_click", "button": button})

    def mouse_double_click(self):
        self.send_command({"type": "mouse_double_click"})

    def mouse_scroll(self, clicks):
        self.send_command({"type": "mouse_scroll", "clicks": clicks})

    def key_press(self, text):
        self.send_command({"type": "key_press", "text": text})

    def key_hotkey(self, keys):
        self.send_command({"type": "key_hotkey", "keys": keys})

    def disconnect(self):
        self.running = False
        self.connected = False
        self.authenticated = False
        try:
            if self.sock:
                self.sock.close()
        except:
            pass


def main(page: ft.Page):
    page.title = "ToolCheats Hunter — Админ"
    page.theme_mode = ft.ThemeMode.DARK
    page.bgcolor = "#0f0f1a"
    page.padding = 0
    page.window_width = 1100
    page.window_height = 750
    page.window_resizable = True

    admin = AdminClient()

    # ─── Theme ───
    page.theme = ft.Theme(
        color_scheme=ft.ColorScheme(
            primary="#7c3aed",
            primary_container="#2d1b69",
            secondary="#00d4ff",
            secondary_container="#003d4d",
            surface="#1a1a2e",
            surface_variant="#1e293b",
            background="#0f0f1a",
            error="#ef4444",
            on_primary="#ffffff",
            on_surface="#e2e8f0",
            on_surface_variant="#94a3b8",
            on_background="#e2e8f0",
            outline="#334155",
        ),
        font_family="Segoe UI",
        use_material3=True,
    )

    CARD_STYLE = {
        "border_radius": 12,
        "bgcolor": "#1a1a2e",
        "border": ft.border.all(1, "#2d2d4a"),
        "padding": 20,
    }

    def make_card(content, title=None, width=None, height=None):
        items = []
        if title:
            items.append(ft.Text(title, size=16, weight=ft.FontWeight.BOLD, color="#7c3aed"))
            items.append(ft.Container(height=12))
        items.append(content)
        return ft.Container(
            content=ft.Column(items, spacing=0),
            width=width, height=height,
            **CARD_STYLE,
        )

    def status_badge(text, color="#7c3aed"):
        return ft.Container(
            content=ft.Row(
                [
                    ft.Container(width=8, height=8, border_radius=4, bgcolor=color),
                    ft.Text(text, size=13, color=color),
                ],
                spacing=8,
            ),
            padding=ft.padding.only(left=4),
        )

    # ─── Connection UI ───
    ip_field = ft.TextField(
        label="IP-адрес",
        hint_text="192.168.1.100",
        width=250,
        border_color="#334155",
        focused_border_color="#7c3aed",
        bgcolor="#1a1a2e",
        color="#e2e8f0",
        label_style=ft.TextStyle(color="#94a3b8"),
    )
    port_field = ft.TextField(
        label="Порт",
        value="9090",
        width=120,
        border_color="#334155",
        focused_border_color="#7c3aed",
        bgcolor="#1a1a2e",
        color="#e2e8f0",
        label_style=ft.TextStyle(color="#94a3b8"),
    )
    code_field = ft.TextField(
        label="Код подключения",
        hint_text="XXXX-XXXX",
        width=250,
        border_color="#334155",
        focused_border_color="#7c3aed",
        bgcolor="#1a1a2e",
        color="#e2e8f0",
        label_style=ft.TextStyle(color="#94a3b8"),
        password=True,
        can_reveal_password=True,
    )
    connection_status = ft.Text("", size=14, color="#94a3b8")
    connect_btn = ft.ElevatedButton(
        content=ft.Row([
            ft.Icon(ft.icons.CAST_CONNECTED, color="#ffffff"),
            ft.Text("Подключиться", color="#ffffff", weight=ft.FontWeight.BOLD),
        ]),
        style=ft.ButtonStyle(bgcolor="#7c3aed", padding=ft.padding.symmetric(horizontal=24, vertical=14), shape=ft.RoundedRectangleBorder(10)),
        on_click=lambda e: do_connect(),
    )

    # ─── Control UI ───
    mouse_status = ft.Text("", size=12, color="#64748b")
    mouse_pad = ft.Container(
        content=ft.Column([
            ft.Icon(ft.icons.TOUCH_APP, color="#4a4a6a", size=48),
            ft.Text("Область управления мышью", size=14, color="#4a4a6a"),
            ft.Text("Нажмите и перетаскивайте для перемещения", size=11, color="#3a3a5a"),
        ], horizontal_alignment=ft.CrossAxisAlignment.CENTER, alignment=ft.MainAxisAlignment.CENTER),
        width=500, height=350,
        border_radius=12,
        border=ft.border.all(2, "#2d2d4a"),
        bgcolor="#12121e",
        animate=ft.animation.Animation(200, "ease"),
    )

    keys_input = ft.TextField(
        label="Ввод текста",
        hint_text="Введите текст и нажмите Enter...",
        width=500,
        border_color="#334155",
        focused_border_color="#7c3aed",
        bgcolor="#1a1a2e",
        color="#e2e8f0",
        label_style=ft.TextStyle(color="#94a3b8"),
        on_submit=lambda e: send_keys(),
    )

    hotkey_input = ft.TextField(
        label="Горячие клавиши (через +)",
        hint_text="ctrl+shift+esc",
        width=300,
        border_color="#334155",
        focused_border_color="#7c3aed",
        bgcolor="#1a1a2e",
        color="#e2e8f0",
        label_style=ft.TextStyle(color="#94a3b8"),
        on_submit=lambda e: send_hotkey(),
    )

    control_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)
    connection_content = ft.Column(spacing=16, scroll=ft.ScrollMode.AUTO)

    # ─── Mouse handling ───
    mouse_down_pos = None
    is_mouse_down = False

    def on_mouse_down(e):
        nonlocal is_mouse_down, mouse_down_pos
        is_mouse_down = True
        mouse_down_pos = (e.local_x, e.local_y)
        mouse_pad.border = ft.border.all(2, "#7c3aed")
        mouse_pad.bgcolor = "#1a1a30"
        mouse_status.value = "Мышь активна"
        mouse_status.color = "#7c3aed"
        # Click on mouse down position
        if admin.connected:
            admin.mouse_click("left")
        page.update()

    def on_mouse_up(e):
        nonlocal is_mouse_down
        is_mouse_down = False
        mouse_pad.border = ft.border.all(2, "#2d2d4a")
        mouse_pad.bgcolor = "#12121e"
        mouse_status.value = ""
        page.update()

    def on_mouse_move(e):
        if is_mouse_down and mouse_down_pos:
            dx = int(e.local_x - mouse_down_pos[0])
            dy = int(e.local_y - mouse_down_pos[1])
            if admin.connected and (abs(dx) > 2 or abs(dy) > 2):
                admin.mouse_move(dx * 2, dy * 2)
                mouse_down_pos = (e.local_x, e.local_y)
                mouse_status.value = f"x: {int(e.local_x)}, y: {int(e.local_y)}"
                page.update()

    # ─── Control buttons ───
    click_buttons = ft.Row(
        [
            ft.ElevatedButton("Левый клик", icon=ft.icons.MOUSE, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8), padding=ft.padding.symmetric(horizontal=16, vertical=10)), on_click=lambda e: admin.mouse_click("left") if admin.connected else None),
            ft.ElevatedButton("Правый клик", icon=ft.icons.MOUSE, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8), padding=ft.padding.symmetric(horizontal=16, vertical=10)), on_click=lambda e: admin.mouse_click("right") if admin.connected else None),
            ft.ElevatedButton("Двойной клик", icon=ft.icons.ONETOUCH, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8), padding=ft.padding.symmetric(horizontal=16, vertical=10)), on_click=lambda e: admin.mouse_double_click() if admin.connected else None),
            ft.ElevatedButton("Колёсико вверх", icon=ft.icons.KEYBOARD_ARROW_UP, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8), padding=ft.padding.symmetric(horizontal=16, vertical=10)), on_click=lambda e: admin.mouse_scroll(3) if admin.connected else None),
            ft.ElevatedButton("Колёсико вниз", icon=ft.icons.KEYBOARD_ARROW_DOWN, style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8), padding=ft.padding.symmetric(horizontal=16, vertical=10)), on_click=lambda e: admin.mouse_scroll(-3) if admin.connected else None),
        ],
        wrap=True, spacing=8,
    )

    hotkey_buttons = ft.Row(
        [
            ft.ElevatedButton("Ctrl+Alt+Del", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["ctrl", "alt", "delete"]) if admin.connected else None),
            ft.ElevatedButton("Ctrl+Shift+Esc", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["ctrl", "shift", "esc"]) if admin.connected else None),
            ft.ElevatedButton("Win+R", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["win", "r"]) if admin.connected else None),
            ft.ElevatedButton("Win+D", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["win", "d"]) if admin.connected else None),
            ft.ElevatedButton("Alt+F4", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["alt", "f4"]) if admin.connected else None),
            ft.ElevatedButton("Ctrl+C", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["ctrl", "c"]) if admin.connected else None),
            ft.ElevatedButton("Ctrl+V", style=ft.ButtonStyle(bgcolor="#1e293b", color="#e2e8f0", shape=ft.RoundedRectangleBorder(8)), on_click=lambda e: admin.key_hotkey(["ctrl", "v"]) if admin.connected else None),
        ],
        wrap=True, spacing=8,
    )

    def send_keys():
        if admin.connected and keys_input.value:
            admin.key_press(keys_input.value)
            keys_input.value = ""
            page.update()

    def send_hotkey():
        if admin.connected and hotkey_input.value:
            keys = [k.strip().lower() for k in hotkey_input.value.split("+")]
            admin.key_hotkey(keys)
            hotkey_input.value = ""
            page.update()

    # ═══════════════════════════════════════
    # BUILD INTERFACES
    # ═══════════════════════════════════════

    def build_connection_ui():
        connection_content.controls.clear()
        connection_content.controls.extend([
            ft.Text("Подключение к клиенту", size=24, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ft.Container(height=4, border_radius=2, gradient=ft.LinearGradient(colors=["#7c3aed", "#00d4ff"])),
            ft.Container(height=8),
            make_card(
                ft.Column([
                    ft.Row([ip_field, port_field], spacing=12),
                    ft.Container(height=8),
                    code_field,
                    ft.Container(height=12),
                    connect_btn,
                    ft.Container(height=12),
                    connection_status,
                ]),
                width=None,
            ),
        ])

    def build_control_ui():
        control_content.controls.clear()
        control_content.controls.extend([
            ft.Text("Управление", size=24, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ft.Container(height=4, border_radius=2, gradient=ft.LinearGradient(colors=["#7c3aed", "#00d4ff"])),
            ft.Container(height=8),

            # Mouse pad card
            make_card(
                ft.Column([
                    ft.Text("Мышь", size=16, weight=ft.FontWeight.BOLD, color="#7c3aed"),
                    ft.Container(height=8),
                    ft.Container(
                        content=mouse_pad,
                        on_hover=lambda e: None,
                    ),
                    ft.Container(height=8),
                    mouse_status,
                    ft.Container(height=8),
                    click_buttons,
                ]),
                width=None,
            ),

            # Keyboard card
            make_card(
                ft.Column([
                    ft.Text("Клавиатура", size=16, weight=ft.FontWeight.BOLD, color="#7c3aed"),
                    ft.Container(height=8),
                    keys_input,
                    ft.Container(height=4),
                    ft.Text("Нажмите Enter для отправки текста", size=11, color="#64748b"),
                ]),
                width=None,
            ),

            # Hotkeys card
            make_card(
                ft.Column([
                    ft.Text("Горячие клавиши", size=16, weight=ft.FontWeight.BOLD, color="#7c3aed"),
                    ft.Container(height=8),
                    hotkey_buttons,
                    ft.Container(height=12),
                    hotkey_input,
                    ft.Container(height=4),
                    ft.Text("Пример: ctrl+shift+esc, win+r, alt+f4", size=11, color="#64748b"),
                ]),
                width=None,
            ),

            # Disconnect button
            ft.Container(height=8),
            ft.ElevatedButton(
                content=ft.Row([
                    ft.Icon(ft.icons.LINK_OFF, color="#ffffff"),
                    ft.Text("Отключиться", color="#ffffff", weight=ft.FontWeight.BOLD),
                ]),
                style=ft.ButtonStyle(bgcolor="#ef4444", padding=ft.padding.symmetric(horizontal=24, vertical=14), shape=ft.RoundedRectangleBorder(10)),
                on_click=lambda e: do_disconnect(),
            ),
        ])

    build_connection_ui()
    build_control_ui()

    # ─── Connection logic ───
    def do_connect():
        ip = ip_field.value.strip()
        port_str = port_field.value.strip()
        code = code_field.value.strip()

        if not ip:
            connection_status.value = "Введите IP-адрес"
            connection_status.color = "#ef4444"
            page.update()
            return
        if not code:
            connection_status.value = "Введите код подключения"
            connection_status.color = "#ef4444"
            page.update()
            return

        try:
            port = int(port_str)
        except:
            connection_status.value = "Некорректный порт"
            connection_status.color = "#ef4444"
            page.update()
            return

        connection_status.value = "Подключение..."
        connection_status.color = "#f59e0b"
        connect_btn.disabled = True
        page.update()

        def connect_thread():
            status, message = admin.connect(ip, port, code)
            page.after(0, lambda: handle_connect_result(status, message))

        threading.Thread(target=connect_thread, daemon=True).start()

    def handle_connect_result(status, message):
        connect_btn.disabled = False
        if status == "waiting":
            connection_status.value = message
            connection_status.color = "#f59e0b"
            # Show waiting status
            conn_indicator.content.controls[0].bgcolor = "#f59e0b"
            conn_indicator.content.controls[1].value = "Ожидание подтверждения..."
            conn_indicator.content.controls[1].color = "#f59e0b"
            # Check periodically if connection established
            def wait_for_confirm():
                timeout = 30
                start = time.time()
                while time.time() - start < timeout:
                    if admin.connected and admin.authenticated:
                        page.after(0, lambda: on_connected())
                        return
                    if not admin.connected:
                        page.after(0, lambda: handle_connect_result("error", "Подключение отклонено или разорвано"))
                        return
                    time.sleep(0.5)
                page.after(0, lambda: handle_connect_result("error", "Время ожидания истекло"))
            threading.Thread(target=wait_for_confirm, daemon=True).start()
        elif status == "connected":
            on_connected()
        elif status == "rejected":
            connection_status.value = message
            connection_status.color = "#ef4444"
        else:
            connection_status.value = f"Ошибка: {message}"
            connection_status.color = "#ef4444"
        page.update()

    def on_connected():
        connection_status.value = "Подключено!"
        connection_status.color = "#22c55e"
        # Switch to control view
        main_content.selected_index = 1
        conn_indicator.content.controls[0].bgcolor = "#22c55e"
        conn_indicator.content.controls[1].value = "Подключено"
        conn_indicator.content.controls[1].color = "#22c55e"
        page.update()

    def do_disconnect():
        admin.disconnect()
        connection_status.value = "Отключено"
        connection_status.color = "#64748b"
        # Switch back to connection view
        main_content.selected_index = 0
        conn_indicator.content.controls[0].bgcolor = "#64748b"
        conn_indicator.content.controls[1].value = "Не подключено"
        conn_indicator.content.controls[1].color = "#64748b"
        page.update()

    # ─── Connection indicator ───
    conn_indicator = ft.Container(
        content=ft.Row([
            ft.Container(width=10, height=10, border_radius=5, bgcolor="#64748b"),
            ft.Text("Не подключено", size=13, color="#64748b"),
        ], spacing=8),
        padding=ft.padding.symmetric(horizontal=12, vertical=6),
        border_radius=20,
        border=ft.border.all(1, "#334155"),
    )

    # ─── Main navigation ───
    main_content = ft.Tabs(
        selected_index=0,
        animation_duration=300,
        tabs=[
            ft.Tab(
                text="Подключение",
                icon=ft.icons.CAST,
                content=ft.Container(connection_content, padding=20),
            ),
            ft.Tab(
                text="Управление",
                icon=ft.icons.GAMEPAD,
                content=ft.Container(control_content, padding=20),
            ),
        ],
        expand=1,
    )

    # ─── App Bar ───
    page.appbar = ft.AppBar(
        title=ft.Row(
            [
                ft.Icon(ft.icons.SHIELD_MOON, color="#7c3aed", size=28),
                ft.Text("ToolCheats Hunter", size=20, weight=ft.FontWeight.BOLD, color="#e2e8f0"),
            ],
            spacing=10,
        ),
        bgcolor="#0f0f1a",
        actions=[conn_indicator, ft.Container(width=10)],
    )

    page.add(main_content)

    # Attach mouse events
    mouse_pad.on_pointer_down = on_mouse_down
    mouse_pad.on_pointer_up = on_mouse_up
    mouse_pad.on_pan_update = on_mouse_move

    page.update()


if __name__ == "__main__":
    ft.app(target=main)
