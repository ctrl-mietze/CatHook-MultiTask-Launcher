package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RootPluginManager {
    public interface Callback {
        void onResult(boolean ok, String message);
    }

    public static final String MODULE_ID = "catcore_multitask_root";
    private static final String MODULE_DIR = "/data/adb/modules/" + MODULE_ID;
    private static final String HELPER = MODULE_DIR + "/bin/catcore-helper";

    private static final ExecutorService EXEC = Executors.newCachedThreadPool();

    private RootPluginManager() {}

    public static boolean isInstalled() {
        RootShell.Result r = RootShell.run(
                "[ -f " + RootShell.quote(MODULE_DIR + "/module.prop")
                        + " ] && [ ! -f " + RootShell.quote(MODULE_DIR + "/disable")
                        + " ] && echo installed", 4);
        return r.ok && r.output.contains("installed");
    }

    public static boolean isReady() {
        RootShell.Result r = RootShell.run(
                "[ -x " + RootShell.quote(HELPER) + " ] && "
                        + RootShell.quote(HELPER) + " status", 4);
        return r.ok && r.output.toLowerCase().contains("ready");
    }

    public static void runAsync(String operation, String packageName, Callback callback) {
        EXEC.execute(() -> {
            Result result = run(operation, packageName);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onResult(result.ok, result.message));
            }
        });
    }

    public static Result run(String operation, String packageName) {
        if (!isInstalled()) {
            return new Result(false, "CatCore Root Helper is not installed.");
        }

        StringBuilder command = new StringBuilder();
        command.append(RootShell.quote(HELPER))
                .append(" ")
                .append(RootShell.quote(operation == null ? "" : operation));
        if (packageName != null && !packageName.trim().isEmpty()) {
            command.append(" ").append(RootShell.quote(packageName.trim()));
        }

        RootShell.Result r = RootShell.run(command.toString(), 8);
        return new Result(r.ok, r.ok ? r.output : RootShell.shortReason(r));
    }

    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message == null ? "" : message;
        }
    }
}
