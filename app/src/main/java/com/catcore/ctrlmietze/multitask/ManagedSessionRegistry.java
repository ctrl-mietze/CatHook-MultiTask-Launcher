package com.catcore.ctrlmietze.multitask;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ManagedSessionRegistry {
    private static final long DEFAULT_MAX_AGE = 30 * 60_000L;

    private ManagedSessionRegistry() {}

    public static void touch(Context context, String packageName) {
        if (!valid(packageName)) return;
        File dir = directory(context);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        File marker = new File(dir, packageName);
        try (FileOutputStream stream = new FileOutputStream(marker, false)) {
            stream.write(String.valueOf(System.currentTimeMillis())
                    .getBytes(StandardCharsets.UTF_8));
            stream.flush();
        } catch (Throwable ignored) {
        }
        //noinspection ResultOfMethodCallIgnored
        marker.setLastModified(System.currentTimeMillis());
    }

    public static void clear(Context context, String packageName) {
        if (!valid(packageName)) return;
        //noinspection ResultOfMethodCallIgnored
        new File(directory(context), packageName).delete();
    }

    public static Set<String> recent(Context context) {
        return recent(context, DEFAULT_MAX_AGE);
    }

    public static Set<String> recent(Context context, long maxAgeMs) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        File[] files = directory(context).listFiles();
        if (files == null) return out;

        long now = System.currentTimeMillis();
        for (File file : files) {
            if (!file.isFile()) continue;
            if (!valid(file.getName())) continue;
            if (now - file.lastModified() > maxAgeMs) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                continue;
            }
            out.add(file.getName());
        }
        return out;
    }

    private static File directory(Context context) {
        return new File(context.getFilesDir(), "catcore_managed_sessions");
    }

    private static boolean valid(String pkg) {
        return pkg != null
                && pkg.length() <= 255
                && pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");
    }
}
