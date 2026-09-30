"""
══════════════════════════════════════════════════════════════════════════════
Главное окно текстового редактора (Lab 2 - POO Python / PyQt6)

📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE)
TextEditorWindow наследует класс QMainWindow из библиотеки PyQt6.

📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (ENCAPSULATION)
Все компоненты окна (меню, тулбар, вкладки, диалог поиска, статус-бар)
объявлены как приватные атрибуты с префиксом '_'.
Доступ к ним и управление состоянием производятся через методы.

📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ (POLYMORPHISM)
События и команды меню маршрутизируются к текущей активной вкладке:
независимо от того, какой документ открыт (TXT или HTML со стилями),
вызываются единые методы форматирования и сохранения.
══════════════════════════════════════════════════════════════════════════════
"""
from __future__ import annotations

import os
from PyQt6.QtCore import Qt
from PyQt6.QtGui import (
    QAction,
    QColor,
    QFont,
    QIcon,
    QKeySequence,
)
from PyQt6.QtWidgets import (
    QApplication,
    QColorDialog,
    QFontComboBox,
    QLabel,
    QMainWindow,
    QMessageBox,
    QSpinBox,
    QTabWidget,
    QToolBar,
    QToolButton,
    QWidget,
)

from editor_tab import EditorTab
from file_manager import FileManager
from find_replace_dialog import FindReplaceDialog


