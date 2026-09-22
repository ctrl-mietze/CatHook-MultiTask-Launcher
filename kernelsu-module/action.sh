#!/system/bin/sh
MODDIR=${0%/*}

echo "CatCore MultiTask Root Helper"
echo
"$MODDIR/bin/catcore-helper" status
echo
echo "Any stale session priority state will now be restored."
"$MODDIR/bin/catcore-helper" restore-all
