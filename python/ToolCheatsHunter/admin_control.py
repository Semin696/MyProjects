import flet as ft
import socket, json, threading, time, os, base64
from datetime import datetime

I=ft.icons.Icons;B=ft.border

class AdminClient:
    def __init__(self):
        self.sock=None;self.connected=False;self.authenticated=False;self.running=False
        self._resp=None;self._ev=threading.Event();self.client_ip=None;self.client_port=9090

    def discover(self,code,timeout=3):
        try:
            s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
            s.setsockopt(socket.SOL_SOCKET,socket.SO_BROADCAST,1)
            s.settimeout(timeout);s.bind(("0.0.0.0",0))
            s.sendto(json.dumps({"type":"discover","code":code}).encode(),("255.255.255.255",9091))
            t0=time.time()
            while time.time()-t0<timeout:
                try:
                    d,a=s.recvfrom(1024);resp=json.loads(d.decode())
                    if resp.get("type")=="found"and resp.get("code")==code:
                        self.client_ip=resp["ip"];self.client_port=resp.get("port",9090)
                        return self.client_ip
                except socket.timeout:continue
            return None
        except:return None

    def connect(self,ip,port=9090,code=None):
        if code and not ip:
            found=self.discover(code)
            if not found:return "error","Клиент не найден в сети"
            ip=self.client_ip;port=self.client_port
        try:
            self.sock=socket.socket(socket.AF_INET,socket.SOCK_STREAM);self.sock.settimeout(5)
            self.sock.connect((ip,port))
            if code:
                self.sock.sendall(json.dumps({"type":"auth","code":code}).encode())
            else:
                self.sock.sendall(json.dumps({"type":"auth","code":""}).encode())
            resp=json.loads(self.sock.recv(1024).decode())
            if resp.get("status")=="waiting_confirmation":
                self.authenticated=True;self.connected=True;self.running=True
                threading.Thread(target=self._recv,daemon=True).start()
                return "waiting","Ожидание подтверждения..."
            elif resp.get("status")=="confirmed":
                self.authenticated=True;self.connected=True;self.running=True
                threading.Thread(target=self._recv,daemon=True).start()
                return "connected","Подключено!"
            elif resp.get("status")=="rejected":self.disconnect();return "rejected","Отклонено"
            else:self.disconnect();return "error",resp.get("message","Ошибка")
        except socket.timeout:return "error","Таймаут"
        except ConnectionRefusedError:return "error","Соединение отклонено"
        except Exception as e:return "error",str(e)

    def _recv(self):
        buf=b"";self.sock.settimeout(0.5)
        while self.running and self.connected:
            try:
                d=self.sock.recv(65536)
                if not d:break
                buf+=d
                try:resp=json.loads(buf.decode());buf=b"";self._resp=resp;self._ev.set()
                except:continue
            except socket.timeout:continue
            except:break

    def send_cmd(self,cmd):
        if not self.connected or not self.sock:return None
        self._ev.clear();self._resp=None
        try:
            self.sock.sendall(json.dumps(cmd).encode())
            if self._ev.wait(timeout=10):return self._resp
        except:self.connected=False
        return None

    def send(self,cmd):
        if not self.connected or not self.sock:return False
        try:self.sock.sendall(json.dumps(cmd).encode());return True
        except:self.connected=False;return False

    def mouse_move(self,dx,dy):self.send({"type":"mouse_move","dx":dx,"dy":dy})
    def mouse_click(self,b="left"):self.send({"type":"mouse_click","button":b})
    def mouse_double(self):self.send({"type":"mouse_double_click"})
    def mouse_scroll(self,n):self.send({"type":"mouse_scroll","clicks":n})
    def key_press(self,t):self.send({"type":"key_press","text":t})
    def key_hotkey(self,k):self.send({"type":"key_hotkey","keys":k})
    def list_files(self,path):return self.send_cmd({"type":"list_files","path":path})
    def get_file(self,path):return self.send_cmd({"type":"get_file","path":path})

    def disconnect(self):
        self.running=False;self.connected=False;self.authenticated=False
        try:
            if self.sock:self.sock.close()
        except:pass

