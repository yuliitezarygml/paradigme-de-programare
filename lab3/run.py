"""Запуск окна либо отдельного сервера: python3 run.py [--server]."""

import argparse
import logging
import time
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description="Лабораторная 3 — локальный чат")
    parser.add_argument("--server", action="store_true", help="Сервер без графического окна")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=5000)
    parser.add_argument("--data-dir", type=Path, default=Path(__file__).parent / "data")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO)
    if not args.server:
        from app import ChatApp
        ChatApp().mainloop()
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


if __name__ == "__main__":
    main()
