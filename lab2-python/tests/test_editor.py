"""
Модульные автоматические тесты для Текстового редактора (Lab 2).
Запуск: python3 -m unittest discover tests
или ./run.sh --test
"""
import os
import sys
import unittest
import tempfile

# Добавляем src в sys.path
SRC_DIR = os.path.join(os.path.dirname(__file__), "..", "src")
sys.path.insert(0, SRC_DIR)

from PyQt6.QtWidgets import QApplication
from PyQt6.QtGui import QColor, QFont
from PyQt6.QtCore import Qt

# Создаем QApplication один раз для всех headless-тестов
app = QApplication.instance()
if app is None:
    app = QApplication(["--platform", "offscreen"])

from file_manager import FileManager
from editor_tab import EditorTab
from find_replace_dialog import FindReplaceDialog


class TestFileManager(unittest.TestCase):
    """Тесты файлового менеджера (чтение, запись, обработка путей)."""

    def setUp(self):
        self.fm = FileManager()
        self.temp_dir = tempfile.TemporaryDirectory()

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_save_and_read_plain_text(self):
        file_path = os.path.join(self.temp_dir.name, "test_plain.txt")
        test_content = "Привет, мир!\nВторая строка."

        success = self.fm.save_file(file_path, test_content)
        self.assertTrue(success, "Файл должен успешно сохраниться")

        read_content = self.fm.open_file_from_path(file_path)
        self.assertEqual(read_content, test_content)

    def test_save_and_read_html(self):
        file_path = os.path.join(self.temp_dir.name, "test_formatted.html")
        test_html = "<html><body><p><b>Жирный</b> <i>Курсив</i></p></body></html>"

        success = self.fm.save_file(file_path, test_html)
        self.assertTrue(success)

        read_content = self.fm.open_file_from_path(file_path)
        self.assertIn("Жирный", read_content)
        self.assertIn("Курсив", read_content)

    def test_nonexistent_file_returns_none(self):
        fake_path = os.path.join(self.temp_dir.name, "does_not_exist.txt")
        result = self.fm.open_file_from_path(fake_path)
        self.assertIsNone(result)

    def test_get_filename(self):
        self.assertEqual(self.fm.get_filename("/path/to/my_doc.txt"), "my_doc.txt")
        self.assertEqual(self.fm.get_filename(""), "Новый документ")


class TestEditorTab(unittest.TestCase):
    """Тесты вкладки редактора: стили, модификации, статистика."""

    def test_tab_initialization_plain_text(self):
        tab = EditorTab(file_path="/tmp/sample.txt", content="Hello World")
        self.assertEqual(tab.editor.toPlainText(), "Hello World")
        self.assertEqual(tab.file_path, "/tmp/sample.txt")
        self.assertFalse(tab.is_modified)
        self.assertEqual(tab.title, "sample.txt")

    def test_tab_modified_flag(self):
        tab = EditorTab(content="Initial text")
        self.assertFalse(tab.is_modified)

        # Дописываем текст
        tab.editor.insertPlainText(" added")
        self.assertTrue(tab.is_modified)
        self.assertTrue(tab.title.endswith("*"))

        # Помечаем как сохраненный
        tab.mark_saved("/tmp/saved.txt")
        self.assertFalse(tab.is_modified)
        self.assertFalse(tab.title.endswith("*"))

    def test_tab_statistics(self):
        text = "Раз два три.\nВторая строка текста."
        tab = EditorTab(content=text)
        stats = tab.get_stats()

        self.assertEqual(stats["words"], 6)
        self.assertEqual(stats["lines"], 2)
        self.assertEqual(stats["chars"], len(text))

    def test_formatting_bold_italic_underline(self):
        tab = EditorTab(content="Текст для форматирования")
        tab.editor.selectAll()

        # Проверяем жирный
        tab.toggle_bold()
        html = tab.get_content()
        self.assertTrue(any(w in html for w in ("font-weight:700", "font-weight:600", "font-weight:bold")))

        # Проверяем курсив
        tab.toggle_italic()
        html = tab.get_content()
        self.assertTrue("font-style:italic" in html)

        # Проверяем подчеркивание
        tab.toggle_underline()
        html = tab.get_content()
        self.assertTrue("text-decoration: underline" in html)


class TestFindAndReplace(unittest.TestCase):
    """Тесты поиска и замены подстрок."""

    def setUp(self):
        self.tab = EditorTab(content="яблоко груша яблоко банан яблоко")
        self.dialog = FindReplaceDialog(self.tab.editor)

    def test_find_text(self):
        self.dialog._search_input.setText("груша")
        found = self.dialog._find_next()
        self.assertTrue(found)
        self.assertEqual(self.tab.editor.textCursor().selectedText(), "груша")

    def test_find_not_found(self):
        self.dialog._search_input.setText("апельсин")
        found = self.dialog._find_next()
        self.assertFalse(found)

    def test_replace_all(self):
        self.dialog._search_input.setText("яблоко")
        self.dialog._replace_input.setText("персик")
        self.dialog._replace_all()

        result = self.tab.editor.toPlainText()
        self.assertEqual(result, "персик груша персик банан персик")
        self.assertNotIn("яблоко", result)


if __name__ == "__main__":
    unittest.main()
