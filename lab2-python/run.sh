#!/usr/bin/env bash

# Скрипт запуска и тестирования Текстового редактора (Lab 2 Python)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if ! command -v python3 &> /dev/null; then
    echo "❌ Ошибка: python3 не найден в системе!"
    exit 1
fi

# Проверка наличия PyQt6
if ! python3 -c "import PyQt6" &> /dev/null; then
    echo "📦 Установка зависимостей (PyQt6)..."
    pip3 install -r requirements.txt
    if [ $? -ne 0 ]; then
        echo "❌ Ошибка установки PyQt6!"
        exit 1
    fi
fi

if [ "$1" == "--test" ]; then
    echo "🧪 Запуск модульных тестов..."
    python3 -m unittest -v tests/test_editor.py
    exit $?
fi

echo "🚀 Запуск Текстового редактора..."
python3 src/main.py "$@"
