"""Простой выбор: открыть сервер или клиент. Параметры командной строки не нужны."""

import subprocess
import sys
import tkinter as tk
from pathlib import Path
from tkinter import ttk

from theme import apply_chat_theme


def open_program(filename):
    # Новый процесс = отдельное окно. Его логи остаются в этом терминале.
    path = Path(__file__).parent / filename
    subprocess.Popen([sys.executable, "-u", str(path)])


if __name__ == "__main__":
    window = tk.Tk()
    apply_chat_theme(window)
    window.title("Локальный чат — запуск")
    window.geometry("440x280")
    window.resizable(False, False)
    ttk.Label(window, text="Локальный чат", style="Title.TLabel").pack(pady=(28, 6))
    ttk.Label(window, text="Лабораторная 3 · Rețeaua locală", style="Muted.TLabel").pack(pady=(0, 20))
    ttk.Button(window, text="Панель сервера", style="Accent.TButton",
               command=lambda: open_program("server_app.py")).pack(fill="x", padx=40, pady=5)
    ttk.Button(window, text="Открыть клиент",
               command=lambda: open_program("app.py")).pack(fill="x", padx=40, pady=5)
    ttk.Label(window, text="Логи будут видны в терминале.", style="Muted.TLabel").pack(pady=14)
    print("Выберите сервер или клиент в открывшемся окне.", flush=True)
    window.mainloop()
