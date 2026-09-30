#!/usr/bin/env bash

# ==============================================================================
# Script Central de Lansare & Testare: Laborator 3 (Rețeaua locală)
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

echo "=========================================================="
echo "  💬 LABORATORUL 3: REȚEAUA LOCALĂ (CLIENT - SERVER CHAT)"
echo "=========================================================="
echo "⚙️  Compilare proiect Java..."
javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/*.java

if [ $? -ne 0 ]; then
    echo "❌ Eroare critică la compilare!"
    exit 1
fi
echo "✅ Compilare finalizată cu succes!"

MODE="$1"

if [ -z "$MODE" ]; then
    echo ""
    echo "Alege modul de rulare:"
    echo "  1) Pornire Server (Panou de Administrare)"
    echo "  2) Pornire Client Chat"
    echo "  3) Pornire Server + 2 Clienți (Demo Complet Instant)"
    echo "  4) Rulare Teste Automate (Validare Cerințe a-e)"
    echo "  5) Ieșire"
    echo ""
    read -p "Opțiunea ta (1-5): " CHOICE
    case "$CHOICE" in
        1) MODE="server" ;;
        2) MODE="client" ;;
        3) MODE="demo" ;;
        4) MODE="test" ;;
        *) echo "Ieșire."; exit 0 ;;
    esac
fi

case "$MODE" in
    server)
        echo "🚀 Lansare Server..."
        java -cp bin ServerMain
        ;;
    client)
        echo "💬 Lansare Client..."
        java -cp bin ClientMain
        ;;
    demo)
        echo "⚡ Lansare Demo Complet (1 Server + 2 Clienți)..."
        java -cp bin ServerMain &
        SERVER_PID=$!
        sleep 1.5
        java -cp bin ClientMain &
        sleep 0.8
        java -cp bin ClientMain &
        echo "Aplicațiile rulează! Apasă Ctrl+C pentru a opri totul."
        wait $SERVER_PID
        ;;
    test)
        echo "🧪 Rulare teste automate..."
        java -cp bin TestNetworkChat
        ;;
    *)
        echo "Opțiune necunoscută: $MODE"
        echo "Utilizare: ./run.sh [server|client|demo|test]"
        exit 1
        ;;
esac
