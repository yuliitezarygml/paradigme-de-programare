"""Общий протокол клиента и сервера: длина сообщения + JSON в UTF-8."""

import json
import struct

DEFAULT_PORT = 5050
MAX_FILE_SIZE = 10 * 1024 * 1024
MAX_PACKET_SIZE = 15 * 1024 * 1024


def send_packet(connection, packet):
    """TCP — поток байтов, поэтому перед JSON отправляются 4 байта его длины.

    Одновременную отправку из разных потоков вызывающий код защищает Lock.
    """
    data = json.dumps(packet, ensure_ascii=False).encode("utf-8")
    if not 0 < len(data) <= MAX_PACKET_SIZE:
        raise ValueError("Слишком большое сетевое сообщение.")
    connection.sendall(struct.pack("!I", len(data)) + data)


def receive_packet(connection):
    header = _read_exactly(connection, 4)
    size = struct.unpack("!I", header)[0]
    if not 0 < size <= MAX_PACKET_SIZE:
        raise ValueError("Недопустимый размер сетевого сообщения.")
    try:
        packet = json.loads(_read_exactly(connection, size).decode("utf-8"))
    except (UnicodeError, json.JSONDecodeError) as error:
        raise ValueError("Повреждённое сетевое сообщение.") from error
    if not isinstance(packet, dict):
        raise ValueError("Сетевое сообщение должно быть объектом JSON.")
    return packet


def _read_exactly(connection, size):
    """recv может вернуть лишь часть сообщения — собираем все части."""
    result = bytearray()
    while len(result) < size:
        part = connection.recv(size - len(result))
        if not part:
            raise ConnectionError("Соединение закрыто.")
        result.extend(part)
    return bytes(result)
