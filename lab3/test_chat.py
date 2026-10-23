"""Проверки реального сетевого обмена без графического интерфейса."""

import socket
import base64
import json
import tempfile
import threading
import unittest
from pathlib import Path

from history import ChatHistory
from protocol import receive_packet, send_packet
from server import ChatServer, ClientSession


class ProtocolTests(unittest.TestCase):
    def test_unicode_message_round_trip(self):
        first, second = socket.socketpair()
        self.addCleanup(first.close)
        self.addCleanup(second.close)
        send_packet(first, {"type": "text", "text": "Привет! Salut, țară!"})
        self.assertEqual(receive_packet(second), {
            "type": "text", "text": "Привет! Salut, țară!"})


class SessionTests(unittest.TestCase):
    def test_slow_connection_does_not_block_dispatch(self):
        started, release, returned = threading.Event(), threading.Event(), threading.Event()

        class SlowSocket:
            def sendall(self, data):
                started.set()
                release.wait(2)

            def shutdown(self, how):
                release.set()

            def close(self):
                release.set()

        session = ClientSession(SlowSocket())
        def dispatch():
            session.send({"type": "message", "text": "Проверка"})
            returned.set()
        worker = threading.Thread(target=dispatch)
        try:
            worker.start()
            self.assertTrue(started.wait(1))
            self.assertTrue(returned.wait(0.2), "Медленный клиент блокирует рассылку")
        finally:
            release.set()
            session.close()
            worker.join(timeout=2)


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

    def restart_server(self):
        self.server.stop()
        self.server = ChatServer("127.0.0.1", 0, Path(self.directory.name))
        self.server.start()
        self.addCleanup(self.server.stop)

    def test_message_reaches_all_three_computers(self):
        alice, _ = self.connect_user("Alice")
        bob, _ = self.connect_user("Bob")
        carol, _ = self.connect_user("Carol")
        send_packet(alice, {"type": "text", "text": "Привет, комната!"})
        for connection in (alice, bob, carol):
            message = self.next_packet(connection, "message")["message"]
            self.assertEqual(message["text"], "Привет, комната!")
            self.assertEqual(message["sender"], "Alice")

    def test_server_creates_persistent_rooms_with_separate_json_files(self):
        self.server.create_room("Учёба")
        self.assertEqual([room["name"] for room in self.server.rooms()], ["General", "Учёба"])
        alice, welcome = self.connect_user("Alice", "Учёба")
        self.assertEqual(welcome["rooms"][1]["name"], "Учёба")
        send_packet(alice, {"type": "text", "text": "Задание на завтра"})
        self.next_packet(alice, "message")
        room_files = list((Path(self.directory.name) / "rooms").glob("*.json"))
        saved = [json.loads(path.read_text(encoding="utf-8")) for path in room_files]
        self.assertEqual({room["name"] for room in saved}, {"General", "Учёба"})
        study = next(room for room in saved if room["name"] == "Учёба")
        self.assertEqual(study["messages"][0]["text"], "Задание на завтра")
        self.restart_server()
        _, welcome = self.connect_user("Bob", "Учёба")
        self.assertEqual(welcome["messages"][0]["text"], "Задание на завтра")
        self.assertEqual([room["name"] for room in welcome["rooms"]], ["General", "Учёба"])

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

    def test_history_is_json_and_survives_server_restart(self):
        alice, _ = self.connect_user("Alice")
        send_packet(alice, {"type": "text", "text": "Сохранить в JSON"})
        self.next_packet(alice, "message")
        paths = list((Path(self.directory.name) / "rooms").glob("*.json"))
        self.assertEqual(len(paths), 1, "У каждой комнаты должен быть отдельный JSON")
        self.assertEqual(json.loads(paths[0].read_text(encoding="utf-8"))["messages"][0]["text"],
                         "Сохранить в JSON")
        self.restart_server()
        _, welcome = self.connect_user("Bob")
        self.assertEqual(welcome["messages"][0]["text"], "Сохранить в JSON")

    def test_occupied_port_explained_and_server_can_retry(self):
        other = ChatServer("127.0.0.1", self.server.port, Path(self.directory.name) / "other")
        with self.assertRaisesRegex(ValueError, "уже занят"):
            other.start()
        other.port = 0
        other.start()
        self.addCleanup(other.stop)
        alice, _ = self.connect_user("Alice")
        send_packet(alice, {"type": "text", "text": "Первый сервер продолжает работать"})
        self.assertEqual(self.next_packet(alice, "message")["message"]["text"],
                         "Первый сервер продолжает работать")

    def test_switching_rooms_isolates_messages_replies_files_and_members(self):
        self.server.create_room("Study")
        alice, _ = self.connect_user("Alice")
        bob, _ = self.connect_user("Bob", "Study")
        send_packet(alice, {"type": "upload", "name": "private.bin", "data": "aGVsbG8="})
        original = self.next_packet(alice, "message")["message"]
        send_packet(bob, {"type": "download", "file_id": original["file"]["id"]})
        self.assertIn("не найден", self.next_packet(bob, "error")["message"])
        send_packet(bob, {"type": "text", "text": "Чужой ответ", "reply_to": original["id"]})
        self.assertIn("не найдено", self.next_packet(bob, "error")["message"])
        send_packet(bob, {"type": "switch_room", "room": "General"})
        switched = self.next_packet(bob, "room")
        self.assertEqual(switched["room"], "General")
        self.assertEqual([m["id"] for m in switched["messages"]], [original["id"]])
        members = self.next_packet(bob, "members")
        self.assertEqual(members["room"], "General")
        self.assertEqual(members["names"], ["Alice", "Bob"])
        send_packet(bob, {"type": "text", "text": "Теперь вместе", "reply_to": original["id"]})
        reply = self.next_packet(alice, "message")["message"]
        self.assertEqual(reply["reply"]["file_name"], "private.bin")
        self.assertEqual({room["name"]: room["members"] for room in self.server.rooms()},
                         {"General": 2, "Study": 0})
        # В комнате Study не должно остаться сообщений из General.
        send_packet(bob, {"type": "switch_room", "room": "Study"})
        self.assertEqual(self.next_packet(bob, "room")["messages"], [])

    def test_closed_room_rejects_writes_but_keeps_history_and_downloads(self):
        alice, _ = self.connect_user("Alice")
        send_packet(alice, {"type": "upload", "name": "readable.txt", "data": "aGVsbG8="})
        original = self.next_packet(alice, "message")["message"]
        self.server.set_room_closed("General", True)
        self.assertEqual(self.next_packet(alice, "room_status"), {
            "type": "room_status", "room": "General", "closed": True})
        send_packet(alice, {"type": "text", "text": "Не записывать"})
        self.assertIn("закрыта", self.next_packet(alice, "error")["message"])
        send_packet(alice, {"type": "upload", "name": "blocked.txt", "data": "aGVsbG8="})
        self.assertIn("закрыта", self.next_packet(alice, "error")["message"])
        send_packet(alice, {"type": "history", "before": original["id"] + 1})
        page = self.next_packet(alice, "history")
        self.assertEqual(page["room"], "General")
        self.assertEqual(len(page["messages"]), 1)
        send_packet(alice, {"type": "download", "file_id": original["file"]["id"]})
        self.assertEqual(base64.b64decode(self.next_packet(alice, "file")["data"]), b"hello")
        _, welcome = self.connect_user("Bob")
        self.assertTrue(welcome["closed"])
        self.server.set_room_closed("General", False)
        self.assertFalse(self.next_packet(alice, "room_status")["closed"])
        send_packet(alice, {"type": "text", "text": "Снова открыта"})
        self.assertEqual(self.next_packet(alice, "message")["message"]["text"], "Снова открыта")

    def test_clear_history_keeps_ids_and_other_rooms_and_survives_restart(self):
        self.server.create_room("Study")
        alice, _ = self.connect_user("Alice")
        bob, _ = self.connect_user("Bob", "Study")
        send_packet(alice, {"type": "text", "text": "Удалить"})
        first = self.next_packet(alice, "message")["message"]
        send_packet(bob, {"type": "text", "text": "Оставить"})
        self.next_packet(bob, "message")
        self.server.clear_room_history("General")
        self.assertEqual(self.next_packet(alice, "history_cleared"), {
            "type": "history_cleared", "room": "General"})
        send_packet(alice, {"type": "text", "text": "Новый ответ", "reply_to": first["id"]})
        self.assertIn("не найдено", self.next_packet(alice, "error")["message"])
        self.restart_server()
        alice, welcome = self.connect_user("Alice")
        self.assertEqual(welcome["messages"], [])
        _, welcome = self.connect_user("Bob", "Study")
        self.assertEqual(welcome["messages"][0]["text"], "Оставить")
        send_packet(alice, {"type": "text", "text": "После очистки"})
        self.assertGreater(self.next_packet(alice, "message")["message"]["id"], first["id"])

    def test_delete_last_room_moves_clients_to_lobby_and_stays_deleted(self):
        alice, _ = self.connect_user("Alice")
        self.server.delete_room("General")
        self.assertEqual(self.next_packet(alice, "room_deleted")["room"], "General")
        lobby = self.next_packet(alice, "room")
        self.assertIsNone(lobby["room"])
        self.assertEqual(lobby["messages"], [])
        self.assertEqual(self.next_packet(alice, "rooms")["rooms"], [])
        send_packet(alice, {"type": "text", "text": "Некуда отправлять"})
        self.assertIn("выберите", self.next_packet(alice, "error")["message"])
        self.restart_server()
        self.assertEqual(self.server.rooms(), [])
        bob, welcome = self.connect_user("Bob", "")
        self.assertIsNone(welcome["room"])
        self.server.create_room("Новая")
        self.assertEqual(self.next_packet(bob, "rooms")["rooms"][0]["name"], "Новая")
        send_packet(bob, {"type": "switch_room", "room": "Новая"})
        self.assertEqual(self.next_packet(bob, "room")["room"], "Новая")

    def test_unknown_rooms_and_client_administration_are_rejected(self):
        connection = socket.create_connection(("127.0.0.1", self.server.port), timeout=3)
        self.addCleanup(connection.close)
        send_packet(connection, {"type": "join", "name": "Alice", "room": "Unknown"})
        self.assertEqual(receive_packet(connection)["type"], "error")
        alice, welcome = self.connect_user("Alice", "")
        self.assertEqual(welcome["room"], "General")
        send_packet(alice, {"type": "switch_room", "room": "Unknown"})
        self.assertIn("не найдена", self.next_packet(alice, "error")["message"])
        send_packet(alice, {"type": "create_room", "room": "Forbidden"})
        self.assertIn("Неизвестная", self.next_packet(alice, "error")["message"])
        self.assertEqual([room["name"] for room in self.server.rooms()], ["General"])


