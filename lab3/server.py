"""Сервер локального чата."""

import logging
import base64
import binascii
import errno
import queue
import socket
import threading
import uuid
from pathlib import Path

from history import ChatHistory
from protocol import DEFAULT_PORT, MAX_FILE_SIZE, receive_packet, send_packet

LOGGER = logging.getLogger(__name__)


def checked_text(value, label, maximum):
    if not isinstance(value, str) or not value.strip() or len(value) > maximum:
        raise ValueError(f"{label}: введите от 1 до {maximum} символов.")
    if any(ord(character) < 32 and character not in "\n\t" for character in value):
        raise ValueError(f"{label}: недопустимые управляющие символы.")
    return value.strip()


class ClientSession:
    """Соединение одного участника; имя устанавливает сервер при входе."""

    def __init__(self, connection):
        self.connection = connection
        self.name = ""
        self.room = ""
        # Отдельная очередь: медленный клиент не задерживает остальных.
        self._outgoing = queue.Queue(maxsize=100)
        self._closed = threading.Event()
        threading.Thread(target=self._send_loop, daemon=True).start()

    def send(self, packet):
        if self._closed.is_set():
            raise ConnectionError("Клиент отключён.")
        try:
            self._outgoing.put_nowait(packet)
        except queue.Full:
            self.close()
            raise ConnectionError("Клиент не успевает получать сообщения.") from None

    def _send_loop(self):
        while not self._closed.is_set():
            try:
                packet = self._outgoing.get(timeout=0.3)
            except queue.Empty:
                continue
            try:
                send_packet(self.connection, packet)
            except (OSError, ValueError):
                self.close()

    def close(self):
        self._closed.set()
        try:
            self.connection.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        self.connection.close()