# ══════════════════════════════════════════════════════════════════════════════
# 📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE: extends QMainWindow)
# ══════════════════════════════════════════════════════════════════════════════
class TextEditorWindow(QMainWindow):
    """
    📌 ПРИНЦИП ООП #1: АБСТРАКЦИЯ (ABSTRACTION)
    Главное окно текстового редактора. Объединяет в единую систему
    многодокументный интерфейс (MDI), панель инструментов, строку состояния
    и взаимодействие с файловым менеджером.
    """

    def __init__(self) -> None:
        super().__init__()

        # ──────────────────────────────────────────────────────────────────────
        # 📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ — приватные поля состояния окна
        self._file_manager: FileManager = FileManager(self)
        self._find_dialog: FindReplaceDialog | None = None
        self._tab_widget: QTabWidget = QTabWidget(self)

        # Элементы строки состояния
        self._status_pos: QLabel = QLabel("Стр 1, Кол 1")
        self._status_stats: QLabel = QLabel("Символов: 0 | Слов: 0")
        self._status_encoding: QLabel = QLabel("UTF-8")
        # ──────────────────────────────────────────────────────────────────────

        self._init_window()
        self._build_menu()
        self._build_toolbar()
        self._build_status_bar()
        self._build_tabs()

        # Открываем первую чистую вкладку по умолчанию
        self.new_tab()

    # ──────────────────────────────────────────────────────────────────────────
    # Инициализация параметров окна
    # ──────────────────────────────────────────────────────────────────────────
    def _init_window(self) -> None:
        self.setWindowTitle("Текстовый редактор - Laborator 2 (POO Python)")
        self.resize(1000, 700)
        self.setMinimumSize(600, 400)
        # Центрирование окна на экране
        screen = QApplication.primaryScreen()
        if screen:
            geo = screen.availableGeometry()
            self.move(
                (geo.width() - self.width()) // 2,
                (geo.height() - self.height()) // 2,
            )

    # ──────────────────────────────────────────────────────────────────────────
    # Многодокументный интерфейс (Вкладки / Tabs)
    # ──────────────────────────────────────────────────────────────────────────
    def _build_tabs(self) -> None:
        self._tab_widget.setTabsClosable(True)
        self._tab_widget.setMovable(True)
        self._tab_widget.setDocumentMode(True)

        # Кнопка "+" для быстрого создания новой вкладки в углу
        new_tab_btn = QToolButton(self)
        new_tab_btn.setText("+")
        new_tab_btn.setToolTip("Создать новую вкладку (Ctrl+N / Cmd+N)")
        new_tab_btn.clicked.connect(self.new_tab)
        self._tab_widget.setCornerWidget(new_tab_btn, Qt.Corner.TopRightCorner)

        # Сигналы вкладок
        self._tab_widget.tabCloseRequested.connect(self.close_tab)
        self._tab_widget.currentChanged.connect(self._on_tab_changed)

        self.setCentralWidget(self._tab_widget)

    @property
    def current_tab(self) -> EditorTab | None:
        """
        📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (Геттер текущей активной вкладки)
        Возвращает экземпляр EditorTab или None, если вкладок нет.
        """
        widget = self._tab_widget.currentWidget()
        return widget if isinstance(widget, EditorTab) else None

    def new_tab(self, file_path: str | None = None, content: str = "",
                is_html: bool = False) -> EditorTab:
        """
        Создаёт новую вкладку редактора.
        """
        tab = EditorTab(file_path=file_path, content=content, is_html=is_html, parent=self)

        # Подключение сигналов вкладки к главному окну
        tab.title_changed.connect(lambda title, t=tab: self._update_tab_title(t, title))
        tab.cursor_position_changed.connect(self._update_cursor_status)
        tab.modified_changed.connect(lambda _: self._update_stats_status())

        index = self._tab_widget.addTab(tab, tab.title)
        self._tab_widget.setCurrentIndex(index)
        tab.editor.setFocus()
        self._update_stats_status()
        return tab

    def close_tab(self, index: int) -> bool:
        """
        Закрывает вкладку с проверкой на несохранённые изменения.
        Возвращает True, если вкладка закрыта.
        """
        widget = self._tab_widget.widget(index)
        if not isinstance(widget, EditorTab):
            return False

        if widget.is_modified:
            name = os.path.basename(widget.file_path) if widget.file_path else "Новый документ"
            res = QMessageBox.question(
                self,
                "Несохранённые изменения",
                f"Файл «{name}» был изменён.\nСохранить изменения перед закрытием?",
                QMessageBox.StandardButton.Save
                | QMessageBox.StandardButton.Discard
                | QMessageBox.StandardButton.Cancel,
                QMessageBox.StandardButton.Save,
            )
            if res == QMessageBox.StandardButton.Save:
                if not self.save_file():
                    return False
            elif res == QMessageBox.StandardButton.Cancel:
                return False

        self._tab_widget.removeTab(index)
        widget.deleteLater()

        # Если все вкладки закрыты, открываем пустую
        if self._tab_widget.count() == 0:
            self.new_tab()
        return True

    def _update_tab_title(self, tab: EditorTab, title: str) -> None:
        idx = self._tab_widget.indexOf(tab)
        if idx != -1:
            self._tab_widget.setTabText(idx, title)
            if tab.file_path:
                self._tab_widget.setTabToolTip(idx, tab.file_path)

    def _on_tab_changed(self, index: int) -> None:
        tab = self.current_tab
        if tab:
            cursor = tab.editor.textCursor()
            self._update_cursor_status(cursor.blockNumber() + 1, cursor.columnNumber() + 1)
            self._update_stats_status()
            if self._find_dialog and self._find_dialog.isVisible():
                self._find_dialog.set_editor(tab.editor)

    # ──────────────────────────────────────────────────────────────────────────
    # Главное меню программы
    # ──────────────────────────────────────────────────────────────────────────
    def _build_menu(self) -> None:
        mb = self.menuBar()

        # ── 1. Файл ───────────────────────────────────────────────────────────
        file_menu = mb.addMenu("&Файл")

        act_new = QAction("Новая вкладка", self)
        act_new.setShortcut(QKeySequence.StandardKey.New)
        act_new.triggered.connect(self.new_tab)
        file_menu.addAction(act_new)

        act_open = QAction("Открыть файл...", self)
        act_open.setShortcut(QKeySequence.StandardKey.Open)
        act_open.triggered.connect(self.open_file)
        file_menu.addAction(act_open)

        file_menu.addSeparator()

        act_save = QAction("Сохранить", self)
        act_save.setShortcut(QKeySequence.StandardKey.Save)
        act_save.triggered.connect(self.save_file)
        file_menu.addAction(act_save)

        act_save_as = QAction("Сохранить как...", self)
        act_save_as.setShortcut(QKeySequence.StandardKey.SaveAs)
        act_save_as.triggered.connect(self.save_file_as)
        file_menu.addAction(act_save_as)

        file_menu.addSeparator()

        act_close_tab = QAction("Закрыть вкладку", self)
        act_close_tab.setShortcut(QKeySequence.StandardKey.Close)
        act_close_tab.triggered.connect(lambda: self.close_tab(self._tab_widget.currentIndex()))
        file_menu.addAction(act_close_tab)

        act_exit = QAction("Выход", self)
        act_exit.setShortcut(QKeySequence.StandardKey.Quit)
        act_exit.triggered.connect(self.close)
        file_menu.addAction(act_exit)

        # ── 2. Правка ─────────────────────────────────────────────────────────
        edit_menu = mb.addMenu("&Правка")

        act_undo = QAction("Отменить", self)
        act_undo.setShortcut(QKeySequence.StandardKey.Undo)
        act_undo.triggered.connect(lambda: self.current_tab.editor.undo() if self.current_tab else None)
        edit_menu.addAction(act_undo)

        act_redo = QAction("Повторить", self)
        act_redo.setShortcut(QKeySequence.StandardKey.Redo)
        act_redo.triggered.connect(lambda: self.current_tab.editor.redo() if self.current_tab else None)
        edit_menu.addAction(act_redo)

        edit_menu.addSeparator()

        act_cut = QAction("Вырезать", self)
        act_cut.setShortcut(QKeySequence.StandardKey.Cut)
        act_cut.triggered.connect(lambda: self.current_tab.editor.cut() if self.current_tab else None)
        edit_menu.addAction(act_cut)

        act_copy = QAction("Копировать", self)
        act_copy.setShortcut(QKeySequence.StandardKey.Copy)
        act_copy.triggered.connect(lambda: self.current_tab.editor.copy() if self.current_tab else None)
        edit_menu.addAction(act_copy)

        act_paste = QAction("Вставить", self)
        act_paste.setShortcut(QKeySequence.StandardKey.Paste)
        act_paste.triggered.connect(lambda: self.current_tab.editor.paste() if self.current_tab else None)
        edit_menu.addAction(act_paste)

        act_select_all = QAction("Выделить всё", self)
        act_select_all.setShortcut(QKeySequence.StandardKey.SelectAll)
        act_select_all.triggered.connect(lambda: self.current_tab.editor.selectAll() if self.current_tab else None)
        edit_menu.addAction(act_select_all)

        edit_menu.addSeparator()

        act_find = QAction("Поиск и замена...", self)
        act_find.setShortcut(QKeySequence.StandardKey.Find)
        act_find.triggered.connect(self.show_find_replace_dialog)
        edit_menu.addAction(act_find)

        # ── 3. Формат (Требования п. d методички) ─────────────────────────────
        format_menu = mb.addMenu("&Формат")

        act_bold = QAction("Жирный (Bold)", self)
        act_bold.setShortcut(QKeySequence.StandardKey.Bold)
        act_bold.triggered.connect(lambda: self.current_tab.toggle_bold() if self.current_tab else None)
        format_menu.addAction(act_bold)

        act_italic = QAction("Курсив (Italic)", self)
        act_italic.setShortcut(QKeySequence.StandardKey.Italic)
        act_italic.triggered.connect(lambda: self.current_tab.toggle_italic() if self.current_tab else None)
        format_menu.addAction(act_italic)

        act_underline = QAction("Подчёркнутый (Underline)", self)
        act_underline.setShortcut(QKeySequence.StandardKey.Underline)
        act_underline.triggered.connect(lambda: self.current_tab.toggle_underline() if self.current_tab else None)
        format_menu.addAction(act_underline)

        format_menu.addSeparator()

        act_font = QAction("Выбрать шрифт...", self)
        act_font.triggered.connect(lambda: self.current_tab.choose_and_set_font() if self.current_tab else None)
        format_menu.addAction(act_font)

        act_color = QAction("Цвет текста...", self)
        act_color.triggered.connect(lambda: self.current_tab.choose_and_set_color() if self.current_tab else None)
        format_menu.addAction(act_color)

        act_bg_color = QAction("Цвет фона (выделитель)...", self)
        act_bg_color.triggered.connect(self._choose_bg_color)
        format_menu.addAction(act_bg_color)

        format_menu.addSeparator()

        # Выравнивание
        align_left = QAction("По левому краю", self)
        align_left.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignLeft) if self.current_tab else None)
        format_menu.addAction(align_left)

        align_center = QAction("По центру", self)
        align_center.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignHCenter) if self.current_tab else None)
        format_menu.addAction(align_center)

        align_right = QAction("По правому краю", self)
        align_right.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignRight) if self.current_tab else None)
        format_menu.addAction(align_right)

        align_justify = QAction("По ширине", self)
        align_justify.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignJustify) if self.current_tab else None)
        format_menu.addAction(align_justify)

        # ── 4. Справка ────────────────────────────────────────────────────────
        help_menu = mb.addMenu("&Справка")
        act_about = QAction("О программе (Принципы ООП)...", self)
        act_about.triggered.connect(self._show_about_dialog)
        help_menu.addAction(act_about)

    # ──────────────────────────────────────────────────────────────────────────
    # Панель инструментов (ToolBar)
    # ──────────────────────────────────────────────────────────────────────────
    def _build_toolbar(self) -> None:
        tb = QToolBar("Панель форматирования", self)
        tb.setMovable(False)
        self.addToolBar(tb)

        # Файловые кнопки
        act_new = tb.addAction("📄 Новый")
        act_new.setToolTip("Создать вкладку (Ctrl+N)")
        act_new.triggered.connect(self.new_tab)

        act_open = tb.addAction("📂 Открыть")
        act_open.setToolTip("Открыть файл (Ctrl+O)")
        act_open.triggered.connect(self.open_file)

        act_save = tb.addAction("💾 Сохранить")
        act_save.setToolTip("Сохранить файл (Ctrl+S)")
        act_save.triggered.connect(self.save_file)

        tb.addSeparator()

        # Выбор шрифта (только масштабируемые шрифты, исключает системные bitmap-шрифты macOS)
        self._font_combo = QFontComboBox(self)
        self._font_combo.setFontFilters(QFontComboBox.FontFilter.ScalableFonts)
        self._font_combo.setCurrentFont(QFont("Menlo"))
        self._font_combo.currentFontChanged.connect(
            lambda font: self.current_tab.set_font_family(font.family()) if self.current_tab else None
        )
        tb.addWidget(self._font_combo)

        # Размер шрифта
        self._size_spin = QSpinBox(self)
        self._size_spin.setRange(6, 96)
        self._size_spin.setValue(13)
        self._size_spin.setSuffix(" pt")
        self._size_spin.valueChanged.connect(
            lambda val: self.current_tab.set_font_size(float(val)) if self.current_tab else None
        )
        tb.addWidget(self._size_spin)

        tb.addSeparator()

        # Кнопки стилей: B, I, U
        act_bold = tb.addAction("𝗕")
        act_bold.setToolTip("Жирный (Ctrl+B)")
        act_bold.triggered.connect(lambda: self.current_tab.toggle_bold() if self.current_tab else None)

        act_italic = tb.addAction("𝘐")
        act_italic.setToolTip("Курсив (Ctrl+I)")
        act_italic.triggered.connect(lambda: self.current_tab.toggle_italic() if self.current_tab else None)

        act_underline = tb.addAction("U̲")
        act_underline.setToolTip("Подчёркнутый (Ctrl+U)")
        act_underline.triggered.connect(lambda: self.current_tab.toggle_underline() if self.current_tab else None)

        tb.addSeparator()

        # Цвета
        act_color = tb.addAction("🎨 Цвет")
        act_color.setToolTip("Цвет текста")
        act_color.triggered.connect(lambda: self.current_tab.choose_and_set_color() if self.current_tab else None)

        act_highlighter = tb.addAction("🖍 Маркер")
        act_highlighter.setToolTip("Цвет фона выделения")
        act_highlighter.triggered.connect(self._choose_bg_color)

        tb.addSeparator()

        # Выравнивание
        act_left = tb.addAction("⯬")
        act_left.setToolTip("По левому краю")
        act_left.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignLeft) if self.current_tab else None)

        act_center = tb.addAction("⯮⯬")
        act_center.setToolTip("По центру")
        act_center.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignHCenter) if self.current_tab else None)

        act_right = tb.addAction("⯮")
        act_right.setToolTip("По правому краю")
        act_right.triggered.connect(lambda: self.current_tab.set_alignment(Qt.AlignmentFlag.AlignRight) if self.current_tab else None)

        tb.addSeparator()

        # Поиск
        act_find = tb.addAction("🔍 Поиск")
        act_find.setToolTip("Поиск и замена (Ctrl+F)")
        act_find.triggered.connect(self.show_find_replace_dialog)

    # ──────────────────────────────────────────────────────────────────────────
    # Строка состояния (Status Bar)
    # ──────────────────────────────────────────────────────────────────────────
    def _build_status_bar(self) -> None:
        sb = self.statusBar()
        sb.addPermanentWidget(self._status_pos)
        sb.addPermanentWidget(self._status_stats)
        sb.addPermanentWidget(self._status_encoding)
        sb.showMessage("Готово")

    def _update_cursor_status(self, line: int, col: int) -> None:
        self._status_pos.setText(f"Стр {line}, Кол {col}")

    def _update_stats_status(self) -> None:
        tab = self.current_tab
        if tab:
            stats = tab.get_stats()
            self._status_stats.setText(
                f"Символов: {stats['chars']} | Слов: {stats['words']} | Строк: {stats['lines']}"
            )
        else:
            self._status_stats.setText("Символов: 0 | Слов: 0")

    # ──────────────────────────────────────────────────────────────────────────
    # Файловые операции
    # ──────────────────────────────────────────────────────────────────────────
    def open_file(self) -> None:
        """Открыть файл и загрузить его в новую или пустую вкладку."""
        result = self._file_manager.open_file(self)
        if not result:
            return
        path, content = result

        # Если текущая вкладка пустая и безымянная — открываем в ней
        tab = self.current_tab
        if tab and not tab.file_path and not tab.is_modified and not tab.editor.toPlainText():
            tab.file_path = path
            tab.set_content(content)
        else:
            self.new_tab(file_path=path, content=content)

        self.statusBar().showMessage(f"Файл открыт: {path}", 3000)

    def open_file_from_path(self, path: str) -> bool:
        """Открыть файл по заданному пути (из командной строки или drag-and-drop)."""
        content = self._file_manager.open_file_from_path(path, self)
        if content is None:
            return False

        tab = self.current_tab
        if tab and not tab.file_path and not tab.is_modified and not tab.editor.toPlainText():
            tab.file_path = path
            tab.set_content(content)
        else:
            self.new_tab(file_path=path, content=content)

        self.statusBar().showMessage(f"Файл открыт: {path}", 3000)
        return True

    def save_file(self) -> bool:
        """Сохранить текущую вкладку."""
        tab = self.current_tab
        if not tab:
            return False

        if not tab.file_path:
            return self.save_file_as()

        content = tab.get_content()
        success = self._file_manager.save_file(tab.file_path, content, self)
        if success:
            tab.mark_saved(tab.file_path)
            self.statusBar().showMessage(f"Сохранено: {tab.file_path}", 3000)
            return True
        return False

    def save_file_as(self) -> bool:
        """Сохранить текущую вкладку под новым именем."""
        tab = self.current_tab
        if not tab:
            return False

        content = tab.get_content()
        suggested = os.path.basename(tab.file_path) if tab.file_path else "document.html"
        new_path = self._file_manager.save_file_as(content, suggested_name=suggested, parent_widget=self)
        if new_path:
            tab.mark_saved(new_path)
            self.statusBar().showMessage(f"Сохранено как: {new_path}", 3000)
            return True
        return False

    # ──────────────────────────────────────────────────────────────────────────
    # Диалоги
    # ──────────────────────────────────────────────────────────────────────────
    def show_find_replace_dialog(self) -> None:
        """Открыть диалог поиска и замены."""
        tab = self.current_tab
        if not tab:
            return

        if not self._find_dialog:
            self._find_dialog = FindReplaceDialog(tab.editor, self)
        else:
            self._find_dialog.set_editor(tab.editor)

        self._find_dialog.open_with_selection()

    def _choose_bg_color(self) -> None:
        tab = self.current_tab
        if not tab:
            return
        col = QColorDialog.getColor(QColor(255, 255, 0), self, "Выберите цвет маркера")
        if col.isValid():
            tab.set_background_color(col)

    def _show_about_dialog(self) -> None:
        text = (
            "<h3>📘 Лабораторная работа №2: Текстовый редактор (POO Python)</h3>"
            "<p><b>Автор:</b> Студент</p>"
            "<hr/>"
            "<p><b>1. 🔹 Абстракция (Abstraction):</b><br/>"
            "Класс <code>FileManager</code> абстрагирует чтение/запись на диск. "
            "Класс <code>EditorTab</code> абстрагирует форматирование и работу с текстом.</p>"
            "<p><b>2. 🔹 Наследование (Inheritance):</b><br/>"
            "<code>TextEditorWindow extends QMainWindow</code>, "
            "<code>EditorTab extends QWidget</code>, "
            "<code>FindReplaceDialog extends QDialog</code>.</p>"
            "<p><b>3. 🔹 Полиморфизм (Polymorphism):</b><br/>"
            "Единые методы <code>toggle_bold()</code>, <code>get_content()</code> вызываются полиморфно "
            "для любой вкладки независимо от открытого формата (TXT или HTML со стилями).</p>"
            "<p><b>4. 🔹 Инкапсуляция (Encapsulation):</b><br/>"
            "Все компоненты, пути и флаги модификации объявлены приватными (<code>_tab_widget</code>, "
            "<code>_file_path</code>, <code>_is_modified</code>), доступ через свойства и методы.</p>"
        )
        QMessageBox.about(self, "О программе - Принципы ООП", text)

    # ──────────────────────────────────────────────────────────────────────────
    # Перехват закрытия окна (проверка несохранённых файлов)
    # ──────────────────────────────────────────────────────────────────────────
    def closeEvent(self, event) -> None:
        """Перед выходом проверяем все вкладки на наличие несохранённых изменений."""
        count = self._tab_widget.count()
        modified_tabs = [
            self._tab_widget.widget(i)
            for i in range(count)
            if isinstance(self._tab_widget.widget(i), EditorTab) and self._tab_widget.widget(i).is_modified
        ]

        if modified_tabs:
            res = QMessageBox.question(
                self,
                "Выход из программы",
                f"Имеются несохранённые документы ({len(modified_tabs)} шт.).\nВы действительно хотите выйти?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                QMessageBox.StandardButton.No,
            )
            if res != QMessageBox.StandardButton.Yes:
                event.ignore()
                return

        event.accept()
