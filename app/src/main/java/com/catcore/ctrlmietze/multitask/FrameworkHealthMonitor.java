package com.catcore.ctrlmietze.multitask;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class FrameworkHealthMonitor {
    interface StatusSink {
        void onStatus(String status);
    }

    private static final long CHECK_SECONDS = 15L;
    private static final long BAD_GRACE_MS = 20_000L;
    private static final long RECOVERY_COOLDOWN_MS = 75_000L;

    private final Context context;
    private final StatusSink sink;
    private ScheduledExecutorService executor;

    private int badStreak;
    private long badSince;
    private long lastRecovery;

    FrameworkHealthMonitor(Context context, StatusSink sink) {
        this.context = context.getApplicationContext();
        this.sink = sink;
    }

    void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(
                this::check,
                10L,
                CHECK_SECONDS,
                TimeUnit.SECONDS);
    }

    void stop() {
        ScheduledExecutorService current = executor;
        executor = null;
        if (current != null) current.shutdownNow();
    }

    private void check() {
        try {
            Set<String> managed = ManagedSessionRegistry.recent(context);
            if (managed.isEmpty()) {
                resetBadState();
                return;
            }

            ActivityManager manager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            if (manager != null) manager.getMemoryInfo(memory);

            long start = SystemClock.elapsedRealtime();
            RootShell.Result probe = RootShell.run("pidof system_server", 3);
            long probeMs = SystemClock.elapsedRealtime() - start;

            boolean bad = (manager != null && memory.lowMemory)
                    || !probe.ok
                    || probe.output.trim().isEmpty()
                    || probeMs > 2200L;

            if (!bad) {
                resetBadState();
                return;
            }

            badStreak++;
            long now = SystemClock.elapsedRealtime();
            if (badSince == 0L) badSince = now;

            if (badStreak < 3
                    || now - badSince < BAD_GRACE_MS
                    || now - lastRecovery < RECOVERY_COOLDOWN_MS) {
                if (sink != null && badStreak >= 2) {
                    sink.onStatus("Stability Guard is watching system pressure…");
                }
                return;
            }

            int removed = shedManagedExtras(managed);
            lastRecovery = now;
            resetBadState();

            if (sink != null) {
                sink.onStatus(removed > 0
                        ? "Stability Guard recovered " + removed + " managed task"
                                + (removed == 1 ? "" : "s")
                        : "Stability Guard checked managed tasks · no cleanup needed");
            }
        } catch (Throwable ignored) {
            // The guard must never become a new source of instability.
        }
    }

    private int shedManagedExtras(Set<String> managed) {
        List<TaskInspector.TaskInfo> tasks;
        try {
            tasks = TaskInspector.readUserTasks(context);
        } catch (Throwable t) {
            return 0;
        }

        Map<String, List<TaskInspector.TaskInfo>> grouped = new LinkedHashMap<>();
        for (TaskInspector.TaskInfo task : tasks) {
            if (!managed.contains(task.packageName)) continue;
            grouped.computeIfAbsent(task.packageName, k -> new ArrayList<>()).add(task);
        }

        int removed = 0;
        for (Map.Entry<String, List<TaskInspector.TaskInfo>> entry : grouped.entrySet()) {
            List<TaskInspector.TaskInfo> group = entry.getValue();
            if (group.isEmpty()) continue;

            int primaryTaskId = -1;
            for (TaskInspector.TaskInfo task : group) {
                if (task.displayId == 0) {
                    primaryTaskId = task.taskId;
                    break;
                }
            }

            // If these are own-task duplicates on display 0, preserve one.
            if (primaryTaskId < 0 && group.size() > 1) {
                primaryTaskId = group.get(0).taskId;
            }

            for (TaskInspector.TaskInfo task : group) {
                boolean secondaryWindow = task.displayId != 0;
                boolean duplicate = group.size() > 1 && task.taskId != primaryTaskId;
                if (!secondaryWindow && !duplicate) continue;
                if (task.taskId == primaryTaskId && !secondaryWindow) continue;

                if (TaskInspector.removeTask(task.taskId)) removed++;
            }

            if (removed == 0 && group.size() <= 1 && group.get(0).displayId == 0) {
                ManagedSessionRegistry.clear(context, entry.getKey());
            }
        }
        return removed;
    }

    private void resetBadState() {
        badStreak = 0;
        badSince = 0L;
    }
}
