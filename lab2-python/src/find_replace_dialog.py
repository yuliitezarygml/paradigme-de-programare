"""
📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (ENCAPSULATION)
Класс FindReplaceDialog скрывает внутри себя всю логику
поиска и замены подстрок в документе.

📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE)
FindReplaceDialog наследует стандартный QDialog из PyQt6.
"""
from __future__ import annotations

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import (
    QCheckBox,
    QDialog,
    QDialogButtonBox,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QMessageBox,
    QPushButton,
    QTextEdit,
    QVBoxLayout,
)


# ════════════════════════════════════════════════════════════════════════
# 📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE: extends QDialog)
# ════════════════════════════════════════════════════════════════════════
class FindReplaceDialog(QDialog):
    """
    📌 ПРИНЦИП ООП #1: АБСТРАКЦИЯ (ABSTRACTION)
    Диалог поиска и замены текста.
    Снаружи вызывается просто: dialog.exec() — внутри работает вся логика.
    """

    def __init__(self, editor: QTextEdit, parent=None) -> None:
        super().__init__(parent)
        # ────────────────────────────────────────────────────
        # 📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ — приватные поля
        self._editor = editor
        self._last_found_pos: int = -1
        # ────────────────────────────────────────────────────

        self.setWindowTitle("Поиск и замена")
        self.setMinimumWidth(420)
        self.setModal(False)          # не блокирует редактор
        self._build_ui()

    # ──────────────────────────────────────────────────────
    # Построение интерфейса (приватный)
    # ──────────────────────────────────────────────────────
    def _build_ui(self) -> None:
        layout = QVBoxLayout(self)

        # — Строки ввода ——————————————————————————————————
        search_row = QHBoxLayout()
        search_row.addWidget(QLabel("Найти:"))
        self._search_input = QLineEdit()
        self._search_input.setPlaceholderText("Введите текст для поиска…")
        search_row.addWidget(self._search_input)
        layout.addLayout(search_row)

        replace_row = QHBoxLayout()
        replace_row.addWidget(QLabel("Заменить:"))
        self._replace_input = QLineEdit()
        self._replace_input.setPlaceholderText("Текст замены (можно оставить пустым)…")
        replace_row.addWidget(self._replace_input)
        layout.addLayout(replace_row)

        # — Опции ———————————————————————————————————————
        opts_group = QGroupBox("Параметры поиска")
        opts_layout = QHBoxLayout(opts_group)
        self._case_check = QCheckBox("С учётом регистра")
        self._backward_check = QCheckBox("Искать назад ←")
        self._whole_word_check = QCheckBox("Целое слово")
        opts_layout.addWidget(self._case_check)
        opts_layout.addWidget(self._backward_check)
        opts_layout.addWidget(self._whole_word_check)
        layout.addWidget(opts_group)

        # — Кнопки ——————————————————————————————————————
        btn_layout = QHBoxLayout()

        self._btn_find = QPushButton("Найти далее →")
        self._btn_find.setDefault(True)
        self._btn_find.clicked.connect(self._find_next)

        self._btn_replace = QPushButton("Заменить")
        self._btn_replace.clicked.connect(self._replace_one)

        self._btn_replace_all = QPushButton("Заменить всё")
        self._btn_replace_all.clicked.connect(self._replace_all)

        btn_close = QPushButton("Закрыть")
        btn_close.clicked.connect(self.close)

        btn_layout.addWidget(self._btn_find)
        btn_layout.addWidget(self._btn_replace)
        btn_layout.addWidget(self._btn_replace_all)
        btn_layout.addStretch()
        btn_layout.addWidget(btn_close)
        layout.addLayout(btn_layout)

        # Подключить Enter в поле поиска к «Найти далее»
        self._search_input.returnPressed.connect(self._find_next)

    # ──────────────────────────────────────────────────────
    # Публичные методы (интерфейс диалога)
    # ──────────────────────────────────────────────────────
    def set_editor(self, editor: QTextEdit) -> None:
        """Переключить диалог на другой редактор (при смене вкладки)."""
        self._editor = editor
        self._last_found_pos = -1

    def open_with_selection(self) -> None:
        """Открыть диалог, предзаполнив поле выделенным текстом."""
        sel = self._editor.textCursor().selectedText()
        if sel:
            self._search_input.setText(sel)
        self.show()
        self.raise_()
        self._search_input.setFocus()
        self._search_input.selectAll()

    # ──────────────────────────────────────────────────────
    # Приватная логика поиска / замены
    # ──────────────────────────────────────────────────────
    def _build_flags(self):
        """Собирает флаги поиска из чекбоксов."""
        from PyQt6.QtGui import QTextDocument
        flags = QTextDocument.FindFlag(0)
        if self._case_check.isChecked():
            flags |= QTextDocument.FindFlag.FindCaseSensitively
        if self._backward_check.isChecked():
            flags |= QTextDocument.FindFlag.FindBackward
        if self._whole_word_check.isChecked():
            flags |= QTextDocument.FindFlag.FindWholeWords
        return flags

    def _find_next(self) -> bool:
        """
        📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ (POLYMORPHISM)
        В зависимости от флагов один и тот же метод ищет
        вперёд или назад, с регистром или без — поведение меняется.
        """
        query = self._search_input.text()
        if not query:
            return False

        found = self._editor.find(query, self._build_flags())

        if not found:
            # Перемотать документ и попробовать ещё раз (wrap-around)
            cursor = self._editor.textCursor()
            if self._backward_check.isChecked():
                cursor.movePosition(cursor.MoveOperation.End)
            else:
                cursor.movePosition(cursor.MoveOperation.Start)
            self._editor.setTextCursor(cursor)
            found = self._editor.find(query, self._build_flags())

            if not found and self.isVisible():
                QMessageBox.information(
                    self, "Поиск", f'Текст «{query}» не найден.'
                )
        return found

    def _replace_one(self) -> None:
        """Заменяет текущее выделение (если совпадает) и ищет следующее."""
        query = self._search_input.text()
        replacement = self._replace_input.text()
        if not query:
            return

        cursor = self._editor.textCursor()
        selected = cursor.selectedText()

        # Сравниваем с учётом регистра или без
        match = (
            selected == query
            if self._case_check.isChecked()
            else selected.lower() == query.lower()
        )

        if match:
            cursor.insertText(replacement)

        self._find_next()

    def _replace_all(self) -> None:
        """Заменяет ВСЕ вхождения сразу (атомарно — одним undo-шагом)."""
        query = self._search_input.text()
        replacement = self._replace_input.text()
        if not query:
            return

        # Начало документа
        cursor = self._editor.textCursor()
        cursor.movePosition(cursor.MoveOperation.Start)
        self._editor.setTextCursor(cursor)

        count = 0
        from PyQt6.QtGui import QTextDocument
        flags = QTextDocument.FindFlag(0)
        if self._case_check.isChecked():
            flags |= QTextDocument.FindFlag.FindCaseSensitively
        if self._whole_word_check.isChecked():
            flags |= QTextDocument.FindFlag.FindWholeWords

        # Один большой блок undo для всех замен
        cursor.beginEditBlock()
        while self._editor.find(query, flags):
            self._editor.textCursor().insertText(replacement)
            count += 1
        cursor.endEditBlock()

        if self.isVisible():
            QMessageBox.information(
                self, "Замена завершена", f"Заменено вхождений: {count}"
            )
