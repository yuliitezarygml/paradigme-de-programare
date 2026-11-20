#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [ "$1" = "test" ]; then
    exec python3 -m unittest tests -v
fi
exec python3 main.py "$@"
