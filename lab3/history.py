"""Модель сообщения и хранилище истории. Никаких зависимостей от GUI и сети."""

import sqlite3
import threading
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path


@dataclass(frozen=True)
class ChatMessage:
    id: int
    room: str
    sender: str
    text: str
    timestamp: str
    reply: dict | None = None
    file: dict | None = None

    def to_dict(self):
        return asdict(self)


class ChatHistory:
    """Инкапсулирует SQLite; Lock защищает общую БД от потоков клиентов."""

    def __init__(self, directory):
        self.directory = Path(directory)
        self.directory.mkdir(parents=True, exist_ok=True)
        self.files_dir = self.directory / "files"
        self.files_dir.mkdir(exist_ok=True)
        self._lock = threading.RLock()
        self._db = sqlite3.connect(self.directory / "history.sqlite3", check_same_thread=False)
        self._db.row_factory = sqlite3.Row
        self._db.executescript("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                room TEXT NOT NULL,
                sender TEXT NOT NULL,
                text TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                reply_to INTEGER REFERENCES messages(id),
                file_id TEXT,
                file_name TEXT,
                file_size INTEGER
            );
            CREATE INDEX IF NOT EXISTS messages_room_id ON messages(room, id);
        """)

    def add(self, room, sender, text, reply_to=None, attachment=None):
        attachment = attachment or {}
        with self._lock, self._db:
            cursor = self._db.execute("""
                INSERT INTO messages
                    (room, sender, text, timestamp, reply_to, file_id, file_name, file_size)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, (room, sender, text, datetime.now(timezone.utc).isoformat(timespec="seconds"),
                  reply_to, attachment.get("id"), attachment.get("name"), attachment.get("size")))
            return self.find(cursor.lastrowid, room)

    def find(self, message_id, room):
        with self._lock:
            row = self._db.execute(self._select() + " WHERE m.id = ? AND m.room = ?",
                                   (message_id, room)).fetchone()
            return self._to_message(row) if row else None

    def page(self, room, before=None, limit=100):
        with self._lock:
            rows = self._db.execute(self._select() + """
                WHERE m.room = ? AND (? IS NULL OR m.id < ?)
                ORDER BY m.id DESC LIMIT ?
            """, (room, before, before, limit + 1)).fetchall()
            return [self._to_message(row) for row in reversed(rows[:limit])], len(rows) > limit

    def file_info(self, file_id, room):
        with self._lock:
            row = self._db.execute("""
                SELECT file_id, file_name, file_size FROM messages
                WHERE file_id = ? AND room = ? LIMIT 1
            """, (file_id, room)).fetchone()
            return dict(row) if row else None

    def close(self):
        with self._lock:
            self._db.close()

    @staticmethod
    def _select():
        return """SELECT m.*, r.sender AS reply_sender, r.text AS reply_text,
                         r.file_name AS reply_file_name
                  FROM messages m LEFT JOIN messages r ON r.id = m.reply_to"""

    @staticmethod
    def _to_message(row):
        reply = None
        if row["reply_to"] is not None:
            reply = {"id": row["reply_to"], "sender": row["reply_sender"],
                     "text": row["reply_text"], "file_name": row["reply_file_name"]}
        attachment = None
        if row["file_id"]:
            attachment = {"id": row["file_id"], "name": row["file_name"], "size": row["file_size"]}
        return ChatMessage(row["id"], row["room"], row["sender"], row["text"],
                           row["timestamp"], reply, attachment).to_dict()
