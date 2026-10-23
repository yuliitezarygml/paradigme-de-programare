"""Отдельная панель сервера. Логи видны здесь и в консоли, но не пишутся в файл."""

import logging
import queue
import socket
import tkinter as tk
from tkinter import messagebox, scrolledtext, ttk

from server import ChatServer
from protocol import DEFAULT_PORT
from theme import FONT, apply_theme


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
        apply_theme(self)
        self.title("Панель сервера — Rețeaua locală")
        self.geometry("900x650")
        self.minsize(760, 520)
        self.server = None
        self.data_dir = data_dir
        self.events = queue.Queue()
        self.host = tk.StringVar(value=host)
        self.port = tk.StringVar(value=str(port))
        self.status = tk.StringVar(value="Сервер остановлен.")
        self.people = tk.StringVar(value="Участники: —")
        self.columnconfigure(0, weight=1)
        self.rowconfigure(4, weight=1)
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
        ttk.Label(self, text="События сервера", font=(FONT, 12, "bold")).grid(
            row=3, column=0, sticky="w", padx=22, pady=(0, 10))
        self.logs = scrolledtext.ScrolledText(
            self, font=("Menlo", 11), wrap="word", state="disabled", padx=14, pady=14,
            background="#152033", foreground="#dbe7f5", borderwidth=0, highlightthickness=0)
        self.logs.grid(row=4, column=0, sticky="nsew", padx=20)
        ttk.Label(self, text="Логи видны здесь и в терминале · Файл логов не создаётся",
                  style="Muted.TLabel").grid(row=5, column=0, sticky="w", padx=22, pady=14)
        self.log_handler = WindowLogHandler(self.events)
        logging.getLogger("server").addHandler(self.log_handler)
        self.protocol("WM_DELETE_WINDOW", self.close)
        self.after(100, self.refresh)

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

    def stop_server(self):
        if self.server:
            self.server.stop()
            self.server = None
        self.status.set("Сервер остановлен. История сохранена в data/history.json.")
        self.people.set("Участники: —")
        for widget in (self.host_entry, self.port_entry, self.start_button):
            widget.configure(state="normal")
        self.stop_button.configure(state="disabled")
        self.badge.configure(text="Остановлен", style="Offline.TLabel")

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
        self.after(100, self.refresh)

    def close(self):
        self.stop_server()
        logging.getLogger("server").removeHandler(self.log_handler)
        self.destroy()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s: %(message)s", datefmt="%H:%M:%S")
    logging.info("Открыта панель сервера. Нажмите «Запустить сервер».")
    ServerApp().mainloop()