class ChatServer:
    def __init__(self, host="0.0.0.0", port=DEFAULT_PORT, data_dir=None):
        self.host = host
        self.port = port
        self.history = ChatHistory(data_dir or Path(__file__).parent / "data")
        self._lock = threading.RLock()
        self._clients = set()
        self._connections = set()
        self._threads = set()
        self._stopping = threading.Event()
        self._listener = None
        self._accept_thread = None

    def start(self):
        listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            listener.bind((self.host, self.port))
            listener.listen(20)
            listener.settimeout(0.3)
        except OSError as error:
            listener.close()
            if error.errno == errno.EADDRINUSE:
                raise ValueError(f"Порт {self.port} уже занят другой программой. "
                                 "Укажите другой порт и в сервере, и в клиенте.") from None
            raise
        self._listener = listener
        self.port = listener.getsockname()[1]
        self._accept_thread = threading.Thread(target=self._accept, daemon=True)
        self._accept_thread.start()
        LOGGER.info("Сервер запущен на %s:%s. История: %s", self.host, self.port, self.history.path)

    def stop(self):
        if self._stopping.is_set():
            return
        self._stopping.set()
        if self._listener:
            self._listener.close()
        if self._accept_thread:
            self._accept_thread.join(timeout=2)
        with self._lock:
            connections = list(self._connections)
            threads = list(self._threads)
        for session in connections:
            session.close()
        for thread in threads:
            thread.join(timeout=2)
        LOGGER.info("Сервер остановлен.")

    def members(self):
        with self._lock:
            return sorted(f"{client.name} ({client.room})" for client in self._clients)

    def _accept(self):
        while not self._stopping.is_set():
            try:
                connection, _ = self._listener.accept()
            except socket.timeout:
                continue
            except OSError:
                break
            connection.settimeout(45)
            session = ClientSession(connection)
            thread = threading.Thread(target=self._handle, args=(session,), daemon=True)
            with self._lock:
                self._connections.add(session)
                self._threads.add(thread)
            thread.start()

    def _handle(self, session):
        try:
            packet = receive_packet(session.connection)
            if packet.get("type") != "join":
                raise ValueError("Сначала подключитесь к комнате.")
            session.name = checked_text(packet.get("name"), "Имя", 32)
            session.room = checked_text(packet.get("room"), "Комната", 48)
            if "\n" in session.name + session.room or "\t" in session.name + session.room:
                raise ValueError("Имя и комната должны быть в одну строку.")
            # Вход, история и рассылка сериализуются: сообщения не теряются
            # между чтением истории и подключением нового участника.
            with self._lock:
                if any(client.name.casefold() == session.name.casefold()
                       and client.room == session.room for client in self._clients):
                    raise ValueError("Это имя уже занято в комнате. Выберите другое.")
                messages, more = self.history.page(session.room)
                session.send({"type": "welcome", "name": session.name, "room": session.room,
                              "messages": messages, "has_more": more})
                self._clients.add(session)
                LOGGER.info("Вход: %s → комната %s; история: %s сообщений",
                            session.name, session.room, len(messages))
                self._broadcast_members(session.room)
            while not self._stopping.is_set():
                packet = receive_packet(session.connection)
                try:
                    self._dispatch(session, packet)
                except (ValueError, OSError) as error:
                    LOGGER.warning("%s: %s", session.name, error)
                    session.send({"type": "error", "message": str(error)})
        except (OSError, ConnectionError):
            pass
        except ValueError as error:
            LOGGER.warning("Подключение отклонено: %s", error)
            try:
                session.send({"type": "error", "message": str(error)})
            except OSError:
                pass
        except Exception:
            LOGGER.exception("Ошибка обработки клиента")
        finally:
            session.close()
            with self._lock:
                self._clients.discard(session)
                self._connections.discard(session)
                self._threads.discard(threading.current_thread())
                if session.name:
                    LOGGER.info("Выход: %s из комнаты %s", session.name, session.room)
                if not self._stopping.is_set():
                    self._broadcast_members(session.room)

    def _dispatch(self, session, packet):
        kind = packet.get("type")
        if kind == "ping":
            session.send({"type": "pong"})
        elif kind == "text":
            text = checked_text(packet.get("text"), "Сообщение", 4000)
            with self._lock:
                reply_to = self._reply_id(session, packet)
                message = self.history.add(session.room, session.name, text, reply_to)
                self._broadcast(session.room, {"type": "message", "message": message})
                LOGGER.info("[%s] %s: %r%s", session.room, session.name, text,
                            f" (ответ на #{reply_to})" if reply_to else "")
        elif kind == "history":
            before = packet.get("before")
            if type(before) is not int or before < 1:
                raise ValueError("Некорректный номер сообщения.")
            messages, more = self.history.page(session.room, before)
            session.send({"type": "history", "messages": messages, "has_more": more})
            LOGGER.info("История → %s: %s сообщений", session.name, len(messages))
        elif kind == "upload":
            self._upload(session, packet)
        elif kind == "download":
            file_id = packet.get("file_id")
            if not isinstance(file_id, str):
                raise ValueError("Некорректный идентификатор файла.")
            info = self.history.file_info(file_id, session.room)
            if not info:
                raise ValueError("Файл не найден в этой комнате.")
            try:
                data = (self.history.files_dir / file_id).read_bytes()
            except OSError as error:
                raise ValueError("Сохранённый файл недоступен на сервере.") from error
            session.send({"type": "file", "file_id": file_id, "name": info["file_name"],
                          "data": base64.b64encode(data).decode("ascii")})
            LOGGER.info("Скачивание → %s: %s (%s байт)", session.name, info["file_name"], len(data))
        else:
            raise ValueError("Неизвестная команда.")

    def _upload(self, session, packet):
        name = checked_text(packet.get("name"), "Имя файла", 255)
        # Имя для показа отделено от имени хранения: клиент не задаёт путь на сервере.
        name = name.replace("\\", "/").rsplit("/", 1)[-1]
        if name in ("", ".", "..") or "\n" in name or "\t" in name:
            raise ValueError("Некорректное имя файла.")
        encoded = packet.get("data")
        if not isinstance(encoded, str) or len(encoded) > 4 * ((MAX_FILE_SIZE + 2) // 3):
            raise ValueError("Размер файла не должен превышать 10 МБ.")
        try:
            data = base64.b64decode(encoded, validate=True)
        except (ValueError, binascii.Error) as error:
            raise ValueError("Файл повреждён при передаче.") from error
        if len(data) > MAX_FILE_SIZE:
            raise ValueError("Размер файла не должен превышать 10 МБ.")
        file_id = uuid.uuid4().hex
        destination = self.history.files_dir / file_id
        with self._lock:
            reply_to = self._reply_id(session, packet)
            try:
                destination.write_bytes(data)
                message = self.history.add(session.room, session.name, "", reply_to,
                                           {"id": file_id, "name": name, "size": len(data)})
            except Exception:
                destination.unlink(missing_ok=True)
                raise
            self._broadcast(session.room, {"type": "message", "message": message})
            LOGGER.info("[%s] Файл от %s: %s (%s байт)", session.room, session.name, name, len(data))

    def _reply_id(self, session, packet):
        message_id = packet.get("reply_to")
        if message_id is not None:
            if type(message_id) is not int or not self.history.find(message_id, session.room):
                raise ValueError("Сообщение для ответа не найдено в этой комнате.")
        return message_id

    def _broadcast(self, room, packet):
        for client in list(self._clients):
            if client.room == room:
                try:
                    client.send(packet)
                except OSError:
                    client.close()

    def _broadcast_members(self, room):
        self._broadcast(room, {"type": "members", "names": sorted(
            client.name for client in self._clients if client.room == room)})
