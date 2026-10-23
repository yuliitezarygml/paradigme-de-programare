"""Простое окно чата. Сетевые потоки передают события в очередь Tkinter."""

import base64
import queue
import socket
import threading
import tkinter as tk
from datetime import datetime
from pathlib import Path
from tkinter import filedialog, messagebox, scrolledtext, ttk

from client import ChatClient
from protocol import DEFAULT_PORT


class ChatApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Клиент — Rețeaua locală")
        self.geometry("900x680")
        self.minsize(700, 540)
        self.client = None
        self.events = queue.Queue()
        self.messages = {}
        self.selected = None
        self.reply_to = None
        self.downloads = {}
        self.busy = False
        self.connected = False
        self.has_more = False
        self.name = tk.StringVar(value=socket.gethostname().split(".")[0][:32])
        self.host = tk.StringVar(value="127.0.0.1")
        self.port = tk.StringVar(value=str(DEFAULT_PORT))
        self.room = tk.StringVar(value="General")
        self.status = tk.StringVar(value="Введите IP сервера, имя и комнату, затем подключитесь.")
        self.selection_text = tk.StringVar(value="Нажмите на сообщение, чтобы выбрать его.")
        self.reply_text = tk.StringVar(value="Ответ: не выбран")
        self.connection_fields = []
        self.connected_name = ""
        self._build_window()
        self.protocol("WM_DELETE_WINDOW", self._close)
        self.after(100, self._read_events)

    def _build_window(self):
        self.columnconfigure(0, weight=1)
        self.rowconfigure(4, weight=1)
        connection = ttk.LabelFrame(self, text="Подключение", padding=10)
        connection.grid(row=0, column=0, sticky="ew", padx=12, pady=10)
        for column, (label, variable, width) in enumerate([
            ("Имя", self.name, 16), ("IP сервера", self.host, 18),
            ("Порт", self.port, 7), ("Комната", self.room, 16)
        ]):
            ttk.Label(connection, text=label).grid(row=0, column=column, sticky="w")
            entry = ttk.Entry(connection, textvariable=variable, width=width)
            entry.grid(row=1, column=column, padx=(0, 12), sticky="ew")
            self.connection_fields.append(entry)
            connection.columnconfigure(column, weight=1)
        controls = ttk.Frame(self, padding=(12, 0))
        controls.grid(row=1, column=0, sticky="ew")
        self.connect_button = ttk.Button(controls, text="Подключиться", command=self._connect)
        self.connect_button.pack(side="left", padx=8)
        self.disconnect_button = ttk.Button(controls, text="Отключиться", command=self._disconnect)
        self.disconnect_button.pack(side="left")
        self.history_button = ttk.Button(controls, text="Ранняя история", command=self._history)
        self.history_button.pack(side="right")
        ttk.Label(self, text="Сначала запустите панель сервера. На одном компьютере IP: 127.0.0.1.", wraplength=860).grid(
            row=2, column=0, sticky="w", padx=12, pady=(8, 2))
        self.members = ttk.Label(self, text="Участники: —")
        self.members.grid(row=3, column=0, sticky="w", padx=12, pady=(0, 6))
        self.chat = scrolledtext.ScrolledText(self, wrap="word", font=("Arial", 12),
                                            padx=12, pady=10, state="disabled")
        self.chat.grid(row=4, column=0, sticky="nsew", padx=12)
        self.chat.tag_configure("author", font=("Arial", 11, "bold"))
        self.chat.tag_configure("quote", foreground="#64748b")
        self.chat.tag_configure("attachment", foreground="#155e75")
        self.chat.tag_configure("chosen", background="#dbeafe")
        actions = ttk.Frame(self, padding=(12, 6))
        actions.grid(row=5, column=0, sticky="ew")
        ttk.Label(actions, textvariable=self.selection_text).pack(side="left")
        self.download_button = ttk.Button(actions, text="Сохранить файл", command=self._download)
        self.download_button.pack(side="right")
        self.reply_button = ttk.Button(actions, text="Ответить", command=self._reply)
        self.reply_button.pack(side="right", padx=8)
        composer = ttk.Frame(self, padding=(12, 0, 12, 8))
        composer.grid(row=6, column=0, sticky="ew")
        composer.columnconfigure(0, weight=1)
        ttk.Label(composer, textvariable=self.reply_text).grid(row=0, column=0, sticky="w")
        ttk.Button(composer, text="Убрать ответ", command=self._cancel_reply).grid(row=0, column=1)
        self.input = tk.Text(composer, height=3, wrap="word", font=("Arial", 12))
        self.input.grid(row=1, column=0, sticky="ew", pady=6)
        self.input.bind("<Return>", self._enter)
        self.input.bind("<Shift-Return>", lambda event: None)
        buttons = ttk.Frame(composer)
        buttons.grid(row=1, column=1, padx=(10, 0))
        self.send_button = ttk.Button(buttons, text="Отправить", command=self._send_text)
        self.send_button.pack(fill="x")
        self.file_button = ttk.Button(buttons, text="Отправить файл", command=self._upload)
        self.file_button.pack(fill="x", pady=(6, 0))
        ttk.Label(self, textvariable=self.status, wraplength=860).grid(
            row=7, column=0, sticky="w", padx=12, pady=(0, 10))
        self._update_buttons()

    def _port(self):
        try:
            port = int(self.port.get())
        except ValueError:
            raise ValueError("Порт должен быть числом от 1 до 65535.") from None
        if not 1 <= port <= 65535:
            raise ValueError("Порт должен быть от 1 до 65535.")
        return port

    def _connect(self):
        try:
            port = self._port()
            name, host, room = self.name.get().strip(), self.host.get().strip(), self.room.get().strip()
            if not name or not host or not room:
                raise ValueError("Заполните имя, IP и комнату.")
        except ValueError as error:
            messagebox.showerror("Подключение", str(error))
            return
        self._disconnect()
        client = ChatClient(lambda packet: self.events.put((client, packet)))
        self.client = client
        self.status.set("Подключение к серверу…")
        self._update_buttons()
        threading.Thread(target=client.connect, args=(host, port, name, room), daemon=True).start()

    def _disconnect(self):
        if self.client:
            self.client.close()
        self.client = None
        self.connected = False
        self.busy = False
        self.downloads.clear()
        self._cancel_reply()
        self.members.configure(text="Участники: —")
        self.status.set("Отключено. История остаётся на сервере.")
        self._update_buttons()

    def _work(self, action):
        client = self.client
        def run():
            try:
                action()
            except (OSError, ValueError, ConnectionError) as error:
                self.events.put((client, {"type": "error", "message": str(error)}))
        threading.Thread(target=run, daemon=True).start()

    def _send_text(self):
        text = self.input.get("1.0", "end-1c").strip()
        if not self.connected or self.busy or not text:
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
        path = filedialog.askopenfilename(title="Выберите файл до 10 МБ")
        if path:
            client, reply_to = self.client, self.reply_to
            self.busy = True
            self.status.set("Отправка файла…")
            self._update_buttons()
            self._work(lambda: client.send_file(path, reply_to))

    def _download(self):
        attachment = self.messages[self.selected]["file"]
        path = filedialog.asksaveasfilename(title="Сохранить полученный файл",
                                          initialfile=attachment["name"])
        if path:
            self.downloads[attachment["id"]] = Path(path)
            client = self.client
            self._work(lambda: client.send({"type": "download", "file_id": attachment["id"]}))

    def _reply(self):
        if self.selected is not None:
            message = self.messages[self.selected]
            self.reply_to = self.selected
            preview = message["text"] or message["file"]["name"]
            self.reply_text.set(f"Ответ {message['sender']}: {preview[:65].replace(chr(10), ' ')}")
            self.input.focus_set()

    def _cancel_reply(self):
        self.reply_to = None
        self.reply_text.set("Ответ: не выбран")

    def _history(self):
        if self.messages:
            client, before = self.client, min(self.messages)
            self.history_button.configure(state="disabled")
            self._work(lambda: client.send({"type": "history", "before": before}))

    def _select(self, message_id):
        self.selected = message_id
        self.chat.tag_remove("chosen", "1.0", "end")
        ranges = self.chat.tag_ranges(f"message_{message_id}")
        if ranges:
            self.chat.tag_add("chosen", *ranges)
        self.selection_text.set(f"Выбрано сообщение #{message_id}: {self.messages[message_id]['sender']}")
        self._update_buttons()

    def _render_messages(self, scroll_to_end=True, history=False):
        old_position = self.chat.yview()[0]
        self.chat.configure(state="normal")
        self.chat.delete("1.0", "end")
        for message_id, message in sorted(self.messages.items()):
            tag = f"message_{message_id}"
            time = datetime.fromisoformat(message["timestamp"]).astimezone().strftime("%d.%m %H:%M")
            self.chat.insert("end", f"{message['sender']}   {time}   #{message_id}\n", (tag, "author"))
            if message["reply"]:
                reply = message["reply"]
                preview = reply["text"] or reply["file_name"] or "Файл"
                self.chat.insert("end", f"  Ответ {reply['sender']}: {preview[:100]}\n", (tag, "quote"))
            if message["text"]:
                self.chat.insert("end", message["text"] + "\n", (tag,))
            if message["file"]:
                attachment = message["file"]
                self.chat.insert("end", f"Файл: {attachment['name']} ({attachment['size']} байт)\n",
                                 (tag, "attachment"))
            self.chat.insert("end", "\n")
            self.chat.tag_bind(tag, "<Button-1>", lambda event, number=message_id: self._select(number))
        self.chat.configure(state="disabled")
        if scroll_to_end:
            self.chat.see("end")
        else:
            self.chat.yview_moveto(0 if history else old_position)
        if self.selected in self.messages:
            self._select(self.selected)

    def _update_buttons(self):
        active = self.connected and not self.busy
        for button in (self.send_button, self.file_button):
            button.configure(state="normal" if active else "disabled")
        self.reply_button.configure(state="normal" if active and self.selected in self.messages else "disabled")
        attachment = self.messages.get(self.selected, {}).get("file")
        self.download_button.configure(state="normal" if active and attachment else "disabled")
        self.history_button.configure(state="normal" if self.connected and self.has_more else "disabled")
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
                self.messages = {m["id"]: m for m in packet["messages"]}
                self.selected = None
                self.selection_text.set("Нажмите на сообщение, чтобы выбрать его.")
                self.has_more = packet["has_more"]
                self.status.set(f"Вы в комнате {packet['room']} под именем {packet['name']}.")
                self._render_messages()
            elif kind == "message":
                message = packet["message"]
                near_bottom = self.chat.yview()[1] >= 0.98
                self.messages[message["id"]] = message
                if message["sender"] == self.connected_name and self.busy:
                    if message["text"] == self.input.get("1.0", "end-1c").strip():
                        self.input.delete("1.0", "end")
                    self.busy = False
                    self._cancel_reply()
                    self.status.set("Отправлено.")
                self._render_messages(near_bottom)
            elif kind == "history":
                self.messages.update({m["id"]: m for m in packet["messages"]})
                self.has_more = packet["has_more"]
                self._render_messages(False, history=True)
            elif kind == "members":
                self.members.configure(text="Участники: " + ", ".join(packet["names"]))
            elif kind == "file":
                path = self.downloads.pop(packet["file_id"], None)
                if path:
                    def save(destination=path, data=packet["data"], owner=client):
                        destination.write_bytes(base64.b64decode(data, validate=True))
                        self.events.put((owner, {"type": "saved", "path": str(destination)}))
                    self._work(save)
            elif kind == "saved":
                self.status.set(f"Файл сохранён: {packet['path']}")
            elif kind == "error":
                self.busy = False
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
