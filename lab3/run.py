"""Точка входа: выбор программы, отдельный клиент или отдельный сервер."""

import argparse
import logging
import subprocess
import sys
import time
from pathlib import Path
from protocol import DEFAULT_PORT


def main():
    parser = argparse.ArgumentParser(description="Лабораторная 3 — локальный чат")
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--server", action="store_true", help="Сервер только в консоли")
    modes.add_argument("--server-gui", action="store_true", help="Отдельная панель сервера")
    modes.add_argument("--client", action="store_true", help="Отдельное окно клиента")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--data-dir", type=Path, default=Path(__file__).parent / "data")
    args = parser.parse_args()
    # Только консоль. FileHandler намеренно не используется.
    logging.basicConfig(level=logging.INFO, stream=sys.stdout,
                        format="%(asctime)s %(name)s: %(message)s", datefmt="%H:%M:%S")
    if args.client:
        from app import ChatApp
        logging.info("Запущен клиент. Сетевые события будут видны в этой консоли.")
        ChatApp().mainloop()
        return
    if args.server_gui:
        from server_app import ServerApp
        logging.info("Открыта панель сервера. Нажмите «Запустить».")
        ServerApp(args.host, args.port, args.data_dir).mainloop()
        return
    if not args.server:
        launch_menu()
        return
    from server import ChatServer
    server = ChatServer(args.host, args.port, args.data_dir)
    server.start()
    print(f"Сервер запущен: {args.host}:{server.port}")
    print(f"История: {server.history.path}")
    print("Для остановки нажмите Ctrl+C.")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        pass
    finally:
        server.stop()


def launch_menu():
    import tkinter as tk
    from tkinter import ttk
    window = tk.Tk()
    window.title("Лабораторная 3 — запуск")
    window.geometry("420x210")
    window.resizable(False, False)
    ttk.Label(window, text="Сервер и клиент — отдельные программы.", padding=16).pack()

    def open_program(mode):
        subprocess.Popen([sys.executable, "-u", str(Path(__file__).resolve()), mode])

    ttk.Button(window, text="Открыть панель сервера",
               command=lambda: open_program("--server-gui")).pack(fill="x", padx=30, pady=5)
    ttk.Button(window, text="Открыть клиента",
               command=lambda: open_program("--client")).pack(fill="x", padx=30, pady=5)
    ttk.Label(window, text="Логи открытых программ видны в этом терминале.", padding=12).pack()
    logging.info("Выберите сервер или клиент в открывшемся окне.")
    window.mainloop()


if __name__ == "__main__":
    main()
