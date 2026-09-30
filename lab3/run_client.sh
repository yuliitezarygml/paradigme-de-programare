#!/usr/bin/env bash

# ==============================================================================
# Lansare: Client Chat Rețea Locală (Laboratorul 3)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Configurare cale către OpenJDK pe macOS (Apple Silicon / Intel)
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

echo "⚙️  Compilare surse Java..."
javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/*.java

if [ $? -ne 0 ]; then
    echo "❌ Eroare la compilare!"
    exit 1
fi

echo "✅ Compilare reușită!"
echo "💬 Pornire Client Chat Local..."
java -cp bin ClientMain
