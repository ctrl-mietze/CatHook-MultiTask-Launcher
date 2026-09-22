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
        Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            Result result = applyBlocking(app);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onDone(result.ok, result.message));
            }
        });
    }

    public static Result applyBlocking(Context context) {
        int cached = SettingsStore.maxCachedProcesses(context);
        int phantom = SettingsStore.maxPhantomProcesses(context);

        StringBuilder message = new StringBuilder();
        boolean ok = true;

        if (cached > 0) {
            RootShell.Result r = RootShell.run(
                    "device_config put activity_manager max_cached_processes " + cached, 5);
            ok &= r.ok;
            message.append("Cached processes: ").append(r.ok ? cached : RootShell.shortReason(r)).append('\n');
        } else {
            RootShell.Result r = RootShell.run(
                    "device_config delete activity_manager max_cached_processes", 5);
            ok &= r.ok || r.code == 0;
            message.append("Cached processes: system default").append('\n');
        }

        if (phantom > 0) {
            RootShell.Result r = RootShell.run(
                    "device_config put activity_manager max_phantom_processes " + phantom, 5);
            ok &= r.ok;
            message.append("Phantom processes: ").append(r.ok ? phantom : RootShell.shortReason(r)).append('\n');
        } else {
            RootShell.Result r = RootShell.run(
                    "device_config delete activity_manager max_phantom_processes", 5);
            ok &= r.ok || r.code == 0;
            message.append("Phantom processes: system default");
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
