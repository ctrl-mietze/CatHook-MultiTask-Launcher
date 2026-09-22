package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TaskInspector {
    public static final class TaskInfo {
        public final int taskId;
        public final int displayId;
        public final String packageName;
        public final String label;
        public final int processCount;
        public final long rssBytes;
        public final double cpuPercent;

        TaskInfo(int taskId, int displayId, String packageName, String label,
                 int processCount, long rssBytes, double cpuPercent) {
            this.taskId = taskId;
            this.displayId = displayId;
            this.packageName = packageName;
            this.label = label;
            this.processCount = processCount;
            this.rssBytes = rssBytes;
            this.cpuPercent = cpuPercent;
        }
    }

    private static final Pattern MODERN_TASK = Pattern.compile(
            "^TASK\\s+\\d+:([A-Za-z0-9_.$]+)\\s+id=(\\d+)\\s+userId=\\d+\\s+displayId=(\\d+)");
    private static final Pattern OLD_TASK = Pattern.compile(
            "Task\\{[^}]*#(\\d+)[^}]*\\bA=(?:\\d+:)?([A-Za-z0-9_.$]+)[^}]*");
    private static final Pattern OLD_DISPLAY = Pattern.compile("displayId=(\\d+)");
    private static final Pattern RECORD = Pattern.compile(
            "ActivityRecord\\{[^}]*\\s(?:u\\d+\\s+)?([A-Za-z0-9_.$]+)/[^\\s}]+\\s+t(\\d+)");
    private static final Pattern MODERN_ACTIVITY = Pattern.compile(
            "^ACTIVITY\\s+([A-Za-z0-9_.$]+)/[^\\s]+.*?pid=(\\d+).*?displayId=(\\d+)");

    private TaskInspector() {}

    public static List<TaskInfo> readUserTasks(Context context) {
        RootShell.Result result = RootShell.run("dumpsys activity --verbose activities", 10);
        if (!result.ok && result.output.isEmpty()) {
            result = RootShell.run("dumpsys activity activities", 10);
        }

        Map<Integer, RawTask> tasks = parseTasks(result.output);
        if (tasks.isEmpty()) {
            RootShell.Result fallback = RootShell.run("dumpsys activity activities", 10);
            tasks = parseTasks(fallback.output);
        }

        Set<String> packages = new LinkedHashSet<>();
        for (RawTask raw : tasks.values()) {
            if (raw.packageName != null) packages.add(raw.packageName);
        }

        Map<String, ProcStats> proc = readProcessStats(packages);

        PackageManager pm = context.getPackageManager();
        List<TaskInfo> out = new ArrayList<>();

        for (RawTask raw : tasks.values()) {
            String pkg = raw.packageName;
            if (pkg == null || pkg.equals(context.getPackageName())) continue;

            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (system && !SystemAppAllowlist.isAllowed(pkg)) continue;

                CharSequence labelValue = pm.getApplicationLabel(ai);
                ProcStats stats = proc.get(pkg);
                out.add(new TaskInfo(
                        raw.taskId,
                        raw.displayId,
                        pkg,
                        labelValue == null ? pkg : labelValue.toString(),
                        stats == null ? 0 : stats.processCount,
                        stats == null ? 0L : stats.rssBytes,
                        stats == null ? 0d : stats.cpuPercent));
            } catch (Throwable ignored) {
            }
        }

        return out;
    }

    private static Map<Integer, RawTask> parseTasks(String output) {
        Map<Integer, RawTask> tasks = new LinkedHashMap<>();
        if (output == null) return tasks;

        int currentTask = -1;
        for (String rawLine : output.split("\\r?\\n")) {
            String line = rawLine.trim();

            Matcher modern = MODERN_TASK.matcher(line);
            if (modern.find()) {
                String pkg = modern.group(1);
                int id = safeInt(modern.group(2), -1);
                int display = safeInt(modern.group(3), 0);
                if (id >= 0) {
                    tasks.put(id, new RawTask(id, display, pkg));
                    currentTask = id;
                }
                continue;
            }

            Matcher old = OLD_TASK.matcher(line);
            if (old.find()) {
                int id = safeInt(old.group(1), -1);
                String pkg = old.group(2);
                int display = 0;
                Matcher dm = OLD_DISPLAY.matcher(line);
                if (dm.find()) display = safeInt(dm.group(1), 0);
                if (id >= 0 && pkg != null && pkg.contains(".")) {
                    tasks.put(id, new RawTask(id, display, pkg));
                    currentTask = id;
                }
                continue;
            }

            Matcher record = RECORD.matcher(line);
            if (record.find()) {
                String pkg = record.group(1);
                int taskId = safeInt(record.group(2), -1);
                if (taskId >= 0 && pkg != null && pkg.contains(".")) {
                    RawTask existing = tasks.get(taskId);
                    if (existing == null) {
                        tasks.put(taskId, new RawTask(taskId, 0, pkg));
                    } else if (existing.packageName == null) {
                        existing.packageName = pkg;
                    }
                    currentTask = taskId;
                }
                continue;
            }

            Matcher activity = MODERN_ACTIVITY.matcher(line);
            if (activity.find()) {
                String pkg = activity.group(1);
                int display = safeInt(activity.group(3), 0);
                if (currentTask >= 0) {
                    RawTask existing = tasks.get(currentTask);
                    if (existing != null) {
                        if (existing.packageName == null) existing.packageName = pkg;
                        existing.displayId = display;
                    }
                }
            }
        }

        return tasks;
    }

    private static Map<String, ProcStats> readProcessStats(Set<String> packages) {
        Map<String, ProcStats> out = new LinkedHashMap<>();
        if (packages.isEmpty()) return out;

        RootShell.Result result = RootShell.run(
                "(getconf PAGESIZE 2>/dev/null || echo 4096); "
                        + "ps -A -w -o PID,UID,RSS,PCPU,NAME 2>/dev/null", 8);
        if (!result.ok && result.output.isEmpty()) return out;

        String[] lines = result.output.split("\\r?\\n");
        long pageSize = 4096L;
        int start = 0;
        if (lines.length > 0) {
            try {
                pageSize = Long.parseLong(lines[0].trim());
                start = 1;
            } catch (Throwable ignored) {
            }
        }

        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.toUpperCase(Locale.ROOT).startsWith("PID ")) continue;

            String[] parts = line.split("\\s+", 5);
            if (parts.length < 5) continue;

            long rssPages;
            double cpu;
            try {
                rssPages = Long.parseLong(parts[2]);
            } catch (Throwable t) {
                rssPages = 0L;
            }
            try {
                cpu = Double.parseDouble(parts[3].replace("%", ""));
            } catch (Throwable t) {
                cpu = 0d;
            }

            String process = parts[4];
            for (String pkg : packages) {
                if (!process.equals(pkg) && !process.startsWith(pkg + ":")) continue;

                ProcStats stats = out.get(pkg);
                if (stats == null) {
                    stats = new ProcStats();
                    out.put(pkg, stats);
                }
                stats.processCount++;
                stats.rssBytes += Math.max(0L, rssPages) * Math.max(1024L, pageSize);
                stats.cpuPercent += Math.max(0d, cpu);
                break;
            }
        }

        return out;
    }

    public static Map<String, Integer> countByPackage(List<TaskInfo> tasks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TaskInfo task : tasks) {
            counts.put(task.packageName, counts.getOrDefault(task.packageName, 0) + 1);
        }
        return counts;
    }

    public static boolean removeTask(int taskId) {
        if (taskId < 0) return false;
        RootShell.Result r = RootShell.run("am task remove " + taskId, 6);
        return r.ok;
    }

    private static int safeInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static final class RawTask {
        final int taskId;
        int displayId;
        String packageName;

        RawTask(int taskId, int displayId, String packageName) {
            this.taskId = taskId;
            this.displayId = displayId;
            this.packageName = packageName;
        }
    }

    private static final class ProcStats {
        int processCount;
        long rssBytes;
        double cpuPercent;
    }
}
