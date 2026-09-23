#!/system/bin/sh
MODDIR=${0%/*}
STATE=/data/adb/catcore_multitask

mkdir -p "$STATE/sessions"
chmod 0700 "$STATE" "$STATE/sessions" 2>/dev/null
"$MODDIR/bin/catcore-helper" restore-all >/dev/null 2>&1
echo "ready" > "$STATE/status"
