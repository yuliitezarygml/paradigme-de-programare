#!/usr/bin/env bash

# ==============================================================================
# Скрипт сборки и запуска: Сервер локального чата (Лабораторная работа №3)
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

echo "⚙️  Компиляция исходных файлов Java..."
javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/*.java

if [ $? -ne 0 ]; then
    echo "❌ Ошибка при компиляции!"
    exit 1
fi

echo "✅ Компиляция успешно завершена!"
echo "🚀 Запуск Сервера локального чата..."
java -cp bin ServerMain
