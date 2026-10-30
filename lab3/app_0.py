"""Клиент чата: подключение, список комнат и сообщения справа/слева."""

import base64
import logging
import queue
import socket
import threading
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox, ttk

from chat_view import MessageView
from client import ChatClient
from protocol import DEFAULT_PORT
from theme import (CHAT_BACKGROUND, CHAT_INPUT, CHAT_MUTED, CHAT_PANEL,
                   CHAT_TEXT, FONT, apply_chat_theme)


class ChatApp(tk.Tk):
    def __init__(self):
        super().__init__()
        apply_chat_theme(self)
        self.title("LAN Chat — клиент")
        self.geometry("1180x800")
        self.minsize(1020, 680)
        self.client = None
        self.events = queue.Queue()
        self.connected = False
        self.connected_name = ""
        self.current_room = None
        self.room_closed = False
        self.switching = False
        self.busy = False
        self.rooms = []
        self.messages = {}
        self.drafts = {}
        self.selected = None
        self.reply_to = None
        self.has_more = False
        self.downloads = {}
        self.connection_fields = []
        self.name = tk.StringVar(self, value=socket.gethostname().split(".")[0][:32])
        self.host = tk.StringVar(self, value="127.0.0.1")
        self.port = tk.StringVar(self, value=str(DEFAULT_PORT))
        self.status = tk.StringVar(self, value="Запустите сервер и подключитесь по его IP.")
        self.selection_text = tk.StringVar(self, value="Выберите сообщение для ответа")
        self.reply_text = tk.StringVar(self)
        self._build_window()
        self.protocol("WM_DELETE_WINDOW", self._close)
        self.after(100, self._read_events)

    def _build_window(self):
        self.rowconfigure(0, weight=1)
        self.columnconfigure(1, weight=1)
        sidebar = ttk.Frame(self, width=255, padding=18)
        sidebar.grid(row=0, column=0, sticky="nsew")
        sidebar.grid_propagate(False)
        sidebar.columnconfigure(0, weight=1)
        sidebar.rowconfigure(5, weight=1)
        ttk.Label(sidebar, text="LAN Chat", style="Title.TLabel").grid(row=0, column=0, sticky="w")
        ttk.Label(sidebar, text="Локальная сеть", style="Muted.TLabel").grid(
            row=1, column=0, sticky="w", pady=(3, 20))
        connection = ttk.Frame(sidebar)
        connection.grid(row=2, column=0, sticky="ew")
        connection.columnconfigure(0, weight=1)
        for row, (label, variable) in enumerate([
            ("Ваше имя", self.name), ("IP сервера", self.host), ("Порт", self.port)
        ]):
            ttk.Label(connection, text=label, style="Muted.TLabel").grid(row=row * 2, column=0, sticky="w")
            entry = ttk.Entry(connection, textvariable=variable)
            entry.grid(row=row * 2 + 1, column=0, sticky="ew", pady=(4, 10))
            self.connection_fields.append(entry)
        self.connect_button = ttk.Button(connection, text="Подключиться", style="Accent.TButton", command=self._connect)
        self.connect_button.grid(row=6, column=0, sticky="ew", pady=(2, 6))
        self.disconnect_button = ttk.Button(connection, text="Отключиться", command=self._disconnect)
        self.disconnect_button.grid(row=7, column=0, sticky="ew")
        self.badge = ttk.Label(sidebar, text="Не подключено", style="Offline.TLabel")
        self.badge.grid(row=3, column=0, sticky="w", pady=16)
        ttk.Label(sidebar, text="КОМНАТЫ", style="Muted.TLabel").grid(row=4, column=0, sticky="w", pady=(4, 10))
        self.room_list = tk.Listbox(
            sidebar, background=CHAT_PANEL, foreground=CHAT_TEXT, selectbackground="#29465f",
            selectforeground="white", activestyle="none", borderwidth=0,
            highlightthickness=0, exportselection=False, font=(FONT, 12))
        self.room_list.grid(row=5, column=0, sticky="nsew")
        self.room_list.bind("<<ListboxSelect>>", self._choose_room)
        ttk.Label(sidebar, text="Комнаты создаёт администратор\nв панели сервера.",
                  style="Muted.TLabel").grid(row=6, column=0, sticky="w", pady=(14, 0))

        main = ttk.Frame(self)
        main.grid(row=0, column=1, sticky="nsew", padx=(1, 0))
        main.columnconfigure(0, weight=1)
        main.rowconfigure(1, weight=1)
        header = ttk.Frame(main, padding=(20, 16))
        header.grid(row=0, column=0, sticky="ew")
        header.columnconfigure(0, weight=1)
        self.room_title = ttk.Label(header, text="Выберите комнату", style="Section.TLabel")
        self.room_title.grid(row=0, column=0, sticky="w")
        self.room_status = ttk.Label(header, text="", style="Muted.TLabel")
        self.room_status.grid(row=0, column=1, padx=10)
        self.history_button = ttk.Button(header, text="Ранняя история", command=self._history)
        self.history_button.grid(row=0, column=2, rowspan=2)
        self.members = ttk.Label(header, text="Участники: —", style="Muted.TLabel")
        self.members.grid(row=1, column=0, columnspan=2, sticky="w", pady=(6, 0))
        self.chat = MessageView(main, self._select)
        self.chat.grid(row=1, column=0, sticky="nsew")

        actions = ttk.Frame(main, padding=(16, 8))
        actions.grid(row=2, column=0, sticky="ew")
        actions.columnconfigure(0, weight=1)
        ttk.Label(actions, textvariable=self.selection_text, style="Muted.TLabel").grid(row=0, column=0, sticky="w")
        self.copy_button = ttk.Button(actions, text="Копировать", command=self._copy)
        self.copy_button.grid(row=0, column=1, padx=4)
        self.reply_button = ttk.Button(actions, text="Ответить", command=self._reply)
        self.reply_button.grid(row=0, column=2, padx=4)
        self.download_button = ttk.Button(actions, text="Скачать", command=self._download)
        self.download_button.grid(row=0, column=3, padx=4)
        composer = ttk.Frame(main, padding=(16, 8, 16, 10))
        composer.grid(row=3, column=0, sticky="ew")
        composer.columnconfigure(0, weight=1)
        self.reply_panel = ttk.Frame(composer)
        self.reply_panel.grid(row=0, column=0, columnspan=2, sticky="ew", pady=(0, 8))
        ttk.Label(self.reply_panel, textvariable=self.reply_text, style="Muted.TLabel").pack(side="left")
        ttk.Button(self.reply_panel, text="Отменить", command=self._cancel_reply).pack(side="right")
        self.reply_panel.grid_remove()
        self.input = tk.Text(composer, height=2, wrap="word", font=(FONT, 12),
                             background=CHAT_INPUT, foreground=CHAT_TEXT, insertbackground=CHAT_TEXT,
                             borderwidth=0, highlightthickness=1, highlightbackground="#2b3d50",
                             highlightcolor="#409bd0", padx=12, pady=10)
        self.input.grid(row=1, column=0, sticky="ew")
        self.input.bind("<Return>", self._enter)
        self.input.bind("<Shift-Return>", lambda event: None)
        self.send_button = ttk.Button(composer, text="Отправить", style="Accent.TButton", command=self._send_text)
        self.send_button.grid(row=1, column=1, padx=(10, 0), sticky="ns")
        self.file_button = ttk.Button(composer, text="Прикрепить файл", command=self._upload)
        self.file_button.grid(row=2, column=0, sticky="w", pady=(8, 0))
        ttk.Label(composer, text="Enter — отправить · Shift+Enter — новая строка", style="Muted.TLabel").grid(
            row=2, column=0, columnspan=2, sticky="e", pady=(8, 0))
        ttk.Label(main, textvariable=self.status, style="Muted.TLabel", wraplength=800).grid(
            row=4, column=0, sticky="w", padx=18, pady=(0, 12))
        self._render_messages()
        self._update_buttons()

    def _connect(self):
        try:
            port = int(self.port.get())
            if not 1 <= port <= 65535:
                raise ValueError("Порт должен быть от 1 до 65535.")
            name, host = self.name.get().strip(), self.host.get().strip()
            if not name or not host:
                raise ValueError("Введите имя и IP сервера.")
        except ValueError as error:
            messagebox.showerror("Подключение", str(error))
            return
        self._disconnect()
        client = ChatClient(lambda packet: self.events.put((client, packet)))
        self.client = client
        self.status.set("Подключение к серверу…")
        self._update_buttons()
        threading.Thread(target=client.connect, args=(host, port, name, ""), daemon=True).start()

    def _disconnect(self):
        if self.client:
            self.client.close()
        self.client = None
        self.connected = False
        self.current_room = None
        self.busy = self.switching = False
        self.downloads.clear()
        self.rooms = []
        self.room_list.delete(0, "end")
        self._cancel_reply()
        self.members.configure(text="Участники: —")
        self.status.set("Отключено. Переписка остаётся на сервере.")
        self._update_buttons()

    def _work(self, action):
        client = self.client
        def run():
            try:
                action()
            except (OSError, ValueError, ConnectionError) as error:
                self.events.put((client, {"type": "error", "message": str(error)}))
        threading.Thread(target=run, daemon=True).start()

    def _set_rooms(self, rooms):
        self.rooms = rooms
        self.room_list.delete(0, "end")
        for index, room in enumerate(rooms):
            suffix = " · закрыта" if room["closed"] else f" · {room.get('members', 0)} чел."
            self.room_list.insert("end", "# " + room["name"] + suffix)
            if room["closed"]:
                self.room_list.itemconfigure(index, foreground=CHAT_MUTED)
            if room["name"] == self.current_room:
                self.room_list.selection_set(index)

    def _choose_room(self, event=None):
        selection = self.room_list.curselection()
        if not selection or not self.connected or self.busy or self.switching:
            return
        room = self.rooms[selection[0]]["name"]
        if room == self.current_room:
            return
        if self.current_room:
            self.drafts[self.current_room] = self.input.get("1.0", "end-1c")
        self.switching = True
        self._update_buttons()
        client = self.client
        self._work(lambda: client.send({"type": "switch_room", "room": room}))

    def _enter_room(self, packet):
        self.current_room = packet["room"]
        self.room_closed = packet.get("closed", False)
        self.messages = {message["id"]: message for message in packet["messages"]}
        self.has_more = packet["has_more"]
        self.selected = None
        self.busy = self.switching = False
        self.selection_text.set("Выберите сообщение для ответа")
        self._cancel_reply()
        self.input.configure(state="normal")
        self.input.delete("1.0", "end")
        self.input.insert("1.0", self.drafts.get(self.current_room, ""))
        self.room_title.configure(text="# " + self.current_room if self.current_room else "Выберите комнату слева")
        self._set_rooms(self.rooms)
        self._render_messages()
        self.status.set("Вы в комнате " + self.current_room if self.current_room else "Администратор может создать новую комнату.")

    def _send_text(self):
        text = self.input.get("1.0", "end-1c").strip()
        if not self._can_send() or not text:
            return
        if len(text) > 4000:
            messagebox.showerror("Сообщение", "Не больше 4000 символов.")
            return
        client, reply_to = self.client, self.reply_to
        self.busy = True
        self._update_buttons()
        self._work(lambda: client.send_text(text, reply_to))

    def _enter(self, event):
        self._send_text()
        return "break"

    def _upload(self):
        if not self._can_send():
            return
        path = filedialog.askopenfilename(title="Выберите файл до 10 МБ")
        if path:
            client, reply_to = self.client, self.reply_to
            self.busy = True
            self.status.set("Отправка файла…")
            self._update_buttons()
            self._work(lambda: client.send_file(path, reply_to))

    def _download(self):
        attachment = self.messages[self.selected]["file"]
        path = filedialog.asksaveasfilename(title="Сохранить файл", initialfile=attachment["name"])
        if path:
            self.downloads[attachment["id"]] = Path(path)
            client = self.client
            self._work(lambda: client.send({"type": "download", "file_id": attachment["id"]}))

    def _copy(self):
        if self.selected in self.messages:
            message = self.messages[self.selected]
            self.clipboard_clear()
            self.clipboard_append(message["text"] or message["file"]["name"])
            self.status.set("Скопировано.")

    def _reply(self):
        if self.selected in self.messages and self._can_send():
            message = self.messages[self.selected]
            self.reply_to = self.selected
            preview = message["text"] or message["file"]["name"]
            self.reply_text.set(f"Ответ {message['sender']}: {preview[:65].replace(chr(10), ' ')}")
            self.reply_panel.grid()
            self.input.focus_set()

    def _cancel_reply(self):
        self.reply_to = None
        self.reply_text.set("")
        self.reply_panel.grid_remove()

    def _history(self):
        if self.messages:
            client, before = self.client, min(self.messages)
            self.history_button.configure(state="disabled")
            self._work(lambda: client.send({"type": "history", "before": before}))

    def _select(self, message_id):
        self.selected = message_id
        self.chat.highlight(message_id)
        self.selection_text.set("Сообщение от " + self.messages[message_id]["sender"])
        self._update_buttons()

    def _render_messages(self, scroll_to_end=True, history=False):
        empty = "Пока здесь тихо. Напишите первое сообщение." if self.current_room else "Выберите комнату слева. Комнаты создаются в панели сервера."
        self.chat.render(self.messages, self.connected_name, scroll_to_end, history, empty)
        self.chat.highlight(self.selected)

    def _can_send(self):
        return self.connected and self.current_room is not None and not self.room_closed and not self.busy and not self.switching

    def _update_buttons(self):
        active = self._can_send()
        self.input.configure(state="disabled" if self.switching else "normal")
        self.badge.configure(text="Подключено" if self.connected else "Не подключено",
                             style="Online.TLabel" if self.connected else "Offline.TLabel")
        self.room_status.configure(text="Только чтение" if self.room_closed and self.current_room else "")
        for button in (self.send_button, self.file_button):
            button.configure(state="normal" if active else "disabled")
        selected = self.selected in self.messages
        self.copy_button.configure(state="normal" if selected else "disabled")
        self.reply_button.configure(state="normal" if active and selected else "disabled")
        attachment = self.messages.get(self.selected, {}).get("file")
        self.download_button.configure(state="normal" if self.connected and not self.switching and attachment else "disabled")
        self.history_button.configure(state="normal" if self.connected and self.has_more and not self.switching else "disabled")
        self.connect_button.configure(state="disabled" if self.client else "normal")
        self.disconnect_button.configure(state="normal" if self.client else "disabled")
        for field in self.connection_fields:
            field.configure(state="disabled" if self.client else "normal")

    def _read_events(self):
        for _ in range(50):
            try:
                client, packet = self.events.get_nowait()
            except queue.Empty:
                break
            if client is not self.client:
                continue
            kind = packet["type"]
            if kind == "welcome":
                self.connected = True
                self.connected_name = packet["name"]
                self.rooms = packet["rooms"]
                self._enter_room(packet)
            elif kind == "room":
                self._enter_room(packet)
            elif kind == "rooms":
                self._set_rooms(packet["rooms"])
            elif kind == "room_status" and packet["room"] == self.current_room:
                self.room_closed = packet["closed"]
                if self.room_closed:
                    self._cancel_reply()
                self.status.set("Комната закрыта: доступно только чтение." if self.room_closed else "Комната снова открыта.")
            elif kind == "history_cleared" and packet["room"] == self.current_room:
                self.messages.clear()
                self.selected = None
                self.selection_text.set("Выберите сообщение для ответа")
                self.has_more = False
                self._cancel_reply()
                self._render_messages()
                self.status.set("Администратор очистил историю этой комнаты.")
            elif kind == "room_deleted":
                self.drafts.pop(packet["room"], None)
                self.status.set("Комната удалена администратором: " + packet["room"])
            elif kind == "message" and packet["message"]["room"] == self.current_room:
                message = packet["message"]
                near_bottom = self.chat.near_bottom()
                self.messages[message["id"]] = message
                if message["sender"] == self.connected_name and self.busy:
                    if message["text"] == self.input.get("1.0", "end-1c").strip():
                        self.input.delete("1.0", "end")
                        self.drafts.pop(self.current_room, None)
                    self.busy = False
                    self._cancel_reply()
                    self.status.set("Отправлено.")
                self._render_messages(near_bottom)
            elif kind == "history" and packet.get("room") == self.current_room:
                self.messages.update({message["id"]: message for message in packet["messages"]})
                self.has_more = packet["has_more"]
                self._render_messages(False, history=True)
            elif kind == "members" and packet.get("room") == self.current_room:
                self.members.configure(text="Участники: " + (", ".join(packet["names"]) or "—"))
            elif kind == "file":
                path = self.downloads.pop(packet["file_id"], None)
                if path:
                    def save(destination=path, data=packet["data"], owner=client):
                        destination.write_bytes(base64.b64decode(data, validate=True))
                        self.events.put((owner, {"type": "saved", "path": str(destination)}))
                    self._work(save)
            elif kind == "saved":
                self.status.set("Файл сохранён: " + packet["path"])
            elif kind == "error":
                self.busy = self.switching = False
                self.status.set(packet["message"])
                messagebox.showerror("Ошибка", packet["message"])
            elif kind == "disconnected":
                self._disconnect()
                self.status.set("Соединение потеряно: " + packet["message"])
            self._update_buttons()
        self.after(100, self._read_events)

    def _close(self):
        self._disconnect()
        self.destroy()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s: %(message)s", datefmt="%H:%M:%S")
    logging.info("Открыт клиент. Подключитесь к серверу.")
    ChatApp().mainloop()
