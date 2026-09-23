package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RuntimeTuning {
    public interface Callback {
        void onDone(boolean ok, String message);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private RuntimeTuning() {}

    public static void applyAsync(Context context, Callback callback) {
        applyAsync(context, false, callback);
    }

    public static void applyAsync(Context context, boolean resetDefaults, Callback callback) {
        Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            Result result = applyBlocking(app, resetDefaults);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onDone(result.ok, result.message));
            }
        });
    }

    public static Result applyBlocking(Context context) {
        return applyBlocking(context, false);
    }

    public static Result applyBlocking(Context context, boolean resetDefaults) {
        int cached = SettingsStore.maxCachedProcesses(context);
        int phantom = SettingsStore.maxPhantomProcesses(context);
        int background = SettingsStore.backgroundProcessLimit(context);
        int emptyPercent = SettingsStore.emptyProcessPercent(context);
        boolean manual = SettingsStore.manualProcessTuning(context);

        if (!manual) {
            // Framework-managed mode: remove only CatCore's runtime overrides.
            RootShell.run("device_config delete activity_manager max_cached_processes", 5);
            RootShell.run("device_config delete activity_manager max_phantom_processes", 5);
            RootShell.run("device_config delete activity_manager max_empty_time_millis", 5);
            RootShell.run("settings delete global activity_manager_constants", 5);
            return new Result(true, "Process runtime is managed by CatCore Framework.");
        }

        StringBuilder message = new StringBuilder();
        boolean ok = true;

        if (cached > 0) {
            RootShell.Result r = RootShell.run(
                    "device_config put activity_manager max_cached_processes " + cached, 5);
            ok &= r.ok;
            message.append("Cached processes: ").append(r.ok ? cached : RootShell.shortReason(r)).append('\n');
        } else if (resetDefaults) {
            RootShell.Result r = RootShell.run(
                    "device_config delete activity_manager max_cached_processes", 5);
            ok &= r.ok || r.code == 0;
            message.append("Cached processes: system default").append('\n');
        } else {
            message.append("Cached processes: system default").append('\n');
        }

        if (phantom > 0) {
            RootShell.Result r = RootShell.run(
                    "device_config put activity_manager max_phantom_processes " + phantom, 5);
            ok &= r.ok;
            message.append("Phantom processes: ").append(r.ok ? phantom : RootShell.shortReason(r));
        } else if (resetDefaults) {
            RootShell.Result r = RootShell.run(
                    "device_config delete activity_manager max_phantom_processes", 5);
            ok &= r.ok || r.code == 0;
            message.append("Phantom processes: system default");
        } else {
            message.append("Phantom processes: system default");
        }

        if (background > 0) {
            RootShell.Result r = RootShell.run(
                    "settings put global activity_manager_constants max_cached_processes="
                            + background, 5);
            ok &= r.ok;
            message.append('\n').append("Background process target: ")
                    .append(r.ok ? background : RootShell.shortReason(r));
        }

        if (emptyPercent > 0) {
            // Exposed as a CatCore policy value. OEM ActivityManager implementations differ,
            // so the framework consumes this value conservatively instead of patching files.
            message.append('\n').append("Empty-process reserve: ")
                    .append(emptyPercent).append("% (framework policy)");
        }

        return new Result(ok, message.toString().trim());
    }

    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }
}
