"""Отдельная панель сервера. Логи видны здесь и в консоли, но не пишутся в файл."""

import logging
import queue
import socket
import tkinter as tk
from tkinter import messagebox, scrolledtext, ttk

from server import ChatServer
from protocol import DEFAULT_PORT
from theme import (CHAT_MUTED as MUTED, CHAT_PANEL as SURFACE,
                   CHAT_TEXT as TEXT, FONT, apply_chat_theme)


class WindowLogHandler(logging.Handler):
    """Сетевые потоки кладут строки в очередь, окно читает их через after()."""

    def __init__(self, events):
        super().__init__()
        self.events = events
        self.setFormatter(logging.Formatter("%(asctime)s %(levelname)s: %(message)s", "%H:%M:%S"))

    def emit(self, record):
        self.events.put(self.format(record))


class ServerApp(tk.Tk):
    def __init__(self, host="0.0.0.0", port=DEFAULT_PORT, data_dir=None):
        super().__init__()
        apply_chat_theme(self)
        self.title("Панель сервера — Rețeaua locală")
        self.geometry("1160x740")
        self.minsize(980, 620)
        self.server = None
        self.data_dir = data_dir
        self.events = queue.Queue()
        self.host = tk.StringVar(value=host)
        self.port = tk.StringVar(value=str(port))
        self.status = tk.StringVar(value="Сервер остановлен.")
        self.people = tk.StringVar(value="Участники: —")
        self.room_name = tk.StringVar()
        self.room_hint = tk.StringVar(value="Запустите сервер для управления комнатами.")
        self.room_details = {}
        self.rooms_snapshot = None
        self.columnconfigure(0, weight=1)
        self.rowconfigure(3, weight=1)
        header = ttk.Frame(self, padding=(22, 20, 22, 14))
        header.grid(row=0, column=0, sticky="ew")
        ttk.Label(header, text="Панель сервера", style="Title.TLabel").pack(side="left")
        self.badge = ttk.Label(header, text="Остановлен", style="Offline.TLabel")
        self.badge.pack(side="right")
        controls = ttk.Frame(self, style="Card.TFrame", padding=16)
        controls.grid(row=1, column=0, sticky="ew", padx=20)
        ttk.Label(controls, text="Адрес сервера", style="CardMuted.TLabel").grid(row=0, column=0, sticky="w")
        self.host_entry = ttk.Entry(controls, textvariable=self.host, width=20)
        self.host_entry.grid(row=1, column=0, padx=(0, 14), pady=(6, 0))
        ttk.Label(controls, text="Порт", style="CardMuted.TLabel").grid(row=0, column=1, sticky="w")
        self.port_entry = ttk.Entry(controls, textvariable=self.port, width=8)
        self.port_entry.grid(row=1, column=1, padx=(0, 18), pady=(6, 0))
        self.start_button = ttk.Button(controls, text="Запустить сервер", style="Accent.TButton", command=self.start_server)
        self.start_button.grid(row=1, column=2, padx=(0, 8), pady=(6, 0))
        self.stop_button = ttk.Button(controls, text="Остановить", command=self.stop_server, state="disabled")
        self.stop_button.grid(row=1, column=3, pady=(6, 0))
        info = ttk.Frame(self, padding=(22, 12))
        info.grid(row=2, column=0, sticky="ew")
        ttk.Label(info, textvariable=self.status, style="Muted.TLabel", wraplength=840).pack(anchor="w")
        ttk.Label(info, textvariable=self.people, style="Muted.TLabel", wraplength=840).pack(anchor="w", pady=(6, 0))
        content = ttk.Frame(self)
        content.grid(row=3, column=0, sticky="nsew", padx=20)
        content.columnconfigure(0, weight=1, minsize=470)
        content.columnconfigure(1, weight=1, minsize=390)
        content.rowconfigure(0, weight=1)
        rooms = ttk.Frame(content, style="Card.TFrame", padding=16)
        rooms.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        self.build_rooms_panel(rooms)
        events = ttk.Frame(content, style="Card.TFrame", padding=16)
        events.grid(row=0, column=1, sticky="nsew")
        events.columnconfigure(0, weight=1)
        events.rowconfigure(2, weight=1)
        ttk.Label(events, text="События сервера", style="Section.TLabel").grid(
            row=0, column=0, sticky="w")
        ttk.Label(events, text="Подключения, сообщения и действия с комнатами",
                  style="CardMuted.TLabel").grid(row=1, column=0, sticky="w", pady=(5, 12))
        self.logs = scrolledtext.ScrolledText(
            events, font=("Menlo", 10), wrap="word", state="disabled", padx=10, pady=10,
            background=SURFACE, foreground=TEXT, borderwidth=0,
            highlightthickness=1, highlightbackground="#2b3d50")
        self.logs.grid(row=2, column=0, sticky="nsew")
        ttk.Label(self, text="Логи видны здесь и в терминале · Файл логов не создаётся",
                  style="Muted.TLabel").grid(row=4, column=0, sticky="w", padx=22, pady=14)
        self.log_handler = WindowLogHandler(self.events)
        logging.getLogger("server").addHandler(self.log_handler)
        self.protocol("WM_DELETE_WINDOW", self.close)
        self.after(100, self.refresh)

    def build_rooms_panel(self, panel):
        panel.columnconfigure(0, weight=1)
        panel.rowconfigure(3, weight=1)
        ttk.Label(panel, text="Комнаты", style="Section.TLabel").grid(row=0, column=0, sticky="w")
        ttk.Label(panel, text="У каждой комнаты своя история сообщений.",
                  style="CardMuted.TLabel").grid(row=1, column=0, sticky="w", pady=(5, 14))
        create = ttk.Frame(panel, style="Card.TFrame")
        create.grid(row=2, column=0, sticky="ew", pady=(0, 14))
        create.columnconfigure(0, weight=1)
        ttk.Label(create, text="Название новой комнаты", style="CardMuted.TLabel").grid(
            row=0, column=0, sticky="w", pady=(0, 6))
        self.room_entry = ttk.Entry(create, textvariable=self.room_name, state="disabled")
        self.room_entry.grid(row=1, column=0, sticky="ew", padx=(0, 8))
        self.room_entry.bind("<Return>", lambda event: self.create_room())
        self.create_room_button = ttk.Button(
            create, text="Создать комнату", command=self.create_room, state="disabled")
        self.create_room_button.grid(row=1, column=1)
        table = ttk.Frame(panel, style="Card.TFrame")
        table.grid(row=3, column=0, sticky="nsew")
        table.columnconfigure(0, weight=1)
        table.rowconfigure(0, weight=1)
        style = ttk.Style(self)
        style.configure("Rooms.Treeview", background=SURFACE, fieldbackground=SURFACE,
                        foreground=TEXT, rowheight=35, borderwidth=0)
        style.configure("Rooms.Treeview.Heading", font=(FONT, 10, "bold"), padding=(6, 9))
        style.configure("Rooms.Treeview.Heading", background="#223448", foreground=TEXT)
        style.map("Rooms.Treeview", background=[("selected", "#29465f")],
                  foreground=[("selected", "white")])
        self.room_list = ttk.Treeview(
            table, columns=("name", "status", "messages", "members"),
            show="headings", selectmode="browse", style="Rooms.Treeview", height=7)
        for column, title, width, anchor in (
            ("name", "Комната", 140, "w"), ("status", "Статус", 90, "w"),
            ("messages", "Сообщения", 85, "center"), ("members", "Люди", 55, "center"),
        ):
            self.room_list.heading(column, text=title)
            self.room_list.column(column, width=width, minwidth=width, anchor=anchor,
                                  stretch=column == "name")
        self.room_list.tag_configure("closed", foreground=MUTED)
        self.room_list.grid(row=0, column=0, sticky="nsew")
        scrollbar = ttk.Scrollbar(table, orient="vertical", command=self.room_list.yview)
        scrollbar.grid(row=0, column=1, sticky="ns")
        self.room_list.configure(yscrollcommand=scrollbar.set)
        self.room_list.bind("<<TreeviewSelect>>", self.update_room_buttons)
        ttk.Label(panel, textvariable=self.room_hint, style="CardMuted.TLabel", wraplength=410).grid(
            row=4, column=0, sticky="w", pady=(12, 10))
        actions = ttk.Frame(panel, style="Card.TFrame")
        actions.grid(row=5, column=0, sticky="ew")
        self.toggle_room_button = ttk.Button(
            actions, text="Закрыть комнату", command=self.toggle_room, state="disabled")
        self.toggle_room_button.grid(row=0, column=0, sticky="w", padx=(0, 8))
        self.clear_room_button = ttk.Button(
            actions, text="Очистить историю", command=self.clear_room, state="disabled")
        self.clear_room_button.grid(row=0, column=1, sticky="w")
        self.delete_room_button = ttk.Button(
            actions, text="Удалить комнату", command=self.delete_room, state="disabled")
        self.delete_room_button.grid(row=1, column=0, sticky="w", pady=(8, 0))

    def selected_room(self):
        selected = self.room_list.selection()
        return selected[0] if selected else None

    def update_room_buttons(self, event=None):
        room = self.room_details.get(self.selected_room())
        running = self.server is not None
        self.room_entry.configure(state="normal" if running else "disabled")
        self.create_room_button.configure(state="normal" if running else "disabled")
        for button in (self.toggle_room_button, self.clear_room_button, self.delete_room_button):
            button.configure(state="normal" if running and room else "disabled")
        self.toggle_room_button.configure(text="Открыть комнату" if room and room["closed"] else "Закрыть комнату")
        if not running:
            self.room_hint.set("Запустите сервер для управления комнатами.")
        elif not room:
            self.room_hint.set("Создайте комнату или выберите её в списке.")
        elif room["closed"]:
            self.room_hint.set("Комната закрыта: историю можно читать. Откройте её, чтобы разрешить сообщения.")
        else:
            self.room_hint.set("Комната открыта: участники могут отправлять сообщения.")

    def refresh_rooms(self, select=None):
        rooms = self.server.rooms() if self.server else []
        snapshot = [(room["name"], room["closed"], room["message_count"], room["members"])
                    for room in rooms]
        # Если данные не изменились, список не перерисовывается и выбор остаётся на месте.
        if snapshot != self.rooms_snapshot:
            selected = select or self.selected_room()
            self.rooms_snapshot = snapshot
            self.room_details = {room["name"]: room for room in rooms}
            names = set(self.room_details)
            for name in self.room_list.get_children():
                if name not in names:
                    self.room_list.delete(name)
            for index, room in enumerate(rooms):
                name = room["name"]
                values = (name, "Закрыта" if room["closed"] else "Открыта",
                          room["message_count"], room["members"])
                tags = ("closed",) if room["closed"] else ()
                if self.room_list.exists(name):
                    self.room_list.item(name, values=values, tags=tags)
                    self.room_list.move(name, "", index)
                else:
                    self.room_list.insert("", index, iid=name, values=values, tags=tags)
            if rooms:
                selected = selected if selected in names else rooms[0]["name"]
                self.room_list.selection_set(selected)
                self.room_list.focus(selected)
            self.update_room_buttons()
        elif select and self.room_list.exists(select):
            self.room_list.selection_set(select)
            self.room_list.focus(select)
            self.update_room_buttons()

    def create_room(self):
        if not self.server:
            return
        name = self.room_name.get().strip()
        try:
            self.server.create_room(name)
        except (OSError, ValueError, KeyError) as error:
            messagebox.showerror("Комнаты", str(error), parent=self)
            return
        self.room_name.set("")
        self.refresh_rooms(select=name)

    def toggle_room(self):
        name = self.selected_room()
        if not self.server or name not in self.room_details:
            return
        try:
            self.server.set_room_closed(name, not self.room_details[name]["closed"])
        except (OSError, ValueError, KeyError) as error:
            messagebox.showerror("Комнаты", str(error), parent=self)
            return
        self.refresh_rooms()

    def clear_room(self):
        name = self.selected_room()
        if not self.server or name not in self.room_details:
            return
        if not messagebox.askyesno("Очистить историю", f"Удалить все сообщения комнаты «{name}»?", parent=self):
            return
        try:
            self.server.clear_room_history(name)
        except (OSError, ValueError, KeyError) as error:
            messagebox.showerror("Комнаты", str(error), parent=self)
            return
        self.refresh_rooms()

    def delete_room(self):
        name = self.selected_room()
        if not self.server or name not in self.room_details:
            return
        if not messagebox.askyesno("Удалить комнату", f"Удалить комнату «{name}» и всю её историю?", parent=self):
            return
        try:
            self.server.delete_room(name)
        except (OSError, ValueError, KeyError) as error:
            messagebox.showerror("Комнаты", str(error), parent=self)
            return
        self.refresh_rooms()

    def start_server(self):
        try:
            port = int(self.port.get())
            if not 1 <= port <= 65535:
                raise ValueError("Порт должен быть от 1 до 65535.")
            server = ChatServer(self.host.get().strip(), port, self.data_dir)
            server.start()
            self.server = server
        except (OSError, ValueError, KeyError, TypeError) as error:
            logging.getLogger("server").error("Не удалось запустить сервер: %s", error)
            messagebox.showerror("Сервер", str(error))
            return
        try:
            addresses = socket.gethostbyname_ex(socket.gethostname())[2]
            addresses = [address for address in addresses if not address.startswith("127.")]
            address = ", ".join(addresses) or socket.gethostname()
        except OSError:
            address = socket.gethostname()
        self.status.set(f"Сервер работает. IP для клиентов: {address}, порт {port}. На этом ПК: 127.0.0.1.")
        for widget in (self.host_entry, self.port_entry, self.start_button):
            widget.configure(state="disabled")
        self.stop_button.configure(state="normal")
        self.badge.configure(text="Работает", style="Online.TLabel")
        self.refresh_rooms()
        self.update_room_buttons()

    def stop_server(self):
        if self.server:
            self.server.stop()
            self.server = None
        self.status.set("Сервер остановлен. Комнаты и история сохранены.")
        self.people.set("Участники: —")
        for widget in (self.host_entry, self.port_entry, self.start_button):
            widget.configure(state="normal")
        self.stop_button.configure(state="disabled")
        self.badge.configure(text="Остановлен", style="Offline.TLabel")
        self.refresh_rooms()
        self.update_room_buttons()

    def refresh(self):
        for _ in range(100):
            try:
                line = self.events.get_nowait()
            except queue.Empty:
                break
            self.logs.configure(state="normal")
            self.logs.insert("end", line + "\n")
            self.logs.see("end")
            self.logs.configure(state="disabled")
        if self.server:
            self.people.set("Участники: " + (", ".join(self.server.members()) or "—"))
            self.refresh_rooms()
        self.after(100, self.refresh)

    def close(self):
        self.stop_server()
        logging.getLogger("server").removeHandler(self.log_handler)
        self.destroy()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s: %(message)s", datefmt="%H:%M:%S")
    logging.info("Открыта панель сервера. Нажмите «Запустить сервер».")
    ServerApp().mainloop()
