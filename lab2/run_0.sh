#!/usr/bin/env bash

# ==============================================================================
# Скрипт сборки и запуска: Текстовый редактор (Лабораторная работа №2 - ООП Java)
# Script de compilare și lansare: Redactor Text (Laboratorul 2)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Настройка путей к Java / OpenJDK (на macOS Apple Silicon и Intel)
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

echo "Используется Java: $(which javac)"
echo "Компиляция файлов..."
mkdir -p bin

javac -encoding UTF-8 -d bin src/service/*.java src/*.java

if [ $? -ne 0 ]; then
    echo "❌ Ошибка при компиляции!"
    exit 1
fi

echo "✅ Компиляция успешно завершена!"
echo "🚀 Запуск текстового редактора..."
java -cp bin Main
