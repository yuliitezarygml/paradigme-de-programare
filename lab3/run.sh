#!/usr/bin/env bash

# ==============================================================================
# Центральный скрипт сборки, запуска и тестирования: Лабораторная работа №3
# Тема: Локальная сеть (Клиент - Сервер чат с комнатами и файлами)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Настройка путей к OpenJDK на macOS (Apple Silicon и Intel)
if [ -d "/opt/homebrew/opt/openjdk/bin" ]; then
    export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"
    export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/usr/local/opt/openjdk/bin" ]; then
    export PATH="/usr/local/opt/openjdk/bin:$PATH"
    export JAVA_HOME="/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
fi

if ! javac -version &> /dev/null; then
    for jdk in /opt/homebrew/Cellar/openjdk/*/bin; do
        if [ -d "$jdk" ]; then
            export PATH="$jdk:$PATH"
            break
        fi
    done
fi

mkdir -p bin downloads history

echo "=========================================================="
echo "  💬 ЛАБОРАТОРНАЯ РАБОТА №3: ЛОКАЛЬНАЯ СЕТЬ (JAVA ЧАТ)"
echo "=========================================================="
echo "⚙️  Компиляция проекта Java..."
javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/*.java

if [ $? -ne 0 ]; then
    echo "❌ Критическая ошибка при компиляции!"
    exit 1
fi
echo "✅ Компиляция успешно завершена!"

MODE="$1"

if [ -z "$MODE" ]; then
    echo ""
    echo "Выберите режим запуска:"
    echo "  1) Запуск Сервера (Панель управления администратора)"
    echo "  2) Запуск Клиента чата"
    echo "  3) Запуск Демо (1 Сервер + 2 Клиента одновременно)"
    echo "  4) Запуск Автоматических тестов (Проверка пунктов a-e)"
    echo "  5) Выход"
    echo ""
    read -p "Ваш выбор (1-5): " CHOICE
    case "$CHOICE" in
        1) MODE="server" ;;
        2) MODE="client" ;;
        3) MODE="demo" ;;
        4) MODE="test" ;;
        *) echo "Выход."; exit 0 ;;
    esac
fi

case "$MODE" in
    server)
        echo "🚀 Запуск Сервера..."
        java -cp bin ServerMain
        ;;
    client)
        echo "💬 Запуск Клиента..."
        java -cp bin ClientMain
        ;;
    demo)
        echo "⚡ Запуск полного Демо (1 Сервер + 2 Клиента)..."
        java -cp bin ServerMain &
        SERVER_PID=$!
        sleep 1.5
        java -cp bin ClientMain &
        sleep 0.8
        java -cp bin ClientMain &
        echo "Приложения запущены! Нажмите Ctrl+C для завершения работы."
        wait $SERVER_PID
        ;;
    test)
        echo "🧪 Запуск автоматических тестов..."
        java -cp bin TestNetworkChat
        ;;
    *)
        echo "Неизвестный параметр: $MODE"
        echo "Использование: ./run.sh [server|client|demo|test]"
        exit 1
        ;;
esac
