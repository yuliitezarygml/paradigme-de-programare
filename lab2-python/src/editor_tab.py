"""
📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE)
EditorTab наследует QWidget.

📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (ENCAPSULATION)
Состояние вкладки: путь к файлу, признак модификации,
сам компонент редактора — инкапсулированы внутри класса.
Взаимодействие только через открытые методы.
"""
from __future__ import annotations

import os
from PyQt6.QtCore import pyqtSignal, Qt
from PyQt6.QtGui import (
    QColor,
    QFont,
    QTextCharFormat,
    QTextCursor,
    QTextListFormat,
)
from PyQt6.QtWidgets import (
    QColorDialog,
    QFontDialog,
    QHBoxLayout,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)


class EditorTab(QWidget):
    """
    📌 ПРИНЦИП ООП #1: АБСТРАКЦИЯ (ABSTRACTION)
    Представляет собой единую вкладку редактора с файлом.
    Скрывает внутри сложность работы с QTextEdit, курсором,
    форматированием и отслеживанием изменений.
    """

    # Сигналы для связи с главным окном
    modified_changed = pyqtSignal(bool)           # документ изменён / сохранён
    cursor_position_changed = pyqtSignal(int, int) # (строка, колонка)
    title_changed = pyqtSignal(str)               # новое имя вкладки

    def __init__(self, file_path: str | None = None, content: str = "",
                 is_html: bool = False, parent: QWidget | None = None) -> None:
        super().__init__(parent)

        # ────────────────────────────────────────────────────
        # 📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ — закрытое состояние
        self._file_path: str | None = file_path
        self._is_modified: bool = False
        self._is_html: bool = is_html
        # ────────────────────────────────────────────────────

        self._build_ui(content)
        self._connect_signals()

    # ──────────────────────────────────────────────────────
    # Инициализация интерфейса
    # ──────────────────────────────────────────────────────
    def _build_ui(self, initial_content: str) -> None:
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        self._editor = QTextEdit(self)
        self._editor.setFont(QFont("Menlo", 13))
        self._editor.setAcceptRichText(True)

        if initial_content:
            if self._is_html or initial_content.lstrip().startswith(("<html", "<!DOCTYPE", "<p", "<div")):
                self._editor.setHtml(initial_content)
                self._is_html = True
            else:
                self._editor.setPlainText(initial_content)

        layout.addWidget(self._editor)

    def _connect_signals(self) -> None:
        # Отслеживание изменений текста
        self._editor.textChanged.connect(self._on_text_changed)
        # Отслеживание позиции курсора
        self._editor.cursorPositionChanged.connect(self._on_cursor_changed)

    # ──────────────────────────────────────────────────────
    # Геттеры и сеттеры (Инкапсуляция)
    # ──────────────────────────────────────────────────────
    @property
    def editor(self) -> QTextEdit:
        """Прямой доступ к QTextEdit для специализированных диалогов."""
        return self._editor

    @property
    def file_path(self) -> str | None:
        return self._file_path

    @file_path.setter
    def file_path(self, path: str | None) -> None:
        self._file_path = path
        self.title_changed.emit(self.title)

    @property
    def is_modified(self) -> bool:
        return self._is_modified

    @property
    def title(self) -> str:
        """Возвращает имя вкладки (с звёздочкой, если файл изменён)."""
        name = os.path.basename(self._file_path) if self._file_path else "Новый"
        return f"{name}*" if self._is_modified else name

    def get_content(self) -> str:
        """
        Возвращает содержимое.
        Если документ использует форматирование (шрифты, цвета) — отдаёт HTML.
        Если обычный текст — plain text.
        """
        if self._is_html or self._file_path and self._file_path.lower().endswith((".html", ".htm")):
            return self._editor.toHtml()
        # Проверяем, есть ли форматирование
        html = self._editor.toHtml()
        if any(tag in html for tag in ("font-weight:600", "font-weight:700", "font-weight:bold", "font-style:italic",
                                        "text-decoration: underline", "color:#", "font-family:")):
            return html
        return self._editor.toPlainText()

    def set_content(self, text: str, as_html: bool = False) -> None:
        """Задать содержимое и сбросить флаг модификации."""
        self._editor.blockSignals(True)
        if as_html or text.lstrip().startswith(("<html", "<!DOCTYPE")):
            self._editor.setHtml(text)
            self._is_html = True
        else:
            self._editor.setPlainText(text)
            self._is_html = False
        self._editor.blockSignals(False)
        self._set_modified(False)

    def mark_saved(self, path: str) -> None:
        """Вызывается после успешного сохранения."""
        self._file_path = path
        self._set_modified(False)

    # ──────────────────────────────────────────────────────
    # Методы форматирования (Требования п. d методички)
    # ──────────────────────────────────────────────────────
    def toggle_bold(self) -> None:
        """Переключить жирный шрифт (Bold)."""
        fmt = QTextCharFormat()
        weight = (
            QFont.Weight.Normal
            if self._editor.fontWeight() == QFont.Weight.Bold
            else QFont.Weight.Bold
        )
        fmt.setFontWeight(weight)
        self._merge_format_on_selection(fmt)

    def toggle_italic(self) -> None:
        """Переключить курсив (Italic)."""
        fmt = QTextCharFormat()
        fmt.setFontItalic(not self._editor.fontItalic())
        self._merge_format_on_selection(fmt)

    def toggle_underline(self) -> None:
        """Переключить подчёркивание (Underline)."""
        fmt = QTextCharFormat()
        fmt.setFontUnderline(not self._editor.fontUnderline())
        self._merge_format_on_selection(fmt)

    def set_font_family(self, family: str) -> None:
        """Установить семейство шрифтов."""
        fmt = QTextCharFormat()
        fmt.setFontFamily(family)
        self._merge_format_on_selection(fmt)

    def set_font_size(self, size_pt: float) -> None:
        """Установить размер шрифта (в пунктах)."""
        fmt = QTextCharFormat()
        fmt.setFontPointSize(size_pt)
        self._merge_format_on_selection(fmt)

    def set_text_color(self, color: QColor) -> None:
        """Установить цвет текста."""
        fmt = QTextCharFormat()
        fmt.setForeground(color)
        self._merge_format_on_selection(fmt)

    def set_background_color(self, color: QColor) -> None:
        """Установить цвет фона текста (выделитель / маркер)."""
        fmt = QTextCharFormat()
        fmt.setBackground(color)
        self._merge_format_on_selection(fmt)

    def choose_and_set_font(self) -> None:
        """Показать диалог выбора шрифта."""
        ok, font = QFontDialog.getFont(self._editor.currentFont(), self)
        if ok:
            fmt = QTextCharFormat()
            fmt.setFont(font)
            self._merge_format_on_selection(fmt)

    def choose_and_set_color(self) -> None:
        """Показать диалог палитры цветов текста."""
        col = QColorDialog.getColor(self._editor.textColor(), self, "Выберите цвет текста")
        if col.isValid():
            self.set_text_color(col)

    def set_alignment(self, align: Qt.AlignmentFlag) -> None:
        """Выравнивание абзаца: по левому, центру, правому краю, по ширине."""
        self._editor.setAlignment(align)

    # ──────────────────────────────────────────────────────
    # Статистика документа
    # ──────────────────────────────────────────────────────
    def get_stats(self) -> dict[str, int]:
        """Возвращает количество символов, слов и строк."""
        plain = self._editor.toPlainText()
        chars = len(plain)
        words = len(plain.split()) if plain else 0
        lines = plain.count("\n") + 1 if plain else 0
        return {"chars": chars, "words": words, "lines": lines}

    # ──────────────────────────────────────────────────────
    # Приватные методы
    # ──────────────────────────────────────────────────────
    def _merge_format_on_selection(self, fmt: QTextCharFormat) -> None:
        """
        Применяет формат к выделенному фрагменту,
        либо к будущему вводимому тексту (если выделения нет).
        """
        cursor = self._editor.textCursor()
        if not cursor.hasSelection():
            cursor.select(QTextCursor.SelectionType.WordUnderCursor)
        cursor.mergeCharFormat(fmt)
        self._editor.mergeCurrentCharFormat(fmt)
        self._is_html = True

    def _on_text_changed(self) -> None:
        if not self._is_modified:
            self._set_modified(True)

    def _on_cursor_changed(self) -> None:
        cursor = self._editor.textCursor()
        line = cursor.blockNumber() + 1
        col = cursor.columnNumber() + 1
        self.cursor_position_changed.emit(line, col)

    def _set_modified(self, modified: bool) -> None:
        self._is_modified = modified
        self.title_changed.emit(self.title)
        self.modified_changed.emit(modified)
