package com.catcore.ctrlmietze.multitask;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.catcore.ctrlmietze.multitask.window.WindowFramework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiagnosticsManager {
    public interface Callback {
        void onResult(boolean ok, String location);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private DiagnosticsManager() {}

    public static void export(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            String report = build(app);
            String location = write(app, report);
            boolean ok = location != null;
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            main.post(() -> {
                if (callback != null) callback.onResult(ok,
                        ok ? location : "Could not write diagnostic report.");
            });
        });
    }

    private static String build(Context context) {
        StringBuilder out = new StringBuilder(16_384);
        line(out, "CatCore MultiTask V2 diagnostics");
        line(out, "Generated: " + new Date());
        line(out, "");
        line(out, "[DEVICE]");
        line(out, "manufacturer=" + Build.MANUFACTURER);
        line(out, "brand=" + Build.BRAND);
        line(out, "model=" + Build.MODEL);
        line(out, "device=" + Build.DEVICE);
        line(out, "product=" + Build.PRODUCT);
        line(out, "android=" + Build.VERSION.RELEASE);
        line(out, "sdk=" + Build.VERSION.SDK_INT);
        line(out, "fingerprint=" + Build.FINGERPRINT);
        line(out, "");

        line(out, "[MULTITASK]");
        line(out, "package=" + context.getPackageName());
        line(out, "root=" + EnvironmentProbe.hasRoot());
        line(out, "lsposed_app_hook=" + EnvironmentProbe.isXposedActive());
        line(out, "system_hook=" + EnvironmentProbe.isSystemHookActive(context));
        line(out, "framework_enabled=" + SettingsStore.frameworkEnabled(context));
        line(out, "start_mode=" + SettingsStore.startMode(context));
        line(out, "compatibility_mode=" + SettingsStore.compatibilityMode(context));
        line(out, "max_stability=" + SettingsStore.maxStability(context));
        line(out, "secondary_displays=" + WindowFramework.supportsSecondaryDisplays(context));
        line(out, "freeform=" + WindowFramework.supportsFreeform(context));
        line(out, "root_helper_installed=" + RootPluginManager.isInstalled());
        line(out, "root_helper_ready=" + RootPluginManager.isReady());
        line(out, "");

        line(out, "[RUNTIME]");
        appendCommand(out, "id", "id; getenforce; uname -a");
        appendCommand(out, "display", "dumpsys display | head -n 180");
        appendCommand(out, "activity", "dumpsys activity activities | head -n 360");
        appendCommand(out, "processes", "ps -A -w -o PID,UID,RSS,PCPU,NAME | head -n 220");

        return out.toString();
    }

    private static void appendCommand(StringBuilder out, String name, String command) {
        line(out, "--- " + name + " ---");
        RootShell.Result result = RootShell.run(command, 10);
        line(out, result.output.isEmpty() ? "(no output; code " + result.code + ")" : result.output);
        line(out, "");
    }

    private static String write(Context context, String report) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = "CatCore_MultiTask_V2_Diagnostics_" + stamp + ".txt";

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/CatCore");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = context.getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return null;

                try (OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
                    if (stream == null) return null;
                    stream.write(report.getBytes(StandardCharsets.UTF_8));
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
                return "Download/CatCore/" + name;
            }

            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) return null;
            File file = new File(dir, name);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(report.getBytes(StandardCharsets.UTF_8));
            }
            return file.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void line(StringBuilder out, String value) {
        out.append(value == null ? "" : value).append('\n');
    }
}
