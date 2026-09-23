#!/system/bin/sh

ui_print "***************************************"
ui_print " CatCore MultiTask Root Helper"
ui_print " V2 development helper"
ui_print "***************************************"
ui_print "- No system partition files are mounted"
ui_print "- Installing temporary task/session broker"

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/bin/catcore-helper" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/boot-completed.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
