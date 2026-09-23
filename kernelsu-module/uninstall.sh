#!/system/bin/sh
MODDIR=${0%/*}

"$MODDIR/bin/catcore-helper" restore-all >/dev/null 2>&1
rm -rf /data/adb/catcore_multitask
