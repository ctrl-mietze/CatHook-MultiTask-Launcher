package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CompatibilityFullScanner {
    private CompatibilityFullScanner() {}

    static int scan(Context context) {
        PackageManager pm = context.getPackageManager();
        Set<String> packages = new LinkedHashSet<>();

        Intent launcher = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        try {
            List<ResolveInfo> infos = pm.queryIntentActivities(
                    launcher, PackageManager.MATCH_ALL);
            for (ResolveInfo info : infos) {
                if (info.activityInfo == null) continue;
                ApplicationInfo ai = info.activityInfo.applicationInfo;
                boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (!system || SystemAppAllowlist.isAllowed(ai.packageName)) {
                    packages.add(ai.packageName);
                }
            }
        } catch (Throwable ignored) {}

        File dir = new File(context.getFilesDir(), "compatibility_profiles");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        int written = 0;
        for (String pkg : packages) {
            try {
                PackageInfo pi = pm.getPackageInfo(
                        pkg,
                        PackageManager.GET_ACTIVITIES
                                | PackageManager.GET_META_DATA
                                | PackageManager.MATCH_DISABLED_COMPONENTS);

                JSONObject root = new JSONObject();
                root.put("package", pkg);
                root.put("versionCode", pi.getLongVersionCode());
                root.put("lastUpdateTime", pi.lastUpdateTime);

                Intent launch = pm.getLaunchIntentForPackage(pkg);
                root.put("mainActivity",
                        launch != null && launch.getComponent() != null
                                ? launch.getComponent().flattenToShortString() : "");

                JSONArray activities = new JSONArray();
                if (pi.activities != null) {
                    for (ActivityInfo ai : pi.activities) {
                        JSONObject a = new JSONObject();
                        a.put("name", ai.name);
                        a.put("exported", ai.exported);
                        a.put("enabled", ai.enabled);
                        a.put("launchMode", launchMode(ai.launchMode));
                        a.put("documentLaunchMode", documentMode(ai.documentLaunchMode));
                        a.put("taskAffinity", ai.taskAffinity == null ? "" : ai.taskAffinity);
                        a.put("excludeFromRecents",
                                (ai.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) != 0);
                        a.put("allowTaskReparenting",
                                (ai.flags & ActivityInfo.FLAG_ALLOW_TASK_REPARENTING) != 0);
                        a.put("finishOnTaskLaunch",
                                (ai.flags & ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH) != 0);
                        a.put("clearTaskOnLaunch",
                                (ai.flags & ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
                        activities.put(a);
                    }
                }
                root.put("activities", activities);

                File target = new File(dir, pkg + ".json");
                try (FileOutputStream out = new FileOutputStream(target, false)) {
                    out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
                written++;
            } catch (Throwable ignored) {
            }
        }

        return written;
    }

    private static String launchMode(int value) {
        switch (value) {
            case ActivityInfo.LAUNCH_SINGLE_TOP: return "singleTop";
            case ActivityInfo.LAUNCH_SINGLE_TASK: return "singleTask";
            case ActivityInfo.LAUNCH_SINGLE_INSTANCE: return "singleInstance";
            default: return "standard";
        }
    }

    private static String documentMode(int value) {
        switch (value) {
            case ActivityInfo.DOCUMENT_LAUNCH_INTO_EXISTING: return "intoExisting";
            case ActivityInfo.DOCUMENT_LAUNCH_ALWAYS: return "always";
            case ActivityInfo.DOCUMENT_LAUNCH_NEVER: return "never";
            default: return "none";
        }
    }
}
