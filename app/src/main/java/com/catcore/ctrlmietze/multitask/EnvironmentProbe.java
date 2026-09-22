package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;

public final class EnvironmentProbe {
    public static final String GLOBAL_HOOK_BOOT =
            "catcore_multitask_system_hook_boot";
    public static final String GLOBAL_HOOK_UPTIME =
            "catcore_multitask_system_hook_uptime";

    private EnvironmentProbe() {}

    public static boolean isXposedActive() {
        return false;
    }

    public static boolean hasRoot() {
        RootShell.Result result = RootShell.run("id -u", 4);
        return result.ok && result.output.trim().equals("0");
    }

    public static boolean isSystemHookActive(Context context) {
        try {
            int currentBoot = Settings.Global.getInt(
                    context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
            int hookBoot = Settings.Global.getInt(
                    context.getContentResolver(), GLOBAL_HOOK_BOOT, -2);
            long hookUptime = Settings.Global.getLong(
                    context.getContentResolver(), GLOBAL_HOOK_UPTIME, -1L);
            long now = SystemClock.elapsedRealtime();
            return currentBoot >= 0
                    && currentBoot == hookBoot
                    && hookUptime >= 0L
                    && hookUptime <= now;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String summary(Context context) {
        return "Root " + (hasRoot() ? "ready" : "missing")
                + " · LSPosed " + (isXposedActive() ? "ready" : "inactive")
                + " · System hook " + (isSystemHookActive(context) ? "ready" : "inactive");
    }
}
