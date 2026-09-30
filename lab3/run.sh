#!/usr/bin/env bash

# Скрипт сборки и запуска Сетевого Чата (Лабораторная работа 3)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Добавляем пути к Java на macOS (Homebrew OpenJDK)
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
javac -encoding UTF-8 -d bin src/*.java

if [ $? -ne 0 ]; then
    echo "Ошибка при компиляции!"
    exit 1
fi

echo "Компиляция завершена успешно!"
echo "Запуск сетевого чата..."
java -cp bin Main