def main(page:ft.Page):
    page.title="ToolCheats Hunter — Администратор"
    page.theme_mode=ft.ThemeMode.DARK;page.bgcolor="#1a1a2e";page.padding=0
    page.window_width=900;page.window_height=680;page.window_resizable=True

    admin=AdminClient()
    ANIM=ft.Animation(250,ft.AnimationCurve.EASE)

    # Dark AnyDesk-style color palette
    ACCENT="#7c3aed";ACCENT_HOVER="#9333ea";BG="#16213e";CARD_BG="#1e293b"
    TEXT_PRIMARY="#e2e8f0";TEXT_SECONDARY="#94a3b8";BORDER_COLOR="#2d2d4a"

    # ---------- AnyDesk-style UI ----------
    page.theme=ft.Theme(
        color_scheme=ft.ColorScheme(primary=ACCENT,secondary=ACCENT_HOVER,error="#ef4444"),
        font_family="Segoe UI",use_material3=True
    )

    # Status bar (top)
    status_dot=ft.Container(width=10,height=10,border_radius=5,bgcolor="#475569")
    status_label=ft.Text("Не подключено",size=13,color=TEXT_SECONDARY)
    status_row=ft.Container(
        content=ft.Row([ft.Icon(I.SHIELD_MOON,color=ACCENT,size=20),ft.Text("ToolCheats Hunter",size=16,weight=ft.FontWeight.BOLD,color=TEXT_PRIMARY),ft.Container(expand=True),status_dot,status_label],spacing=8),
        padding=ft.padding.Padding.symmetric(horizontal=20,vertical=12),
        bgcolor=BG,border=ft.border.Border(bottom=B.BorderSide(1,BORDER_COLOR))
    )

    # Session toolbar (hidden until connected)
    session_bar=ft.Row(spacing=6,visible=False)
    tool_btns=[]

    def make_tool_btn(text,icon,on_clk):
        b=ft.Container(
            content=ft.Column([ft.Icon(icon,color=TEXT_SECONDARY,size=22),ft.Text(text,size=11,color=TEXT_SECONDARY)],horizontal_alignment=ft.CrossAxisAlignment.CENTER,spacing=2),
            padding=ft.padding.Padding.symmetric(horizontal=16,vertical=8),border_radius=8,
            on_click=on_clk
        )
        b.on_hover=lambda e,b=b:setattr(b,"bgcolor","#1e293b"if e.data=="true"else"transparent")or b.update()
        return b

    toolbar=ft.Container(
        content=ft.Column([session_bar,ft.Divider(height=1,color=BORDER_COLOR,visible=False)],spacing=0),
        bgcolor=BG,visible=False
    )

    # Main content area
    code_input=ft.TextField(
        hint_text="Введите код клиента",width=320,
        text_align=ft.TextAlign.CENTER,text_size=22,
        border_color=BORDER_COLOR,focused_border_color=ACCENT,
        bgcolor=BG,color=TEXT_PRIMARY,
        border_radius=8,
        hint_style=ft.TextStyle(color="#475569",size=22),
        password=True,can_reveal_password=True,
        on_submit=lambda e:do_conn()
    )

    conn_btn=ft.Container(
        content=ft.Row([ft.Icon(I.CAST_CONNECTED,color="#e2e8f0",size=18),ft.Text("Подключиться",color="#e2e8f0",size=15,weight=ft.FontWeight.BOLD)],spacing=8,alignment=ft.MainAxisAlignment.CENTER),
        padding=ft.padding.Padding.symmetric(horizontal=32,vertical=14),
        border_radius=8,bgcolor=ACCENT,
        on_click=lambda e:do_conn()
    )
    conn_btn.on_hover=lambda e:setattr(conn_btn,"bgcolor",ACCENT_HOVER if e.data=="true"else ACCENT)or conn_btn.update()

    conn_msg=ft.Text("",size=13,color=TEXT_SECONDARY)

    connect_view=ft.Column([
        ft.Container(height=80),
        ft.Container(content=ft.Column([
            ft.Container(content=ft.Icon(I.CAST_CONNECTED,color=ACCENT,size=48),alignment=ft.alignment.Alignment.CENTER),
            ft.Container(height=8),
            ft.Text("Удалённое управление",size=28,weight=ft.FontWeight.BOLD,color=TEXT_PRIMARY,text_align=ft.TextAlign.CENTER),
            ft.Text("Введите код, показанный на клиенте",size=14,color=TEXT_SECONDARY,text_align=ft.TextAlign.CENTER),
            ft.Container(height=24),
            ft.Container(content=code_input,padding=ft.padding.Padding.symmetric(horizontal=40)),
            ft.Container(height=16),
            ft.Container(content=ft.Container(content=conn_btn,padding=ft.padding.Padding.symmetric(horizontal=40))),
            ft.Container(height=8),
            conn_msg,
        ],horizontal_alignment=ft.CrossAxisAlignment.CENTER,spacing=0)),
        ft.Container(expand=True),
    ],expand=True,horizontal_alignment=ft.CrossAxisAlignment.CENTER)

    # Connected view (hidden until connected)
    # Mouse area
    mouse_stat=ft.Text("",size=12,color=TEXT_SECONDARY)
    mouse_pad=ft.Container(
        content=ft.Column([ft.Icon(I.TOUCH_APP,color="#475569",size=40),ft.Text("Область мыши",size=13,color="#475569")],horizontal_alignment=ft.CrossAxisAlignment.CENTER,alignment=ft.MainAxisAlignment.CENTER),
        height=280,border_radius=12,
        border=ft.border.Border(left=B.BorderSide(2,BORDER_COLOR),right=B.BorderSide(2,BORDER_COLOR),top=B.BorderSide(2,BORDER_COLOR),bottom=B.BorderSide(2,BORDER_COLOR)),
        bgcolor=BG
    )
    md=[False,(0,0)]
    def on_down(e):
        md[0]=True;md[1]=(e.local_x,e.local_y)
        mouse_pad.border=ft.border.Border(left=B.BorderSide(2,ACCENT),right=B.BorderSide(2,ACCENT),top=B.BorderSide(2,ACCENT),bottom=B.BorderSide(2,ACCENT))
        mouse_pad.bgcolor="#1e293b";mouse_stat.value="Мышь активна";mouse_stat.color=ACCENT
        if admin.connected:admin.mouse_click("left")
        page.update()
    def on_up(e):
        md[0]=False
        mouse_pad.border=ft.border.Border(left=B.BorderSide(2,BORDER_COLOR),right=B.BorderSide(2,BORDER_COLOR),top=B.BorderSide(2,BORDER_COLOR),bottom=B.BorderSide(2,BORDER_COLOR))
        mouse_pad.bgcolor=BG;mouse_stat.value="";page.update()
    def on_move(e):
        if md[0]:
            dx=int(e.local_x-md[1][0]);dy=int(e.local_y-md[1][1])
            if admin.connected and(abs(dx)>2 or abs(dy)>2):admin.mouse_move(dx*2,dy*2);md[1]=(e.local_x,e.local_y);mouse_stat.value=f"x:{int(e.local_x)} y:{int(e.local_y)}";page.update()
    mouse_pad.on_pointer_down=on_down;mouse_pad.on_pointer_up=on_up;mouse_pad.on_pan_update=on_move

    mouse_btns=ft.Row([
        ft.Button("Левый",icon=I.MOUSE,style=ft.ButtonStyle(bgcolor=ACCENT,color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.mouse_click("left")if admin.connected else None),
        ft.Button("Правый",icon=I.MOUSE,style=ft.ButtonStyle(bgcolor=ACCENT,color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.mouse_click("right")if admin.connected else None),
        ft.Button("Двойной",icon=I.ADS_CLICK,style=ft.ButtonStyle(bgcolor=ACCENT,color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.mouse_double()if admin.connected else None),
        ft.Button("Вверх",icon=I.KEYBOARD_ARROW_UP,style=ft.ButtonStyle(bgcolor=ACCENT,color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.mouse_scroll(3)if admin.connected else None),
        ft.Button("Вниз",icon=I.KEYBOARD_ARROW_DOWN,style=ft.ButtonStyle(bgcolor=ACCENT,color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.mouse_scroll(-3)if admin.connected else None),
    ],wrap=True,spacing=6)

    keys_inp=ft.TextField(hint_text="Введите текст и нажмите Enter...",width=400,border_color=BORDER_COLOR,focused_border_color=ACCENT,bgcolor=BG,color=TEXT_PRIMARY,border_radius=8,on_submit=lambda e:send_k())
    def send_k():
        if admin.connected and keys_inp.value:admin.key_press(keys_inp.value);keys_inp.value="";page.update()

    hk_inp=ft.TextField(hint_text="ctrl+shift+esc",width=280,border_color=BORDER_COLOR,focused_border_color=ACCENT,bgcolor=BG,color=TEXT_PRIMARY,border_radius=8,on_submit=lambda e:send_hk())
    def send_hk():
        if admin.connected and hk_inp.value:admin.key_hotkey([k.strip().lower()for k in hk_inp.value.split("+")]);hk_inp.value="";page.update()

    hotkey_btns=ft.Row([
        ft.Button("Ctrl+Alt+Del",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["ctrl","alt","delete"])if admin.connected else None),
        ft.Button("Ctrl+Shift+Esc",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["ctrl","shift","esc"])if admin.connected else None),
        ft.Button("Win+R",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["win","r"])if admin.connected else None),
        ft.Button("Win+D",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["win","d"])if admin.connected else None),
        ft.Button("Alt+F4",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:admin.key_hotkey(["alt","f4"])if admin.connected else None),
    ],wrap=True,spacing=6)

    # File explorer
    file_cur=ft.Text("~",size=13,color=TEXT_SECONDARY)
    file_list=ft.Column(spacing=2,scroll=ft.ScrollMode.AUTO)
    file_stat=ft.Text("",size=13,color=TEXT_SECONDARY)
    file_view=ft.Column(spacing=8,scroll=ft.ScrollMode.AUTO,expand=True)

    def do_navigate(path):
        if not admin.connected:file_stat.value="Нет подключения";file_stat.color="#ef4444";page.update();return
        file_stat.value="Загрузка...";file_stat.color="#f59e0b";page.update()
        def t():
            resp=admin.list_files(path)
            page.run_thread(lambda:show_files(resp))
        threading.Thread(target=t,daemon=True).start()

    def show_files(resp):
        file_list.controls.clear()
        if not resp or resp.get("status")!="ok":
            file_stat.value=resp.get("message","Ошибка")if resp else "Нет ответа";file_stat.color="#ef4444";page.update();return
        file_cur.value=resp["path"];file_stat.value=f"{len(resp['items'])} эл.";file_stat.color=TEXT_SECONDARY
        parent=resp.get("parent")
        if parent and parent!=resp["path"]:
            up=ft.Container(content=ft.Row([ft.Icon(I.FOLDER_OPEN,color=ACCENT,size=16),ft.Text("..",size=13,color=ACCENT)],spacing=6),padding=6,border_radius=6,on_click=lambda e:do_navigate(parent))
            up.on_hover=lambda e:setattr(up,"bgcolor","#1e293b"if e.data=="true"else"transparent")or up.update()
            file_list.controls.append(up)
        for item in resp["items"]:
            name=item["name"];is_dir=item["dir"]
            icon=I.FOLDER if is_dir else I.DESCRIPTION
            clr="#f59e0b"if is_dir else("#7c3aed"if name.endswith((".py",".js",".txt",".json",".xml",".md"))else("#22c55e"if name.endswith((".exe",".dll",".bat",".ps1"))else TEXT_SECONDARY))
            size_str=f" {item['size']//1024}KB"if not is_dir else""
            row=ft.Container(content=ft.Row([ft.Icon(icon,color=clr,size=16),ft.Text(f"{name}{size_str}",size=13,color=TEXT_PRIMARY,expand=True)],spacing=6),padding=ft.padding.Padding.symmetric(vertical=3,horizontal=6),border_radius=6)
            if is_dir:
                fp=os.path.join(resp["path"],name)
                row.on_click=lambda e,fp=fp:do_navigate(fp)
                row.on_hover=lambda e,r=row:setattr(r,"bgcolor","#1e293b"if e.data=="true"else"transparent")or r.update()
            else:
                row.on_hover=lambda e,r=row:setattr(r,"bgcolor","#1e293b"if e.data=="true"else"transparent")or r.update()
            file_list.controls.append(row)
        page.update()

    def refresh_files():
        if admin.connected:do_navigate(file_cur.value)

    # Tabs for connected view
    def make_section_tab(text,icon,content):
        return ft.Container(
            content=ft.Column([ft.Icon(icon,color=ACCENT,size=28),ft.Text(text,size=12,color=TEXT_SECONDARY)],horizontal_alignment=ft.CrossAxisAlignment.CENTER,spacing=4),
            padding=ft.padding.Padding.symmetric(vertical=12),expand=1,border_radius=8,
            on_click=lambda e:switch_section(content)
        )

    mouse_section=ft.Column([
        ft.Text("Мышь",size=18,weight=ft.FontWeight.BOLD,color=TEXT_PRIMARY),
        ft.Container(height=4),mouse_pad,ft.Container(height=8),mouse_stat,ft.Container(height=4),mouse_btns,
    ],scroll=ft.ScrollMode.AUTO)

    kb_section=ft.Column([
        ft.Text("Клавиатура",size=18,weight=ft.FontWeight.BOLD,color=TEXT_PRIMARY),
        ft.Container(height=4),ft.Text("Ввод текста",size=13,color=TEXT_SECONDARY),ft.Container(height=4),keys_inp,
        ft.Container(height=16),
        ft.Text("Горячие клавиши",size=18,weight=ft.FontWeight.BOLD,color=TEXT_PRIMARY),
        ft.Container(height=4),hk_inp,ft.Container(height=8),hotkey_btns,
    ],scroll=ft.ScrollMode.AUTO)

    files_section=ft.Column([
        ft.Text("Файлы",size=18,weight=ft.FontWeight.BOLD,color=TEXT_PRIMARY),
        ft.Container(height=4),
        ft.Row([ft.Container(content=ft.Icon(I.FOLDER,color=ACCENT,size=16)),file_cur,ft.Container(expand=True),
            ft.Button("Обновить",icon=I.REFRESH,style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:refresh_files())],spacing=6),
        ft.Container(height=4),
        ft.Row([
            ft.Button("Рабочий стол",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:do_navigate(os.path.expanduser("~/Desktop"))),
            ft.Button("Загрузки",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:do_navigate(os.path.expanduser("~/Downloads"))),
            ft.Button("C:\\",style=ft.ButtonStyle(bgcolor="#1e293b",color=TEXT_PRIMARY,shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:do_navigate("C:\\")),
        ],wrap=True,spacing=6),
        file_stat,ft.Divider(height=1,color=BORDER_COLOR),
        file_list,
    ],scroll=ft.ScrollMode.AUTO,expand=True)

    section_content=ft.Container(content=mouse_section,padding=20,expand=True)
    sections=[mouse_section,kb_section,files_section]

    def switch_section(content):
        section_content.content=content;page.update()

    section_tabs=ft.Container(
        content=ft.Row([
            make_section_tab("Мышь",I.TOUCH_APP,mouse_section),
            make_section_tab("Клавиатура",I.KEYBOARD,kb_section),
            make_section_tab("Файлы",I.FOLDER,files_section),
        ],spacing=4),
        padding=ft.padding.Padding.symmetric(horizontal=20,vertical=8),
        bgcolor=BG,border=ft.border.Border(bottom=B.BorderSide(1,BORDER_COLOR))
    )

    disconnect_btn=ft.Button("Отключиться",icon=I.LINK_OFF,style=ft.ButtonStyle(bgcolor="#ef4444",color="#e2e8f0",shape=ft.RoundedRectangleBorder(8)),on_click=lambda e:do_disc())

    connected_view=ft.Column([
        section_tabs,
        ft.Container(content=section_content,expand=True),
        ft.Container(content=ft.Row([disconnect_btn],alignment=ft.MainAxisAlignment.CENTER),padding=ft.padding.Padding.symmetric(vertical=12),bgcolor=BG,border=ft.border.Border(top=B.BorderSide(1,BORDER_COLOR))),
    ],expand=True,spacing=0,visible=False)

    # Main layout
    main_stack=ft.Stack([connect_view,connected_view],expand=True)
    page.add(status_row,main_stack)

    # ---------- Logic ----------

    def do_conn():
        code=code_input.value.strip()
        if not code:conn_msg.value="Введите код";conn_msg.color="#ef4444";page.update();return
        conn_msg.value="Поиск клиента...";conn_msg.color="#f59e0b";page.update()
        def t():
            found_ip=admin.discover(code)
            if found_ip:
                status,msg=admin.connect(admin.client_ip,admin.client_port,code)
            else:
                status,msg="error","Клиент с таким кодом не найден"
            page.run_thread(lambda:conn_res(status,msg))
        threading.Thread(target=t,daemon=True).start()

    def conn_res(status,msg):
        if status=="waiting":
            conn_msg.value=msg;conn_msg.color="#f59e0b"
            status_dot.bgcolor="#f59e0b";status_label.value="Ожидание...";status_label.color="#f59e0b"
            def w():
                for _ in range(60):
                    if admin.connected and admin.authenticated:page.run_thread(on_conn);return
                    if not admin.connected:page.run_thread(lambda:conn_res("error","Отклонено"));return
                    time.sleep(0.5)
                page.run_thread(lambda:conn_res("error","Таймаут"))
            threading.Thread(target=w,daemon=True).start()
        elif status=="connected":on_conn()
        elif status=="rejected":conn_msg.value=msg;conn_msg.color="#ef4444"
        else:conn_msg.value=f"Ошибка: {msg}";conn_msg.color="#ef4444"
        page.update()

    def on_conn():
        conn_msg.value="Подключено!";conn_msg.color="#22c55e"
        status_dot.bgcolor="#22c55e";status_label.value="Подключено";status_label.color="#22c55e"
        connect_view.visible=False;connected_view.visible=True
        page.update()

    def do_disc():
        admin.disconnect()
        connect_view.visible=True;connected_view.visible=False
        status_dot.bgcolor="#475569";status_label.value="Не подключено";status_label.color=TEXT_SECONDARY
        page.update()

    page.update()

if __name__=="__main__":
    ft.run(main)
