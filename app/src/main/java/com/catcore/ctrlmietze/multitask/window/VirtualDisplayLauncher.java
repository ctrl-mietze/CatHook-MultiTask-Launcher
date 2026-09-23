package com.catcore.ctrlmietze.multitask.window;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import com.catcore.ctrlmietze.multitask.RootShell;
import com.catcore.ctrlmietze.multitask.SettingsStore;
import com.catcore.ctrlmietze.multitask.TaskLauncher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class VirtualDisplayLauncher {
    interface Callback {
        void onResult(boolean ok, String message);
    }

    private static final ExecutorService EXEC = Executors.newCachedThreadPool();

    private VirtualDisplayLauncher() {}

    static void launch(Activity host, int displayId, String pkg,
                       String activityName, Callback callback) {
        Intent intent = resolveIntent(host, pkg, activityName);
        if (intent == null) {
            callback.onResult(false, "No launchable activity was found for " + pkg + ".");
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS);
        intent.putExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, true);

        String directReason = "Disabled in Developer Options.";
        if (SettingsStore.methodEnabled(host, SettingsStore.METHOD_VIRTUAL_DIRECT)) {
            try {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(displayId);
                host.startActivity(intent, options.toBundle());
                callback.onResult(true, "Started on virtual display " + displayId + ".");
                return;
            } catch (Throwable directError) {
                directReason = reason(directError);
            }
        }

        final String directFailure = directReason;
        if (!SettingsStore.methodEnabled(host, SettingsStore.METHOD_VIRTUAL_ROOT)) {
            callback.onResult(false,
                    "Direct display launch: " + directFailure
                            + "\nRoot display launch: disabled in Developer Options.");
            return;
        }

        EXEC.execute(() -> {
                String component = intent.getComponent() == null
                        ? "" : intent.getComponent().flattenToString();
                String command;
                if (!component.isEmpty()) {
                    command = "am start --user current --display " + displayId
                            + " --activity-new-task --activity-multiple-task"
                            + " -n " + RootShell.quote(component)
                            + " --ez " + TaskLauncher.EXTRA_FORCE_MULTITASK + " true";
                } else {
                    command = "am start --user current --display " + displayId
                            + " --activity-new-task --activity-multiple-task"
                            + " -a android.intent.action.MAIN"
                            + " -c android.intent.category.LAUNCHER"
                            + " -p " + RootShell.quote(pkg)
                            + " --ez " + TaskLauncher.EXTRA_FORCE_MULTITASK + " true";
                }

                RootShell.Result result = RootShell.run(command, 8);
                String message = result.ok
                        ? "Started through the root display launcher."
                        : "Direct display launch: " + directFailure
                        + "\nRoot display launch: " + RootShell.shortReason(result);

            new Handler(Looper.getMainLooper()).post(
                    () -> callback.onResult(result.ok, message));
        });
    }

    private static Intent resolveIntent(Activity host, String pkg, String activityName) {
        PackageManager pm = host.getPackageManager();

        if (activityName != null && !activityName.trim().isEmpty()) {
            Intent explicit = new Intent();
            explicit.setComponent(new ComponentName(pkg, activityName));
            return explicit;
        }

        try {
            return pm.getLaunchIntentForPackage(pkg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String reason(Throwable t) {
        String message = t.getMessage();
        return t.getClass().getSimpleName()
                + (message == null ? "" : ": " + message);
    }
}
