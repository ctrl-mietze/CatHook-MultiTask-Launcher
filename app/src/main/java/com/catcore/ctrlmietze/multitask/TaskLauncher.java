package com.catcore.ctrlmietze.multitask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TaskLauncher {
    public static final String EXTRA_FORCE_MULTITASK =
            "com.catcore.ctrlmietze.multitask.FORCE_MULTITASK";
    public static final String EXTRA_MAX_STABILITY =
            "com.catcore.ctrlmietze.multitask.MAX_STABILITY";
    public static final String EXTRA_RULE_SPAWN =
            "com.catcore.ctrlmietze.multitask.RULE_SPAWN";

    public interface Callback {
        void onResult(boolean ok, String message);
    }

    public interface Progress {
        void onProgress(String message);
    }

    public interface MultiCallback {
        void onResult(int requested, int started, String message);
    }

    private static final ExecutorService EXEC = Executors.newCachedThreadPool();

    private TaskLauncher() {}

    public static void launchNewTask(Context context, String pkg, String preferredActivity,
                                     Callback callback) {
        launchNewTask(context, pkg, preferredActivity, null, callback);
    }

    public static void launchNewTask(Context context, String pkg, String preferredActivity,
                                     Progress progress, Callback callback) {
        Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            LaunchResult result = launchBlocking(app, pkg, preferredActivity, progress);
            post(app, callback, result.ok, result.message);
        });
    }

    public static void launchMultiple(Context context, String pkg, String preferredActivity,
                                      int requested, Progress progress, MultiCallback callback) {
        Context app = context.getApplicationContext();
        int count = Math.max(1, Math.min(8, requested));
        EXEC.execute(() -> {
            int success = 0;
            String last = "";
            for (int i = 0; i < count; i++) {
                progress(app, progress, "Opening task " + (i + 1) + " of " + count + "…");
                LaunchResult r = launchBlocking(app, pkg, preferredActivity, null);
                last = r.message;
                if (!r.ok) break;
                success++;
                try { Thread.sleep(140L); } catch (InterruptedException ignored) {}
            }
            int done = success;
            String message = done == count
                    ? "Opened " + done + " task" + (done == 1 ? "" : "s") + "."
                    : "Opened " + done + " of " + count + ". " + last;
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onResult(count, done, message));
            }
        });
    }

    private static LaunchResult launchBlocking(Context context, String pkg,
                                               String preferredActivity, Progress progress) {
        if (pkg == null || pkg.trim().isEmpty()) {
            return new LaunchResult(false, "No package name was supplied.");
        }

        boolean compatibility = SettingsStore.compatibilityMode(context);
        boolean maxStability = SettingsStore.maxStability(context);
        boolean childTasks = SettingsStore.childTasks(context);

        Set<String> candidates = resolveCandidates(context, pkg, preferredActivity);

        if (compatibility) {
            progress(context, progress, "Checking launcher entries and task behavior…");
            String analysis = analyze(context, pkg, candidates);
            progress(context, progress, analysis);
        }

        SharedPreferences cache = SettingsStore.launchCache(context);
        String cached = cache.getString(pkg, "");
        List<String> reasons = new ArrayList<>();

        if (!cached.isEmpty()) {
            String[] parts = cached.split("\\n", 2);
            String strategy = parts[0];
            String component = parts.length > 1 ? parts[1] : "";
            progress(context, progress, "Using the known working start method…");
            LaunchResult r = runStrategy(context, strategy, pkg, component, childTasks);
            if (r.ok) return r;
            reasons.add("Cached method: " + r.message);
            cache.edit().remove(pkg).apply();
        }

        progress(context, progress, "Resolving the best launcher entry…");

        if (SettingsStore.rootHelperEnabled(context)
                && SettingsStore.rootTaskStart(context)
                && RootPluginManager.isInstalled()) {
            progress(context, progress, "Trying CatCore Root Helper…");
            RootPluginManager.Result helper = RootPluginManager.run("launch", pkg);
            if (helper.ok) {
                return new LaunchResult(true, "Started through CatCore Root Helper.");
            }
            reasons.add("Root Helper: " + helper.message);
        }

        LaunchResult packageFull = runStrategy(
                context, "root_package_full", pkg, "", childTasks);
        if (packageFull.ok) {
            remember(cache, pkg, "root_package_full", "");
            return packageFull;
        }
        reasons.add("Package launch: " + packageFull.message);

        for (String component : candidates) {
            LaunchResult r = runStrategy(
                    context, "root_component_full", pkg, component, childTasks);
            if (r.ok) {
                remember(cache, pkg, "root_component_full", component);
                return r;
            }
            reasons.add(component + ": " + r.message);
        }

        for (String component : candidates) {
            LaunchResult r = runStrategy(
                    context, "direct_component_full", pkg, component, childTasks);
            if (r.ok) {
                remember(cache, pkg, "direct_component_full", component);
                return r;
            }
            reasons.add("Direct " + component + ": " + r.message);
        }

        LaunchResult directLaunch = runStrategy(
                context, "direct_launch_intent", pkg, "", childTasks);
        if (directLaunch.ok) {
            remember(cache, pkg, "direct_launch_intent", "");
            return directLaunch;
        }
        reasons.add("Android launch intent: " + directLaunch.message);

        if (maxStability || compatibility) {
            progress(context, progress, "Trying compatibility fallbacks…");

            for (String component : candidates) {
                LaunchResult r = runStrategy(
                        context, "root_component_basic", pkg, component, childTasks);
                if (r.ok) {
                    remember(cache, pkg, "root_component_basic", component);
                    return r;
                }
                reasons.add("Basic " + component + ": " + r.message);
            }

            LaunchResult packageBasic = runStrategy(
                    context, "root_package_basic", pkg, "", childTasks);
            if (packageBasic.ok) {
                remember(cache, pkg, "root_package_basic", "");
                return packageBasic;
            }
            reasons.add("Basic package launch: " + packageBasic.message);

            if (maxStability) {
                LaunchResult monkey = runStrategy(context, "monkey", pkg, "", childTasks);
                if (monkey.ok) {
                    remember(cache, pkg, "monkey", "");
                    return new LaunchResult(true,
                            "App started with the final compatibility fallback.");
                }
                reasons.add("Final fallback: " + monkey.message);
            }
        }

        String diagnostic = analyze(context, pkg, candidates);
        String lastReason = reasons.isEmpty() ? "No compatible launcher activity was found."
                : reasons.get(reasons.size() - 1);

        return new LaunchResult(false,
                "Start failed for " + pkg + ".\n\n"
                        + diagnostic + "\n\n"
                        + "Last result: " + lastReason
                        + "\n\nTried " + Math.max(1, reasons.size())
                        + " start paths. Open MultiTask settings for Compatibility or Max Stability.");
    }

    private static Set<String> resolveCandidates(Context context, String pkg,
                                                 String preferredActivity) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        PackageManager pm = context.getPackageManager();

        if (preferredActivity != null && !preferredActivity.trim().isEmpty()) {
            out.add(normalizeComponent(pkg, preferredActivity));
        }

        try {
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch != null && launch.getComponent() != null) {
                out.add(launch.getComponent().flattenToString());
            }
        } catch (Throwable ignored) {}

        try {
            Intent query = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setPackage(pkg);
            List<ResolveInfo> infos = pm.queryIntentActivities(query, PackageManager.MATCH_ALL);
            for (ResolveInfo info : infos) {
                if (info.activityInfo != null) {
                    out.add(new ComponentName(pkg, info.activityInfo.name).flattenToString());
                }
            }
        } catch (Throwable ignored) {}

        RootShell.Result rootResolve = RootShell.run(
                "cmd package resolve-activity --brief --user current "
                        + "-a android.intent.action.MAIN "
                        + "-c android.intent.category.LAUNCHER "
                        + "-p " + RootShell.quote(pkg), 4);
        if (rootResolve.ok) {
            for (String line : rootResolve.output.split("\\r?\\n")) {
                String value = line.trim();
                if (value.contains("/") && !value.startsWith("No activity")) {
                    out.add(value);
                }
            }
        }

        return out;
    }

    private static String normalizeComponent(String pkg, String activity) {
        if (activity.contains("/")) return activity;
        return new ComponentName(pkg, activity).flattenToString();
    }

    private static LaunchResult runStrategy(Context context, String strategy, String pkg,
                                            String component, boolean childTasks) {
        if (!SettingsStore.methodEnabled(context, strategy)) {
            return new LaunchResult(false, "Disabled in Developer Options: " + strategy);
        }

        boolean maxStability = SettingsStore.maxStability(context);
        String marker = " --ez " + EXTRA_FORCE_MULTITASK + " true";
        if (maxStability) {
            marker += " --ez " + EXTRA_MAX_STABILITY + " true";
        }
        String child = childTasks ? " --activity-task-on-home" : "";
        String full = " --activity-new-task --activity-multiple-task"
                + " --activity-new-document --activity-retain-in-recents" + child;

        if ("root_package_full".equals(strategy)) {
            return rootStart("am start --user current" + full
                    + " -a android.intent.action.MAIN"
                    + " -c android.intent.category.LAUNCHER"
                    + " -p " + RootShell.quote(pkg) + marker);
        }

        if ("root_component_full".equals(strategy) && !component.isEmpty()) {
            return rootStart("am start --user current" + full
                    + " -n " + RootShell.quote(component) + marker);
        }

        if ("root_component_basic".equals(strategy) && !component.isEmpty()) {
            return rootStart("am start --user current --activity-new-task"
                    + " --activity-multiple-task"
                    + " -n " + RootShell.quote(component) + marker);
        }

        if ("root_package_basic".equals(strategy)) {
            return rootStart("am start --user current --activity-new-task"
                    + " -a android.intent.action.MAIN"
                    + " -c android.intent.category.LAUNCHER"
                    + " -p " + RootShell.quote(pkg) + marker);
        }

        if ("monkey".equals(strategy)) {
            RootShell.Result r = RootShell.run(
                    "monkey -p " + RootShell.quote(pkg)
                            + " -c android.intent.category.LAUNCHER 1", 7);
            return new LaunchResult(r.ok, r.ok ? "Started." : RootShell.shortReason(r));
        }

        if ("direct_component_full".equals(strategy) && !component.isEmpty()) {
            try {
                Intent intent = new Intent();
                intent.setComponent(ComponentName.unflattenFromString(component));
                addFullFlags(intent, childTasks);
                intent.putExtra(EXTRA_FORCE_MULTITASK, true);
                intent.putExtra(EXTRA_MAX_STABILITY, maxStability);
                context.startActivity(intent);
                return new LaunchResult(true, "Started.");
            } catch (Throwable t) {
                return new LaunchResult(false, throwableReason(t));
            }
        }

        if ("direct_launch_intent".equals(strategy)) {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (intent == null) return new LaunchResult(false, "No launch intent.");
                addFullFlags(intent, childTasks);
                intent.putExtra(EXTRA_FORCE_MULTITASK, true);
                intent.putExtra(EXTRA_MAX_STABILITY, maxStability);
                context.startActivity(intent);
                return new LaunchResult(true, "Started.");
            } catch (Throwable t) {
                return new LaunchResult(false, throwableReason(t));
            }
        }

        return new LaunchResult(false, "Unknown start strategy.");
    }

    private static LaunchResult rootStart(String command) {
        RootShell.Result r = RootShell.run(command, 7);
        if (!r.ok) return new LaunchResult(false, RootShell.shortReason(r));
        String lower = r.output.toLowerCase();
        if (lower.contains("activity not started")
                || lower.contains("delivered to currently running")) {
            return new LaunchResult(false,
                    "Android reused the existing task instead of creating another one.");
        }
        return new LaunchResult(true, "Started.");
    }

    private static void addFullFlags(Intent intent, boolean childTasks) {
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS;
        if (childTasks) flags |= Intent.FLAG_ACTIVITY_TASK_ON_HOME;
        flags &= ~Intent.FLAG_ACTIVITY_CLEAR_TOP;
        flags &= ~Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
        flags &= ~Intent.FLAG_ACTIVITY_SINGLE_TOP;
        intent.addFlags(flags);
    }

    private static String analyze(Context context, String pkg, Set<String> candidates) {
        StringBuilder text = new StringBuilder();
        text.append("Launcher entries: ").append(candidates.size());

        if (!candidates.isEmpty()) {
            String first = candidates.iterator().next();
            ComponentName cn = ComponentName.unflattenFromString(first);
            if (cn != null) {
                try {
                    ActivityInfo info = context.getPackageManager()
                            .getActivityInfo(cn, PackageManager.MATCH_DISABLED_COMPONENTS);
                    text.append(" · launchMode=").append(launchModeName(info.launchMode));
                    text.append(" · document=").append(documentModeName(info.documentLaunchMode));
                    if (!info.exported) text.append(" · not exported");
                    if (!info.enabled) text.append(" · disabled");
                } catch (Throwable ignored) {}
            }
        }

        if (candidates.isEmpty()) {
            text.append(" · no exported launcher component was resolved");
        }
        return text.toString();
    }

    private static String launchModeName(int value) {
        switch (value) {
            case ActivityInfo.LAUNCH_SINGLE_TOP: return "singleTop";
            case ActivityInfo.LAUNCH_SINGLE_TASK: return "singleTask";
            case ActivityInfo.LAUNCH_SINGLE_INSTANCE: return "singleInstance";
            default: return "standard";
        }
    }

    private static String documentModeName(int value) {
        switch (value) {
            case ActivityInfo.DOCUMENT_LAUNCH_INTO_EXISTING: return "intoExisting";
            case ActivityInfo.DOCUMENT_LAUNCH_ALWAYS: return "always";
            case ActivityInfo.DOCUMENT_LAUNCH_NEVER: return "never";
            default: return "none";
        }
    }

    private static void remember(SharedPreferences cache, String pkg,
                                 String strategy, String component) {
        cache.edit().putString(pkg, strategy + "\n" + (component == null ? "" : component)).apply();
    }

    private static String throwableReason(Throwable t) {
        String msg = t.getMessage();
        return t.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }

    private static void progress(Context context, Progress progress, String message) {
        if (progress == null) return;
        new Handler(Looper.getMainLooper()).post(() -> progress.onProgress(message));
    }

    private static void post(Context context, Callback callback, boolean ok, String message) {
        if (callback == null) return;
        new Handler(Looper.getMainLooper()).post(() -> callback.onResult(ok, message));
    }

    private static final class LaunchResult {
        final boolean ok;
        final String message;

        LaunchResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }
}
