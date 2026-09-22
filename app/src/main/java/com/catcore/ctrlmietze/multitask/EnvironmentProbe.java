package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;

public final class EnvironmentProbe {
    public static final String GLOBAL_HOOK_BOOT =
            "catcore_multitask_system_hook_boot";
    public static final String GLOBAL_HOOK_UPTIME =
            "catcore_multitask_system_hook_uptime";
    public static final String GLOBAL_HOOK_PROTOCOL =
            "catcore_multitask_system_hook_protocol";
    public static final String GLOBAL_HOOK_PID =
            "catcore_multitask_system_hook_pid";
    public static final int CURRENT_HOOK_PROTOCOL = 3;

    private static volatile long lastPidCheckAt;
    private static volatile int lastSystemServerPid = -1;

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
            int hookProtocol = Settings.Global.getInt(
                    context.getContentResolver(), GLOBAL_HOOK_PROTOCOL, -1);
            int hookPid = Settings.Global.getInt(
                    context.getContentResolver(), GLOBAL_HOOK_PID, -1);
            int liveSystemPid = currentSystemServerPid();
            long now = SystemClock.elapsedRealtime();
            return currentBoot >= 0
                    && currentBoot == hookBoot
                    && hookProtocol == CURRENT_HOOK_PROTOCOL
                    && hookPid > 0
                    && hookPid == liveSystemPid
                    && hookUptime >= 0L
                    && hookUptime <= now
                    && now - hookUptime <= 5 * 60_000L;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int currentSystemServerPid() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastPidCheckAt < 2000L && lastSystemServerPid > 0) {
            return lastSystemServerPid;
        }

        RootShell.Result result = RootShell.run("pidof system_server", 3);
        int pid = -1;
        if (result.ok) {
            String value = result.output.trim();
            if (!value.isEmpty()) {
                String first = value.split("\\s+")[0];
                try {
                    pid = Integer.parseInt(first);
                } catch (Throwable ignored) {
                    pid = -1;
                }
            }
        }

        lastSystemServerPid = pid;
        lastPidCheckAt = now;
        return pid;
    }

    public static int systemHookProtocol(Context context) {
        try {
            return Settings.Global.getInt(
                    context.getContentResolver(), GLOBAL_HOOK_PROTOCOL, -1);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static String summary(Context context) {
        return "Root " + (hasRoot() ? "ready" : "missing")
                + " · LSPosed " + (isXposedActive() ? "ready" : "inactive")
                + " · System hook " + (isSystemHookActive(context) ? "ready" : "inactive");
    }
}
