"""Сетевой клиент. Обновления окна передаются через очередь событий."""

import base64
import socket
import threading
from pathlib import Path

from protocol import MAX_FILE_SIZE, receive_packet, send_packet


class ChatClient:
    def __init__(self, on_event):
        self.on_event = on_event
        self._socket = None
        self._lock = threading.Lock()
        self._closed = threading.Event()

    def connect(self, host, port, name, room):
        try:
            connection = socket.create_connection((host, port), timeout=8)
            connection.settimeout(45)
            self._socket = connection
            if self._closed.is_set():
                connection.close()
                return
            self.send({"type": "join", "name": name, "room": room})
            threading.Thread(target=self._heartbeat, daemon=True).start()
            while not self._closed.is_set():
                packet = receive_packet(connection)
                if packet["type"] != "pong":
                    self.on_event(packet)
        except (OSError, ValueError, ConnectionError) as error:
            if not self._closed.is_set():
                self.on_event({"type": "disconnected", "message": str(error)})
        finally:
            self.close()

    def send(self, packet):
        with self._lock:
            if self._closed.is_set() or self._socket is None:
                raise ConnectionError("Нет подключения к серверу.")
            send_packet(self._socket, packet)

    def send_text(self, text, reply_to=None):
        self.send({"type": "text", "text": text, "reply_to": reply_to})

    def send_file(self, path, reply_to=None):
        path = Path(path)
        with path.open("rb") as source:
            data = source.read(MAX_FILE_SIZE + 1)
        if len(data) > MAX_FILE_SIZE:
            raise ValueError("Размер файла не должен превышать 10 МБ.")
        self.send({"type": "upload", "name": path.name, "reply_to": reply_to,
                   "data": base64.b64encode(data).decode("ascii")})

    def _heartbeat(self):
        while not self._closed.wait(15):
            try:
                self.send({"type": "ping"})
            except OSError:
                self.close()
                return

    def close(self):
        self._closed.set()
        if self._socket:
            try:
                self._socket.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            self._socket.close()
