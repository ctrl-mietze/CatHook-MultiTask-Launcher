package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    private static final String PREFS = "multitask_settings";
    private static final String K_COMPAT = "compatibility_mode";
    private static final String K_STABILITY = "max_stability";
    private static final String K_CHILD = "child_tasks";
    private static final String K_RESTORE = "restore_runtime";
    private static final String K_MAX_CACHED = "max_cached_processes";
    private static final String K_MAX_PHANTOM = "max_phantom_processes";

    private SettingsStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean compatibilityMode(Context c) { return p(c).getBoolean(K_COMPAT, false); }
    public static boolean maxStability(Context c) { return p(c).getBoolean(K_STABILITY, false); }
    public static boolean childTasks(Context c) { return p(c).getBoolean(K_CHILD, false); }
    public static boolean restoreRuntime(Context c) { return p(c).getBoolean(K_RESTORE, true); }
    public static int maxCachedProcesses(Context c) { return p(c).getInt(K_MAX_CACHED, 0); }
    public static int maxPhantomProcesses(Context c) { return p(c).getInt(K_MAX_PHANTOM, 0); }

    public static void save(Context c, boolean compat, boolean stability, boolean child,
                            boolean restore, int maxCached, int maxPhantom) {
        p(c).edit()
                .putBoolean(K_COMPAT, compat)
                .putBoolean(K_STABILITY, stability)
                .putBoolean(K_CHILD, child)
                .putBoolean(K_RESTORE, restore)
                .putInt(K_MAX_CACHED, clamp(maxCached, 0, 512))
                .putInt(K_MAX_PHANTOM, clamp(maxPhantom, 0, 128))
                .apply();
    }

    public static SharedPreferences launchCache(Context c) {
        return c.getSharedPreferences("launcher_strategy_cache", Context.MODE_PRIVATE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
