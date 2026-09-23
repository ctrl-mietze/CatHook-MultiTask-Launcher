package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SystemTaskBridge {
    public static final String ACTION =
            "com.catcore.ctrlmietze.multitask.action.SYSTEM_TASK";
    public static final int RESULT_OK = 1;
    public static final int RESULT_ERROR = -1;
    private static final Set<String> ACTIVE_PACKAGES =
            ConcurrentHashMap.newKeySet();

    public interface Callback {
        void onResult(boolean ok, int before, int after, int desired, String message);
    }

    private SystemTaskBridge() {}

    public static void ensureTaskCount(
            Context context,
            String packageName,
            String activityName,
            int desired,
            Callback callback) {
        Context app = context.getApplicationContext();
        int target = Math.max(1, Math.min(8, desired));

        if (packageName == null || packageName.trim().isEmpty()) {
            if (callback != null) {
                callback.onResult(false, -1, -1, target, "No package name was supplied.");
            }
            return;
        }

        if (!ACTIVE_PACKAGES.add(packageName)) {
            if (callback != null) {
                callback.onResult(false, -1, -1, target,
                        "A task request for this app is already running.");
            }
            return;
        }
        Handler main = new Handler(Looper.getMainLooper());
        AtomicBoolean finished = new AtomicBoolean(false);

        ResultReceiver receiver = new ResultReceiver(main) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (!finished.compareAndSet(false, true)) return;
                ACTIVE_PACKAGES.remove(packageName);
                int before = resultData == null ? -1 : resultData.getInt("before", -1);
                int after = resultData == null ? -1 : resultData.getInt("after", -1);
                int wanted = resultData == null ? target : resultData.getInt("desired", target);
                String message = resultData == null
                        ? "No result details."
                        : resultData.getString("message", "No result details.");
                boolean ok = resultCode == RESULT_OK;
                if (!ok && rootModuleTaskStartEnabled(app)) {
                    runRootModuleFallback(app, packageName, wanted, before, callback, message);
                } else if (callback != null) {
                    callback.onResult(ok, before, after, wanted, message);
                }
            }
        };

        Intent request = new Intent(ACTION)
                .putExtra("request_id", UUID.randomUUID().toString())
                .putExtra("package", packageName)
                .putExtra("activity", activityName == null ? "" : activityName)
                .putExtra("desired", target)
                .putExtra("user_id", Math.max(0, android.os.Process.myUid() / 100000))
                .putExtra("result", receiver)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        try {
            app.sendBroadcast(request);
        } catch (Throwable t) {
            ACTIVE_PACKAGES.remove(packageName);
            finished.set(true);
            if (callback != null) {
                callback.onResult(false, -1, -1, target,
                        t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            return;
        }

        main.postDelayed(() -> {
            if (!finished.compareAndSet(false, true)) return;
            ACTIVE_PACKAGES.remove(packageName);
            if (rootModuleTaskStartEnabled(app)) {
                runRootModuleFallback(app, packageName, target, -1, callback,
                        "LSPosed bridge timeout; switching to Root Module fallback.");
            } else if (callback != null) {
                callback.onResult(false, -1, -1, target,
                        "The LSPosed system task bridge did not answer within 7 seconds.");
            }
        }, 7000L);
    }
    private static boolean rootModuleTaskStartEnabled(Context context) {
        return SettingsStore.rootHelperEnabled(context)
                && SettingsStore.rootTaskStart(context)
                && RootPluginManager.isInstalled();
    }

    private static void runRootModuleFallback(
            Context context, String pkg, int desired, int observedBefore,
            Callback callback, String bridgeReason) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int before = observedBefore >= 0
                    ? observedBefore : TaskInspector.countTasksForPackage(pkg);
            int current = Math.max(0, before);
            String last = bridgeReason == null ? "" : bridgeReason;

            while (current < desired) {
                RootPluginManager.Result r = RootPluginManager.run("launch", pkg);
                last = r.message;
                if (!r.ok) break;
                try { Thread.sleep(180L); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                int seen = TaskInspector.countTasksForPackage(pkg);
                if (seen >= 0) current = seen;
                else current++;
            }

            int after = TaskInspector.countTasksForPackage(pkg);
            boolean ok = after >= desired || (after < 0 && current >= desired);
            final String finalLast = last;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    callback.onResult(ok, before, after, desired,
                            ok
                                    ? "Root Module created the requested Android task target ("
                                        + before + " → " + after + "). Target app permissions remain unchanged."
                                    : "Root Module fallback could not reach " + desired
                                        + " tasks. " + finalLast);
                }
            });
        });
    }

}
