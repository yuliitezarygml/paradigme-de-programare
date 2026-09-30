#!/usr/bin/env bash

# Скрипт сборки и запуска Резидентного приложения (Лабораторная работа 5)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Пути к Java на macOS (Homebrew OpenJDK)
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
echo "Компиляция файлов Лабораторной работы 5..."
mkdir -p bin
javac -encoding UTF-8 -d bin src/*.java

if [ $? -ne 0 ]; then
    echo "Ошибка компиляции!"
    exit 1
fi

echo "Компиляция завершена успешно!"

# Запуск тестов
if [ "$1" == "--test" ]; then
    echo "Запуск автоматических тестов..."
    java -cp bin TestResidentApp
    exit $?
fi

echo "Запуск резидентного приложения (System Tray + Meteo & Valută)..."
java -cp bin Main
