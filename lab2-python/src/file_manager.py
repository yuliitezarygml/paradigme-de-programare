"""
📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (ENCAPSULATION)
Класс FileManager скрывает внутри себя детали работы с файловой системой.
Снаружи только: open_file(), save_file() — больше ничего не нужно знать.
"""
from __future__ import annotations

import os
from PyQt6.QtWidgets import QFileDialog, QMessageBox
from PyQt6.QtCore import QObject


class FileManager(QObject):
    """
    📌 ПРИНЦИП ООП #1: АБСТРАКЦИЯ (ABSTRACTION)
    Изолирует работу с файловой системой за простым интерфейсом.

    Поддерживаемые форматы:
        - Plain text (.txt, .py, .java, .md, .csv, ...)
        - HTML (с сохранением форматирования — Bold, Italic, Underline, цвет, шрифт)
    """

    SUPPORTED_FILTERS = (
        "HTML файлы (*.html *.htm);;"
        "Текстовые файлы (*.txt *.py *.java *.md *.csv *.json);;"
        "Все файлы (*.*)"
    )

    def __init__(self, parent: QObject | None = None) -> None:
        super().__init__(parent)
        # ────────────────────────────────────────────────────
        # 📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ — приватные атрибуты
        self._last_directory: str = os.path.expanduser("~")
        # ────────────────────────────────────────────────────

    # ──────────────────────────────────────────────────────
    # Открытие файла
    # ──────────────────────────────────────────────────────
    def open_file(self, parent_widget=None) -> tuple[str, str] | None:
        """
        Показывает диалог выбора файла.
        Возвращает (путь_к_файлу, содержимое) или None при отмене.
        """
        path, _ = QFileDialog.getOpenFileName(
            parent_widget,
            "Открыть файл",
            self._last_directory,
            self.SUPPORTED_FILTERS,
        )
        if not path:
            return None

        self._last_directory = os.path.dirname(path)

        try:
            content = self._read(path)
            return path, content
        except OSError as exc:
            self._show_error(parent_widget, f"Ошибка чтения файла:\n{exc}")
            return None

    def open_file_from_path(self, path: str, parent_widget=None) -> str | None:
        """Открыть файл по готовому пути (drag-and-drop, аргументы CLI)."""
        try:
            return self._read(path)
        except OSError as exc:
            self._show_error(parent_widget, f"Ошибка чтения файла:\n{exc}")
            return None

    # ──────────────────────────────────────────────────────
    # Сохранение файла
    # ──────────────────────────────────────────────────────
    def save_file(self, path: str, content: str, parent_widget=None) -> bool:
        """
        Сохраняет content в path.
        Возвращает True при успехе.
        """
        try:
            self._write(path, content)
            return True
        except OSError as exc:
            self._show_error(parent_widget, f"Ошибка сохранения:\n{exc}")
            return False

    def save_file_as(self, content: str, suggested_name: str = "document.html",
                     parent_widget=None) -> str | None:
        """
        Показывает диалог «Сохранить как...».
        Возвращает итоговый путь или None при отмене.
        """
        path, _ = QFileDialog.getSaveFileName(
            parent_widget,
            "Сохранить как...",
            os.path.join(self._last_directory, suggested_name),
            self.SUPPORTED_FILTERS,
        )
        if not path:
            return None

        self._last_directory = os.path.dirname(path)
        if self.save_file(path, content, parent_widget):
            return path
        return None

    # ──────────────────────────────────────────────────────
    # Приватные вспомогательные методы
    # ──────────────────────────────────────────────────────
    @staticmethod
    def _read(path: str) -> str:
        """Определяет кодировку и читает файл."""
        ext = os.path.splitext(path)[1].lower()
        mode = "r"
        encoding = "utf-8"
        with open(path, mode, encoding=encoding, errors="replace") as fh:
            return fh.read()

    @staticmethod
    def _write(path: str, content: str) -> None:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(content)

    @staticmethod
    def _show_error(parent_widget, message: str) -> None:
        if parent_widget is not None:
            QMessageBox.critical(parent_widget, "Ошибка файла", message)

    @staticmethod
    def get_filename(path: str) -> str:
        """Возвращает только имя файла без пути."""
        return os.path.basename(path) if path else "Новый документ"
