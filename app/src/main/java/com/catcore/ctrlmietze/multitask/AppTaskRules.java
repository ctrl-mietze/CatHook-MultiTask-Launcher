package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppTaskRules {
    public static final String GLOBAL_RULES = "catcore_multitask_task_rules";
    private static final String PREFS = "multitask_app_task_rules";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private AppTaskRules() {}

    private static SharedPreferences p(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int get(Context context, String pkg) {
        return clamp(p(context).getInt(pkg, 1));
    }

    public static void set(Context context, String pkg, int count) {
        int value = clamp(count);
        if (value <= 1) p(context).edit().remove(pkg).apply();
        else p(context).edit().putInt(pkg, value).apply();
        mirrorToSystem(context.getApplicationContext());
    }

    public static String serialize(Context context) {
        TreeMap<String, Integer> sorted = new TreeMap<>();
        for (Map.Entry<String, ?> e : p(context).getAll().entrySet()) {
            if (!(e.getValue() instanceof Integer)) continue;
            int count = clamp((Integer) e.getValue());
            if (count > 1) sorted.put(e.getKey(), count);
        }

        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Integer> e : sorted.entrySet()) {
            if (out.length() > 0) out.append(';');
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        return out.toString();
    }

    public static void mirrorToSystem(Context context) {
        String rules = serialize(context);
        EXEC.execute(() -> RootShell.run(
                "settings put global " + GLOBAL_RULES + " " + RootShell.quote(rules), 5));
    }

    public static int parseRule(String encoded, String pkg) {
        if (encoded == null || encoded.isEmpty() || pkg == null) return 1;
        for (String entry : encoded.split(";")) {
            int split = entry.lastIndexOf('=');
            if (split <= 0) continue;
            if (!pkg.equals(entry.substring(0, split))) continue;
            try {
                return clamp(Integer.parseInt(entry.substring(split + 1)));
            } catch (Throwable ignored) {
                return 1;
            }
        }
        return 1;
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(8, value));
    }
}
