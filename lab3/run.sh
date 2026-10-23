#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

# В PATH может быть Python без Tkinter. Проверяем доступные варианты.
for chat_python in "${PYTHON:-python3}" /usr/local/bin/python3 /Library/Frameworks/Python.framework/Versions/Current/bin/python3 /usr/bin/python3; do
    if "$chat_python" -c 'import sys, tkinter; assert sys.version_info >= (3, 10)' >/dev/null 2>&1; then
        echo "Python с Tkinter: $chat_python. Логи будут показаны в этом терминале."
        exec "$chat_python" -u "${1:-run.py}"
    fi
done

echo "Нужен Python 3.10+ с Tkinter. Установите Python с python.org."
echo "На Ubuntu/Debian: sudo apt install python3-tk"
exit 1
