package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TaskInspector {
    public static final class TaskInfo {
        public final int taskId;
        public final String packageName;
        public final String label;

        TaskInfo(int taskId, String packageName, String label) {
            this.taskId = taskId;
            this.packageName = packageName;
            this.label = label;
        }
    }

    private static final Pattern TASK =
            Pattern.compile("Task\\{[^#]*#(\\d+).*?(?:A=\\d+:)?([A-Za-z0-9_.$]+)?");
    private static final Pattern REAL_ACTIVITY =
            Pattern.compile("(?:realActivity|mActivityComponent)=?(?:ComponentInfo\\{)?([A-Za-z0-9_.$]+)/");
    private static final Pattern RECORD =
            Pattern.compile("ActivityRecord\\{[^}]*\\s(?:u\\d+\\s+)?([A-Za-z0-9_.$]+)/[^\\s}]+\\s+t(\\d+)");

    private TaskInspector() {}

    public static List<TaskInfo> readUserTasks(Context context) {
        RootShell.Result result = RootShell.run("dumpsys activity activities", 8);
        if (!result.ok && result.output.isEmpty()) return new ArrayList<>();

        Map<Integer, String> tasks = new LinkedHashMap<>();
        int currentTask = -1;

        for (String raw : result.output.split("\\r?\\n")) {
            String line = raw.trim();

            Matcher task = TASK.matcher(line);
            if (line.contains("Task{") && task.find()) {
                try { currentTask = Integer.parseInt(task.group(1)); }
                catch (Throwable ignored) { currentTask = -1; }

                String candidate = task.groupCount() >= 2 ? task.group(2) : null;
                if (currentTask >= 0 && candidate != null && candidate.contains(".")) {
                    tasks.put(currentTask, candidate);
                }
            }

            Matcher real = REAL_ACTIVITY.matcher(line);
            if (currentTask >= 0 && real.find()) {
                tasks.put(currentTask, real.group(1));
            }

            Matcher record = RECORD.matcher(line);
            if (record.find()) {
                try {
                    int taskId = Integer.parseInt(record.group(2));
                    tasks.put(taskId, record.group(1));
                } catch (Throwable ignored) {}
            }
        }

        PackageManager pm = context.getPackageManager();
        List<TaskInfo> out = new ArrayList<>();
        for (Map.Entry<Integer, String> e : tasks.entrySet()) {
            String pkg = e.getValue();
            if (pkg == null || pkg.equals(context.getPackageName())) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                CharSequence label = pm.getApplicationLabel(ai);
                out.add(new TaskInfo(e.getKey(), pkg,
                        label == null ? pkg : label.toString()));
            } catch (Throwable ignored) {}
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
}
