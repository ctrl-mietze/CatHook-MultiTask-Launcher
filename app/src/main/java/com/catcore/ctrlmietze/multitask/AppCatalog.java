package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppCatalog {
    public static final String ACTION_UPDATED =
            "com.catcore.ctrlmietze.multitask.action.APP_CATALOG_UPDATED";

    public interface Callback {
        void onReady(List<AppEntry> apps);
    }

    public interface IconCallback {
        void onIcon(Drawable drawable);
    }

    private static final long DEFAULT_MAX_AGE = 15 * 60_000L;
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final ExecutorService ICON_EXEC = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Drawable> ICONS = new LruCache<>(96);

    private static volatile List<AppEntry> memory = Collections.emptyList();
    private static volatile long memoryMtime;

    private AppCatalog() {}

    public static List<AppEntry> loadCached(Context context) {
        File file = cacheFile(context);
        long mtime = file.exists() ? file.lastModified() : 0L;

        List<AppEntry> local = memory;
        if (!local.isEmpty() && memoryMtime == mtime) {
            return new ArrayList<>(local);
        }

        if (!file.exists()) return new ArrayList<>();

        try {
            byte[] bytes;
            try (FileInputStream in = new FileInputStream(file)) {
                bytes = new byte[(int) Math.min(Integer.MAX_VALUE, file.length())];
                int offset = 0;
                while (offset < bytes.length) {
                    int read = in.read(bytes, offset, bytes.length - offset);
                    if (read < 0) break;
                    offset += read;
                }
                if (offset != bytes.length) {
                    byte[] exact = new byte[offset];
                    System.arraycopy(bytes, 0, exact, 0, offset);
                    bytes = exact;
                }
            }

            JSONArray array = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            List<AppEntry> out = new ArrayList<>(array.length());

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;

                String pkg = o.optString("package", "");
                String label = o.optString("label", pkg);
                String activity = o.optString("activity", "");
                if (pkg.isEmpty()) continue;

                out.add(new AppEntry(label, pkg, activity, ICONS.get(pkg)));
            }

            memory = Collections.unmodifiableList(new ArrayList<>(out));
            memoryMtime = mtime;
            return out;
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
    }

    public static boolean needsRefresh(Context context) {
        File file = cacheFile(context);
        return !file.exists()
                || file.length() < 8
                || System.currentTimeMillis() - file.lastModified() > DEFAULT_MAX_AGE;
    }

    public static void refreshAsync(Context context, boolean force, Callback callback) {
        Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            List<AppEntry> result;
            if (!force && !needsRefresh(app)) {
                result = loadCached(app);
            } else {
                result = rebuild(app);
            }

            List<AppEntry> deliver = new ArrayList<>(result);
            MAIN.post(() -> {
                if (callback != null) callback.onReady(deliver);
            });
        });
    }

    public static void loadIconAsync(Context context, String packageName,
                                     IconCallback callback) {
        Drawable cached = ICONS.get(packageName);
        if (cached != null) {
            if (callback != null) callback.onIcon(cached);
            return;
        }

        Context app = context.getApplicationContext();
        ICON_EXEC.execute(() -> {
            Drawable drawable = null;
            try {
                drawable = app.getPackageManager().getApplicationIcon(packageName);
                if (drawable != null) ICONS.put(packageName, drawable);
            } catch (Throwable ignored) {
            }

            Drawable result = drawable;
            MAIN.post(() -> {
                if (callback != null) callback.onIcon(result);
            });
        });
    }

    private static List<AppEntry> rebuild(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, AppEntry> unique = new LinkedHashMap<>();

        try {
            for (ResolveInfo info : pm.queryIntentActivities(
                    query, PackageManager.MATCH_ALL)) {
                if (info.activityInfo == null) continue;
                ApplicationInfo ai = info.activityInfo.applicationInfo;
                if (context.getPackageName().equals(ai.packageName)) continue;

                boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (system && !SystemAppAllowlist.isAllowed(ai.packageName)) continue;

                String activity = info.activityInfo.name;
                try {
                    Intent launch = pm.getLaunchIntentForPackage(ai.packageName);
                    if (launch != null && launch.getComponent() != null) {
                        activity = launch.getComponent().getClassName();
                    }
                } catch (Throwable ignored) {
                }

                CharSequence labelValue = info.loadLabel(pm);
                String label = labelValue == null
                        ? ai.packageName : labelValue.toString();

                unique.putIfAbsent(ai.packageName,
                        new AppEntry(label, ai.packageName, activity, ICONS.get(ai.packageName)));
            }
        } catch (Throwable ignored) {
        }

        ArrayList<AppEntry> out = new ArrayList<>(unique.values());
        Collator collator = Collator.getInstance();
        out.sort((a, b) -> collator.compare(a.label, b.label));

        JSONArray array = new JSONArray();
        for (AppEntry entry : out) {
            try {
                JSONObject o = new JSONObject();
                o.put("label", entry.label);
                o.put("package", entry.packageName);
                o.put("activity", entry.activityName);
                array.put(o);
            } catch (Throwable ignored) {
            }
        }

        File target = cacheFile(context);
        File temp = new File(target.getParentFile(), target.getName() + ".tmp");

        try {
            try (FileOutputStream stream = new FileOutputStream(temp, false)) {
                stream.write(array.toString().getBytes(StandardCharsets.UTF_8));
                stream.flush();
            }

            if (!temp.renameTo(target)) {
                try (FileOutputStream stream = new FileOutputStream(target, false)) {
                    stream.write(array.toString().getBytes(StandardCharsets.UTF_8));
                    stream.flush();
                }
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
            }
        } catch (Throwable ignored) {
        }

        memory = Collections.unmodifiableList(new ArrayList<>(out));
        memoryMtime = target.exists() ? target.lastModified() : 0L;

        try {
            context.sendBroadcast(
                    new Intent(ACTION_UPDATED).setPackage(context.getPackageName()));
        } catch (Throwable ignored) {
        }

        return out;
    }

    private static File cacheFile(Context context) {
        return new File(context.getFilesDir(), "catcore_app_catalog_v2.json");
    }
}
