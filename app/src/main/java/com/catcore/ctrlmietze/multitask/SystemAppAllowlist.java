package com.catcore.ctrlmietze.multitask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class SystemAppAllowlist {
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "com.android.chrome",
            "com.android.vending",
            "com.google.android.googlequicksearchbox",
            "com.google.android.gm",
            "com.google.android.apps.maps",
            "com.google.android.youtube",
            "com.google.android.apps.photos",
            "com.google.android.apps.messaging",
            "com.google.android.calendar",
            "com.google.android.keep",
            "com.google.android.apps.docs",
            "com.google.android.apps.tachyon"
    ));

    private SystemAppAllowlist() {}

    public static boolean isAllowed(String packageName) {
        return ALLOWED.contains(packageName);
    }
}
