"""Проверки реального сетевого обмена без графического интерфейса."""

import socket
import base64
import tempfile
import unittest
from pathlib import Path

from protocol import receive_packet, send_packet
from server import ChatServer


class ProtocolTests(unittest.TestCase):
    def test_unicode_message_round_trip(self):
        first, second = socket.socketpair()
        self.addCleanup(first.close)
        self.addCleanup(second.close)
        send_packet(first, {"type": "text", "text": "Привет! Salut, țară!"})
        self.assertEqual(receive_packet(second), {
            "type": "text", "text": "Привет! Salut, țară!"})


class NetworkTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.server = ChatServer("127.0.0.1", 0, Path(self.directory.name))
        self.server.start()
        self.addCleanup(self.server.stop)

    def connect_user(self, name, room="General"):
        connection = socket.create_connection(("127.0.0.1", self.server.port), timeout=3)
        self.addCleanup(connection.close)
        send_packet(connection, {"type": "join", "name": name, "room": room})
        welcome = receive_packet(connection)
        self.assertEqual(welcome["type"], "welcome")
        return connection, welcome

    def next_packet(self, connection, kind):
        for _ in range(20):
            packet = receive_packet(connection)
            if packet["type"] == kind:
                return packet
        self.fail(f"Не получен пакет {kind}")

    def test_message_reaches_all_three_computers(self):
        alice, _ = self.connect_user("Alice")
        bob, _ = self.connect_user("Bob")
        carol, _ = self.connect_user("Carol")
        send_packet(alice, {"type": "text", "text": "Привет, комната!"})
        for connection in (alice, bob, carol):
            message = self.next_packet(connection, "message")["message"]
            self.assertEqual(message["text"], "Привет, комната!")
            self.assertEqual(message["sender"], "Alice")

    def test_reply_and_history_for_a_new_participant(self):
        alice, _ = self.connect_user("Alice")
        send_packet(alice, {"type": "text", "text": "Встречаемся в 12:00?"})
        original = self.next_packet(alice, "message")["message"]
        bob, _ = self.connect_user("Bob")
        send_packet(bob, {"type": "text", "text": "Да!", "reply_to": original["id"]})
        reply = self.next_packet(bob, "message")["message"]
        self.assertEqual(reply["reply"], {
            "id": original["id"], "sender": "Alice", "text": "Встречаемся в 12:00?",
            "file_name": None})
        _, welcome = self.connect_user("Carol")
        self.assertEqual([message["text"] for message in welcome["messages"]],
                         ["Встречаемся в 12:00?", "Да!"])

    def test_binary_file_can_be_received_by_another_computer(self):
        alice, _ = self.connect_user("Alice")
        bob, _ = self.connect_user("Bob")
        contents = b"\x00\xff\x10binary\r\n" * 1000
        send_packet(alice, {"type": "upload", "name": "пример.bin",
                            "data": base64.b64encode(contents).decode("ascii")})
        message = self.next_packet(bob, "message")["message"]
        self.assertEqual(message["file"]["name"], "пример.bin")
        self.assertEqual(message["file"]["size"], len(contents))
        send_packet(bob, {"type": "download", "file_id": message["file"]["id"]})
        download = self.next_packet(bob, "file")
        self.assertEqual(base64.b64decode(download["data"]), contents)


if __name__ == "__main__":
    unittest.main(verbosity=2)
