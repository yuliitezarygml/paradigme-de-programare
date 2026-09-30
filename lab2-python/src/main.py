"""
Точка входа в приложение Текстового редактора (Лабораторная работа 2).
"""
import sys
import os

# Подавление предупреждений CoreText/FreeType на macOS для устаревших растровых шрифтов
os.environ["QT_LOGGING_RULES"] = "qt.qpa.fonts.warning=false"

# Добавляем директорию src в путь поиска модулей
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
if CURRENT_DIR not in sys.path:
    sys.path.insert(0, CURRENT_DIR)

from PyQt6.QtWidgets import QApplication
from PyQt6.QtCore import Qt, qInstallMessageHandler
from main_window import TextEditorWindow


def _qt_message_filter(mode, context, message: str) -> None:
    # Игнорируем предупреждения CoreText для старых системных bitmap-шрифтов macOS (например GB18030)
    if "minimum bearings" in message or "GB18030 Bitmap" in message:
        return
    # Все критические ошибки и другие сообщения выводим в обычном режиме
    sys.stderr.write(f"{message}\n")


def main() -> None:
    # Установка фильтра сообщений шрифтовой подсистемы macOS
    qInstallMessageHandler(_qt_message_filter)

    # Настройки для чёткого отображения на Retina и HiDPI экранах Mac
    app = QApplication(sys.argv)
    app.setApplicationName("Text Editor POO Python")
    app.setOrganizationName("Universitatea")

    # Применение приятного нативного стиля
    app.setStyle("Fusion")

    window = TextEditorWindow()

    # Если через командную строку переданы файлы — открываем их во вкладках
    for arg in sys.argv[1:]:
        if os.path.isfile(arg):
            window.open_file_from_path(arg)

    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