class MigrationTests(unittest.TestCase):
    def test_legacy_history_and_attachments_are_preserved_and_imported_once(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            (path / "files").mkdir()
            (path / "files" / "saved-file").write_bytes(b"old attachment")
            legacy = [{"id": 8, "room": "Old", "sender": "Alice", "text": "Из старой версии",
                       "timestamp": "2026-01-01T00:00:00+00:00", "reply": None,
                       "file": {"id": "saved-file", "name": "old.txt", "size": 14}}]
            original_bytes = json.dumps(legacy, ensure_ascii=False).encode("utf-8")
            (path / "history.json").write_bytes(original_bytes)
            history = ChatHistory(path)
            self.assertEqual(history.page("Old")[0], legacy)
            self.assertEqual(history.file_info("saved-file", "Old")["file_name"], "old.txt")
            history.clear_room_history("Old")
            history = ChatHistory(path)
            self.assertEqual(history.page("Old")[0], [])
            self.assertEqual(history.add("Old", "Bob", "Новое сообщение")["id"], 9)
            history.delete_room("Old")
            self.assertEqual(ChatHistory(path).rooms(), [])
            self.assertEqual((path / "history.json").read_bytes(), original_bytes)
            self.assertEqual((path / "files" / "saved-file").read_bytes(), b"old attachment")


if __name__ == "__main__":
    unittest.main(verbosity=2)
