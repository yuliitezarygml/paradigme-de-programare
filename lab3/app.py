"""Простое окно чата. Сетевые потоки передают события в очередь Tkinter."""

import base64
import logging
import queue
import socket
import threading
import tkinter as tk
from datetime import datetime
from pathlib import Path
from tkinter import filedialog, messagebox, scrolledtext, ttk

from client import ChatClient
from protocol import DEFAULT_PORT
from theme import BLUE, FONT, MUTED, OWN_MESSAGE, SURFACE, TEXT, apply_theme


class ChatApp(tk.Tk):
    def __init__(self):
        super().__init__()
        apply_theme(self)
        self.title("Клиент — Rețeaua locală")
        self.geometry("980x800")
        self.minsize(820, 680)
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
        self.rowconfigure(2, weight=1)
        header = ttk.Frame(self, padding=(22, 20, 22, 14))
        header.grid(row=0, column=0, sticky="ew")
        ttk.Label(header, text="Локальный чат", style="Title.TLabel").pack(side="left")
        self.badge = ttk.Label(header, text="Не подключено", style="Offline.TLabel")
        self.badge.pack(side="right")

        connection = ttk.Frame(self, style="Card.TFrame", padding=16)
        connection.grid(row=1, column=0, sticky="ew", padx=20, pady=(0, 14))
        for column, (label, variable, width) in enumerate([
            ("Ваше имя", self.name, 18), ("IP сервера", self.host, 18),
            ("Порт", self.port, 7), ("Комната", self.room, 16)
        ]):
            ttk.Label(connection, text=label, style="CardMuted.TLabel").grid(
                row=0, column=column, sticky="w", pady=(0, 5))
            entry = ttk.Entry(connection, textvariable=variable, width=width)
            entry.grid(row=1, column=column, padx=(0, 10 if column < 3 else 0), sticky="ew")
            self.connection_fields.append(entry)
            connection.columnconfigure(column, weight=1)
        controls = ttk.Frame(connection, style="Card.TFrame")
        controls.grid(row=2, column=0, columnspan=4, sticky="ew", pady=(12, 0))
        self.connect_button = ttk.Button(controls, text="Подключиться", style="Accent.TButton",
                                         command=self._connect)
        self.connect_button.pack(side="left")
        self.disconnect_button = ttk.Button(controls, text="Отключиться", command=self._disconnect)
        self.disconnect_button.pack(side="left", padx=8)
        ttk.Label(controls, text="На этом компьютере: 127.0.0.1", style="CardMuted.TLabel").pack(side="right")

        conversation = ttk.Frame(self, style="Card.TFrame", padding=14)
        conversation.grid(row=2, column=0, sticky="nsew", padx=20)
        conversation.columnconfigure(0, weight=1)
        conversation.rowconfigure(1, weight=1)
        chat_header = ttk.Frame(conversation, style="Card.TFrame")
        chat_header.grid(row=0, column=0, sticky="ew", pady=(0, 10))
        self.room_title = ttk.Label(chat_header, text="Переписка", style="Section.TLabel")
        self.room_title.pack(side="left")
        self.history_button = ttk.Button(chat_header, text="Ранняя история", command=self._history)
        self.history_button.pack(side="right")
        self.members = ttk.Label(chat_header, text="Участники: —", style="CardMuted.TLabel")
        self.members.pack(side="left", padx=16)
        self.chat = scrolledtext.ScrolledText(
            conversation, wrap="word", font=(FONT, 12), padx=12, pady=12,
            background=SURFACE, foreground=TEXT, selectbackground="#bfdbfe",
            borderwidth=0, highlightthickness=0, state="disabled")
        self.chat.grid(row=1, column=0, sticky="nsew")
        self.chat.tag_configure("author", font=(FONT, 10, "bold"), foreground=MUTED, spacing1=12, spacing3=5)
        self.chat.tag_configure("quote", foreground=MUTED, font=(FONT, 10), spacing3=5)
        self.chat.tag_configure("attachment", foreground=BLUE, font=(FONT, 11, "bold"))
        self.chat.tag_configure("own", background=OWN_MESSAGE, lmargin1=100, lmargin2=100, rmargin=12)
        self.chat.tag_configure("other", lmargin1=12, lmargin2=12, rmargin=100)
        self.chat.tag_configure("date", justify="center", foreground=MUTED, font=(FONT, 9), spacing1=8, spacing3=10)
        self.chat.tag_configure("empty", justify="center", foreground=MUTED, spacing1=35)
        self.chat.tag_configure("gap", font=(FONT, 5))
        self.chat.tag_configure("chosen", background="#fff1c2")
        actions = ttk.Frame(conversation, style="Card.TFrame", padding=(0, 8, 0, 0))
        actions.grid(row=2, column=0, sticky="ew")
        ttk.Label(actions, textvariable=self.selection_text, style="CardMuted.TLabel").pack(side="left")
        self.download_button = ttk.Button(actions, text="Сохранить файл", command=self._download)
        self.download_button.pack(side="right")
        self.reply_button = ttk.Button(actions, text="Ответить", command=self._reply)
        self.reply_button.pack(side="right", padx=8)

        composer = ttk.Frame(self, style="Card.TFrame", padding=14)
        composer.grid(row=3, column=0, sticky="ew", padx=20, pady=(12, 0))
        composer.columnconfigure(0, weight=1)
        self.reply_panel = ttk.Frame(composer, style="Card.TFrame")
        self.reply_panel.grid(row=0, column=0, sticky="ew", pady=(0, 8))
        ttk.Label(self.reply_panel, textvariable=self.reply_text, style="CardMuted.TLabel").pack(side="left")
        ttk.Button(self.reply_panel, text="Отменить ответ", command=self._cancel_reply).pack(side="right")
        self.reply_panel.grid_remove()
        self.input = tk.Text(composer, height=2, wrap="word", font=(FONT, 12),
                             background=SURFACE, foreground=TEXT, insertbackground=TEXT,
                             borderwidth=0, highlightthickness=0, padx=4, pady=6)
        self.input.grid(row=1, column=0, sticky="ew")
        self.input.bind("<Return>", self._enter)
        self.input.bind("<Shift-Return>", lambda event: None)
        buttons = ttk.Frame(composer, style="Card.TFrame")
        buttons.grid(row=2, column=0, sticky="ew", pady=(8, 0))
        self.file_button = ttk.Button(buttons, text="Прикрепить файл", command=self._upload)
        self.file_button.pack(side="left")
        ttk.Label(buttons, text="Enter — отправить · Shift+Enter — новая строка",
                  style="CardMuted.TLabel").pack(side="left", padx=14)
        self.send_button = ttk.Button(buttons, text="Отправить", style="Accent.TButton", command=self._send_text)
        self.send_button.pack(side="right")
        ttk.Label(self, textvariable=self.status, style="Muted.TLabel", wraplength=900).grid(
            row=4, column=0, sticky="w", padx=22, pady=12)
        self._render_messages()
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
            self.reply_panel.grid()
            self.input.focus_set()

    def _cancel_reply(self):
        self.reply_to = None
        self.reply_text.set("Ответ: не выбран")
        self.reply_panel.grid_remove()

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
        if not self.messages:
            self.chat.insert("end", "Пока здесь тихо.\nНапишите первое сообщение.", ("empty",))
        previous_day = None
        for message_id, message in sorted(self.messages.items()):
            moment = datetime.fromisoformat(message["timestamp"]).astimezone()
            day = moment.strftime("%d.%m.%Y")
            if day != previous_day:
                self.chat.insert("end", day + "\n", ("date",))
                previous_day = day
            tag = f"message_{message_id}"
            own = message["sender"] == self.connected_name
            appearance = "own" if own else "other"
            author = "Вы" if own else message["sender"]
            self.chat.insert("end", f"{author}  ·  {moment:%H:%M}\n", (tag, appearance, "author"))
            if message["reply"]:
                reply = message["reply"]
                preview = (reply["text"] or reply["file_name"] or "Файл").replace("\n", " ")
                self.chat.insert("end", f"Ответ {reply['sender']}: {preview[:100]}\n", (tag, appearance, "quote"))
            if message["text"]:
                self.chat.insert("end", message["text"] + "\n", (tag, appearance))
            if message["file"]:
                attachment = message["file"]
                size = f"{attachment['size'] / 1024:.1f} КБ"
                self.chat.insert("end", f"Файл: {attachment['name']}  ·  {size}\n", (tag, appearance, "attachment"))
            self.chat.insert("end", "\n", ("gap",))
            self.chat.tag_bind(tag, "<Button-1>", lambda event, number=message_id: self._select(number))
        self.chat.tag_raise("chosen")
        self.chat.configure(state="disabled")
        if scroll_to_end:
            self.chat.see("end")
        else:
            self.chat.yview_moveto(0 if history else old_position)
        if self.selected in self.messages:
            self._select(self.selected)

    def _update_buttons(self):
        active = self.connected and not self.busy
        self.badge.configure(text="Подключено" if self.connected else "Не подключено",
                             style="Online.TLabel" if self.connected else "Offline.TLabel")
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
                self.room_title.configure(text="Комната #" + packet["room"])
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


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s: %(message)s", datefmt="%H:%M:%S")
    logging.info("Открыт клиент. Подключитесь к серверу.")
    ChatApp().mainloop()
