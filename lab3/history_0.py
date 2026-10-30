"""У каждой комнаты свой JSON-файл; старую общую историю сохраняем как есть."""

import copy
import json
import threading
import uuid
from datetime import datetime, timezone
from pathlib import Path


class ChatHistory:
    def __init__(self, directory):
        self.directory = Path(directory)
        self.directory.mkdir(parents=True, exist_ok=True)
        self.files_dir = self.directory / "files"
        self.files_dir.mkdir(exist_ok=True)
        self.path = self.directory / "rooms"
        self._lock = threading.RLock()
        self._rooms = {}
        self._paths = {}
        # Наличие папки также запоминает случай, когда администратор удалил все комнаты.
        if not self.path.exists():
            self._import_legacy()
            return
        for room_path in self.path.glob("*.json"):
            room = json.loads(room_path.read_text(encoding="utf-8"))
            if not isinstance(room, dict) or not isinstance(room.get("messages"), list):
                raise ValueError(f"Некорректный файл комнаты: {room_path.name}")
            name = room["name"]
            self._rooms[name] = room
            self._paths[name] = room_path

    def _import_legacy(self):
        legacy_path = self.directory / "history.json"
        messages = []
        if legacy_path.exists():
            messages = json.loads(legacy_path.read_text(encoding="utf-8"))
            if not isinstance(messages, list):
                raise ValueError("history.json должен содержать список сообщений.")
        # Файл history.json не перезаписывается и не удаляется при переносе.
        names = {message.get("room") or "General" for message in messages} or {"General"}
        self.path.mkdir()
        try:
            for name in sorted(names):
                room_messages = [dict(message, room=name) for message in messages
                                 if (message.get("room") or "General") == name]
                room = {"name": name, "closed": False,
                        "next_id": max((m["id"] for m in room_messages), default=0) + 1,
                        "messages": room_messages}
                self._paths[name] = self.path / f"{uuid.uuid4().hex}.json"
                self._save(name, room)
                self._rooms[name] = room
        except Exception:
            # Удаляем только созданную нами папку: после исправления импорт повторится.
            for room_path in self.path.iterdir():
                room_path.unlink()
            self.path.rmdir()
            raise

    def _save(self, name, room):
        path = self._paths[name]
        temporary = path.with_suffix(".json.tmp")
        temporary.write_text(json.dumps(room, ensure_ascii=False, indent=2), encoding="utf-8")
        temporary.replace(path)

    def _room(self, name):
        if name not in self._rooms:
            raise ValueError("Комната не найдена.")
        return self._rooms[name]

    def rooms(self):
        with self._lock:
            return [{"name": name, "closed": room["closed"],
                     "message_count": len(room["messages"])}
                    for name, room in sorted(self._rooms.items())]

    def default_room(self):
        with self._lock:
            if "General" in self._rooms:
                return "General"
            return min(self._rooms, default=None)

    def is_closed(self, name):
        with self._lock:
            return self._room(name)["closed"]

    def create_room(self, name):
        with self._lock:
            if any(existing.casefold() == name.casefold() for existing in self._rooms):
                raise ValueError("Комната с таким именем уже существует.")
            room = {"name": name, "closed": False, "next_id": 1, "messages": []}
            self._paths[name] = self.path / f"{uuid.uuid4().hex}.json"
            try:
                self._save(name, room)
            except Exception:
                self._paths.pop(name)
                raise
            self._rooms[name] = room

    def set_room_closed(self, name, closed):
        with self._lock:
            room = dict(self._room(name), closed=closed)
            self._save(name, room)
            self._rooms[name] = room

    def clear_room_history(self, name):
        with self._lock:
            # Счётчик сохраняется: новый ответ не может сослаться на старый номер.
            room = dict(self._room(name), messages=[])
            self._save(name, room)
            self._rooms[name] = room

    def delete_room(self, name):
        with self._lock:
            self._room(name)
            self._paths[name].unlink()
            del self._paths[name]
            del self._rooms[name]

    def add(self, room, sender, text, reply_to=None, attachment=None):
        with self._lock:
            saved_room = self._room(room)
            original = self.find(reply_to, room) if reply_to is not None else None
            reply = None
            if original:
                reply = {"id": original["id"], "sender": original["sender"],
                         "text": original["text"],
                         "file_name": original["file"]["name"] if original["file"] else None}
            message = {"id": saved_room["next_id"], "room": room, "sender": sender, "text": text,
                       "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds"),
                       "reply": reply, "file": attachment}
            updated = dict(saved_room, messages=saved_room["messages"] + [message],
                           next_id=saved_room["next_id"] + 1)
            # Сначала запись на диск, затем изменение списка в памяти.
            self._save(room, updated)
            self._rooms[room] = updated
            return copy.deepcopy(message)

    def find(self, message_id, room):
        with self._lock:
            return next((copy.deepcopy(m) for m in self._room(room)["messages"]
                         if m["id"] == message_id), None)

    def page(self, room, before=None, limit=100):
        with self._lock:
            messages = [m for m in self._room(room)["messages"]
                        if before is None or m["id"] < before]
            return copy.deepcopy(messages[-limit:]), len(messages) > limit

    def file_info(self, file_id, room):
        with self._lock:
            for message in self._room(room)["messages"]:
                attachment = message["file"]
                if attachment and attachment["id"] == file_id:
                    return {"file_id": file_id, "file_name": attachment["name"],
                            "file_size": attachment["size"]}
        return None
