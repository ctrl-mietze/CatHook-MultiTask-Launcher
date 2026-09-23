#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO='deb [trusted=yes] https://raw.githubusercontent.com/ctrl-mietze/CatHook-MultiTask-Launcher/termux-repo/ ./'
LIST="$PREFIX/etc/apt/sources.list.d/cathook-termux.list"

mkdir -p "$(dirname "$LIST")"
printf '%s\n' "$REPO" > "$LIST"
pkg update -y
pkg install -y termux-android-bridge

printf '\n[OK] CatHook Termux Repository ist eingerichtet.\n'
printf '[OK] Zukuenftig genuegt: pkg upgrade\n'
