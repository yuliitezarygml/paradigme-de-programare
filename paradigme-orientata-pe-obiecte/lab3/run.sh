#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

# В PATH может быть Python без Tkinter. Проверяем доступные варианты, включая виртуальное окружение.
for chat_python in "${PYTHON:-}" .venv/bin/python3 ../.venv/bin/python3 venv/bin/python3 python3 /usr/local/bin/python3 /Library/Frameworks/Python.framework/Versions/Current/bin/python3 /usr/bin/python3; do
    [ -z "$chat_python" ] && continue
    if [ -x "$chat_python" ] || command -v "$chat_python" >/dev/null 2>&1; then
        if "$chat_python" -c 'import sys, tkinter; assert sys.version_info >= (3, 10)' >/dev/null 2>&1; then
            echo "Python с Tkinter: $chat_python. Логи будут показаны в этом терминале."
            if [ "$#" -gt 0 ]; then
                exec "$chat_python" -u "$@"
            else
                exec "$chat_python" -u run.py
            fi
        fi
    fi
done

echo "Нужен Python 3.10+ с Tkinter. Установите Python с python.org."
echo "На Ubuntu/Debian: sudo apt install python3-tk"
exit 1
