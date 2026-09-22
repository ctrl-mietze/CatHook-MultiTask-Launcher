package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    public static final int MODE_MY_TASK = 0;
    public static final int MODE_APP_OWN_TASK = 1;

    public static final String METHOD_VIRTUAL_DIRECT = "virtual_direct";
    public static final String METHOD_VIRTUAL_ROOT = "virtual_root";
    public static final String METHOD_FREEFORM = "freeform";
    public static final String METHOD_ROOT_PACKAGE_FULL = "root_package_full";
    public static final String METHOD_ROOT_COMPONENT_FULL = "root_component_full";
    public static final String METHOD_DIRECT_COMPONENT_FULL = "direct_component_full";
    public static final String METHOD_DIRECT_LAUNCH_INTENT = "direct_launch_intent";
    public static final String METHOD_ROOT_COMPONENT_BASIC = "root_component_basic";
    public static final String METHOD_ROOT_PACKAGE_BASIC = "root_package_basic";
    public static final String METHOD_MONKEY = "monkey";

    private static final String PREFS = "multitask_settings";
    private static final String DEV_PREFS = "multitask_developer_methods";

    private static final String K_COMPAT = "compatibility_mode";
    private static final String K_STABILITY = "max_stability";
    private static final String K_CHILD = "child_tasks";
    private static final String K_RESTORE = "restore_runtime";
    private static final String K_MAX_CACHED = "max_cached_processes";
    private static final String K_MAX_PHANTOM = "max_phantom_processes";
    private static final String K_ONBOARDING = "onboarding_complete";
    private static final String K_START_MODE = "start_mode";
    private static final String K_FRAMEWORK = "framework_enabled";
    private static final String K_ROOT_HELPER = "root_helper_enabled";
    private static final String K_ROOT_TASK_START = "root_task_start";
    private static final String K_SESSION_PRIORITY = "session_priority";
    private static final String K_ROOT_TELEMETRY = "root_telemetry";
    private static final String K_AUTO_TRIM = "auto_trim";
    private static final String K_DEV_ACCEPTED = "developer_warning_accepted";
    private static final String K_RESTORE_WINDOWS = "restore_windows";
    private static final String K_FULL_SCAN = "compat_full_scan";
    private static final String K_MANUAL_PROCESS_TUNING = "manual_process_tuning";
    private static final String K_BG_PROCESS_LIMIT = "background_process_limit";
    private static final String K_EMPTY_PROCESS_PERCENT = "empty_process_percent";
    private static final String K_LEGACY_EASY = "legacy_easy_mode";

    private SettingsStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences dev(Context c) {
        return c.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE);
    }

    public static boolean compatibilityMode(Context c) { return p(c).getBoolean(K_COMPAT, false); }
    public static boolean maxStability(Context c) { return p(c).getBoolean(K_STABILITY, false); }
    public static boolean childTasks(Context c) { return p(c).getBoolean(K_CHILD, false); }
    public static boolean restoreRuntime(Context c) { return p(c).getBoolean(K_RESTORE, true); }
    public static int maxCachedProcesses(Context c) { return p(c).getInt(K_MAX_CACHED, 0); }
    public static int maxPhantomProcesses(Context c) { return p(c).getInt(K_MAX_PHANTOM, 0); }
    public static boolean onboardingComplete(Context c) { return p(c).getBoolean(K_ONBOARDING, false); }
    public static int startMode(Context c) { return p(c).getInt(K_START_MODE, MODE_MY_TASK); }
    public static boolean frameworkEnabled(Context c) { return p(c).getBoolean(K_FRAMEWORK, true); }
    public static boolean rootHelperEnabled(Context c) { return p(c).getBoolean(K_ROOT_HELPER, false); }
    public static boolean rootTaskStart(Context c) { return p(c).getBoolean(K_ROOT_TASK_START, false); }
    public static boolean sessionPriority(Context c) { return p(c).getBoolean(K_SESSION_PRIORITY, false); }
    public static boolean rootTelemetry(Context c) { return p(c).getBoolean(K_ROOT_TELEMETRY, true); }
    public static boolean autoTrim(Context c) { return p(c).getBoolean(K_AUTO_TRIM, false); }
    public static boolean developerWarningAccepted(Context c) { return p(c).getBoolean(K_DEV_ACCEPTED, false); }
    public static boolean restoreWindows(Context c) { return p(c).getBoolean(K_RESTORE_WINDOWS, true); }
    public static boolean fullScanMode(Context c) { return p(c).getBoolean(K_FULL_SCAN, false); }
    public static boolean manualProcessTuning(Context c) { return p(c).getBoolean(K_MANUAL_PROCESS_TUNING, false); }
    public static int backgroundProcessLimit(Context c) { return p(c).getInt(K_BG_PROCESS_LIMIT, 0); }
    public static int emptyProcessPercent(Context c) { return p(c).getInt(K_EMPTY_PROCESS_PERCENT, 0); }
    public static boolean legacyEasyMode(Context c) { return p(c).getBoolean(K_LEGACY_EASY, false); }

    public static void setCompatibilityMode(Context c, boolean value) { put(c, K_COMPAT, value); }
    public static void setMaxStability(Context c, boolean value) { put(c, K_STABILITY, value); }
    public static void setChildTasks(Context c, boolean value) { put(c, K_CHILD, value); }
    public static void setRestoreRuntime(Context c, boolean value) { put(c, K_RESTORE, value); }
    public static void setOnboardingComplete(Context c, boolean value) { put(c, K_ONBOARDING, value); }
    public static void setFrameworkEnabled(Context c, boolean value) { put(c, K_FRAMEWORK, value); }
    public static void setRootHelperEnabled(Context c, boolean value) { put(c, K_ROOT_HELPER, value); }
    public static void setRootTaskStart(Context c, boolean value) { put(c, K_ROOT_TASK_START, value); }
    public static void setSessionPriority(Context c, boolean value) { put(c, K_SESSION_PRIORITY, value); }
    public static void setRootTelemetry(Context c, boolean value) { put(c, K_ROOT_TELEMETRY, value); }
    public static void setAutoTrim(Context c, boolean value) { put(c, K_AUTO_TRIM, value); }
    public static void setDeveloperWarningAccepted(Context c, boolean value) { put(c, K_DEV_ACCEPTED, value); }
    public static void setRestoreWindows(Context c, boolean value) { put(c, K_RESTORE_WINDOWS, value); }
    public static void setFullScanMode(Context c, boolean value) { put(c, K_FULL_SCAN, value); }
    public static void setManualProcessTuning(Context c, boolean value) { put(c, K_MANUAL_PROCESS_TUNING, value); }
    public static void setBackgroundProcessLimit(Context c, int value) {
        p(c).edit().putInt(K_BG_PROCESS_LIMIT, clamp(value, 0, 64)).apply();
    }
    public static void setLegacyEasyMode(Context c, boolean value) {
        put(c, K_LEGACY_EASY, value);
        if (value) setStartMode(c, MODE_APP_OWN_TASK);
    }
    public static void setEmptyProcessPercent(Context c, int value) {
        p(c).edit().putInt(K_EMPTY_PROCESS_PERCENT, clamp(value, 0, 100)).apply();
    }

    public static void setStartMode(Context c, int mode) {
        p(c).edit().putInt(K_START_MODE,
                mode == MODE_APP_OWN_TASK ? MODE_APP_OWN_TASK : MODE_MY_TASK).apply();
    }

    public static void setMaxCachedProcesses(Context c, int value) {
        p(c).edit().putInt(K_MAX_CACHED, clamp(value, 0, 512)).apply();
    }

    public static void setMaxPhantomProcesses(Context c, int value) {
        p(c).edit().putInt(K_MAX_PHANTOM, clamp(value, 0, 128)).apply();
    }

    public static boolean methodEnabled(Context c, String method) {
        return dev(c).getBoolean(method, true);
    }

    public static void setMethodEnabled(Context c, String method, boolean enabled) {
        dev(c).edit().putBoolean(method, enabled).apply();
    }

    public static SharedPreferences launchCache(Context c) {
        return c.getSharedPreferences("launcher_strategy_cache", Context.MODE_PRIVATE);
    }

    private static void put(Context c, String key, boolean value) {
        p(c).edit().putBoolean(key, value).apply();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
