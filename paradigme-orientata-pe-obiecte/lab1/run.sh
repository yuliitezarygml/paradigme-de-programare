#!/usr/bin/env bash

# Скрипт для компиляции и запуска калькулятора (Лабораторная работа 1)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Сначала добавляем пути к Homebrew OpenJDK (на macOS Apple Silicon и Intel)
if [ -d "/opt/homebrew/opt/openjdk/bin" ]; then
    export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"
    export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/usr/local/opt/openjdk/bin" ]; then
    export PATH="/usr/local/opt/openjdk/bin:$PATH"
    export JAVA_HOME="/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
fi

# Проверка альтернативного пути в Cellar, если симлинк в opt отсутствует
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
javac -encoding UTF-8 -d bin src/operations/*.java src/Memory.java src/BaseConverter.java src/Calculator.java src/Main.java src/TestCalculator.java

if [ $? -ne 0 ]; then
    echo "Ошибка при компиляции!"
    exit 1
fi

echo "Компиляция завершена успешно!"
echo "Запуск калькулятора..."
java -cp bin Main
