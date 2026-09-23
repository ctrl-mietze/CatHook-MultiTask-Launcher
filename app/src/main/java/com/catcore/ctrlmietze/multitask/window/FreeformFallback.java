package com.catcore.ctrlmietze.multitask.window;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import com.catcore.ctrlmietze.multitask.RootShell;
import com.catcore.ctrlmietze.multitask.SettingsStore;
import com.catcore.ctrlmietze.multitask.TaskLauncher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class FreeformFallback {
    interface Callback {
        void onResult(boolean ok, String message);
    }

    private static final ExecutorService EXEC = Executors.newCachedThreadPool();

    private FreeformFallback() {}

    static void open(Activity host, String pkg, String activityName, Callback callback) {
        if (!SettingsStore.methodEnabled(host, SettingsStore.METHOD_FREEFORM)) {
            callback.onResult(false, "Android freeform fallback is disabled in Developer Options.");
            return;
        }

        int width = host.getResources().getDisplayMetrics().widthPixels;
        int height = host.getResources().getDisplayMetrics().heightPixels;

        int marginX = Math.max(24, width / 12);
        int top = Math.max(80, height / 10);
        Rect bounds = new Rect(
                marginX,
                top,
                width - marginX,
                Math.max(top + 240, height - Math.max(80, height / 8)));

        EXEC.execute(() -> {
            StringBuilder command = new StringBuilder();
            command.append("am start --user current")
                    .append(" --windowingMode 5")
                    .append(" --bounds ")
                    .append(bounds.left).append(",")
                    .append(bounds.top).append(",")
                    .append(bounds.right).append(",")
                    .append(bounds.bottom)
                    .append(" --activity-new-task --activity-multiple-task");

            if (activityName != null && !activityName.trim().isEmpty()) {
                String component = activityName.contains("/")
                        ? activityName
                        : pkg + "/" + activityName;
                command.append(" -n ").append(RootShell.quote(component));
            } else {
                command.append(" -a android.intent.action.MAIN")
                        .append(" -c android.intent.category.LAUNCHER")
                        .append(" -p ").append(RootShell.quote(pkg));
            }

            command.append(" --ez ")
                    .append(TaskLauncher.EXTRA_FORCE_MULTITASK)
                    .append(" true");

            RootShell.Result result = RootShell.run(command.toString(), 8);
            String message = result.ok
                    ? "Opened using Android freeform as the V2 fallback."
                    : RootShell.shortReason(result);

            new Handler(Looper.getMainLooper()).post(
                    () -> callback.onResult(result.ok, message));
        });
    }
}
