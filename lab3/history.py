"""История чата в обычном JSON. Сеть и окно о способе хранения не знают."""

import copy
import json
import threading
from datetime import datetime, timezone
from pathlib import Path


class ChatHistory:
    def __init__(self, directory):
        self.directory = Path(directory)
        self.directory.mkdir(parents=True, exist_ok=True)
        self.files_dir = self.directory / "files"
        self.files_dir.mkdir(exist_ok=True)
        self.path = self.directory / "history.json"
        self._lock = threading.RLock()
        self._messages = []
        if self.path.exists():
            self._messages = json.loads(self.path.read_text(encoding="utf-8"))
            if not isinstance(self._messages, list):
                raise ValueError("history.json должен содержать список сообщений.")
        else:
            self._save([])
        self._next_id = max((m["id"] for m in self._messages), default=0) + 1

    def add(self, room, sender, text, reply_to=None, attachment=None):
        with self._lock:
            original = self.find(reply_to, room) if reply_to is not None else None
            reply = None
            if original:
                reply = {"id": original["id"], "sender": original["sender"],
                         "text": original["text"],
                         "file_name": original["file"]["name"] if original["file"] else None}
            message = {"id": self._next_id, "room": room, "sender": sender, "text": text,
                       "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds"),
                       "reply": reply, "file": attachment}
            # Сначала запись на диск, затем изменение списка в памяти.
            updated = self._messages + [message]
            self._save(updated)
            self._messages = updated
            self._next_id += 1
            return copy.deepcopy(message)

    def _save(self, messages):
        temporary = self.path.with_suffix(".json.tmp")
        temporary.write_text(json.dumps(messages, ensure_ascii=False, indent=2), encoding="utf-8")
        temporary.replace(self.path)

    def find(self, message_id, room):
        with self._lock:
            return next((copy.deepcopy(m) for m in self._messages
                         if m["id"] == message_id and m["room"] == room), None)

    def page(self, room, before=None, limit=100):
        with self._lock:
            messages = [m for m in self._messages if m["room"] == room
                        and (before is None or m["id"] < before)]
            return copy.deepcopy(messages[-limit:]), len(messages) > limit

    def file_info(self, file_id, room):
        with self._lock:
            for message in self._messages:
                attachment = message["file"]
                if message["room"] == room and attachment and attachment["id"] == file_id:
                    return {"file_id": file_id, "file_name": attachment["name"],
                            "file_size": attachment["size"]}
        return None
