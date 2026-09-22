package com.catcore.ctrlmietze.multitask.window;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public final class WindowFramework {
    public static final String DISPLAY_PREFIX = "CatCore.MultiTask.Window:";

    static final String EXTRA_PACKAGE = "window_package";
    static final String EXTRA_ACTIVITY = "window_activity";
    static final String EXTRA_LABEL = "window_label";

    private WindowFramework() {}

    public static void open(Activity activity, String packageName,
                            String activityName, String label) {
        Intent intent = new Intent(activity, WindowHostActivity.class);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_ACTIVITY, activityName == null ? "" : activityName);
        intent.putExtra(EXTRA_LABEL, label == null ? packageName : label);
        activity.startActivity(intent);
    }

    public static boolean supportsSecondaryDisplays(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS);
    }

    public static boolean supportsFreeform(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT);
    }

    public static String capabilitySummary(Context context) {
        return "Secondary displays: " + yesNo(supportsSecondaryDisplays(context))
                + " · Freeform: " + yesNo(supportsFreeform(context));
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
