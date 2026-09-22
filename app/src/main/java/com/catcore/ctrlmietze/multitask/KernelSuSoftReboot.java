package com.catcore.ctrlmietze.multitask;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class KernelSuSoftReboot {
    public interface Callback {
        void onResult(boolean accepted, String message);
    }

    private static final String KSUD = "/data/adb/ksud";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private KernelSuSoftReboot() {}

    public static void request(Callback callback) {
        EXEC.execute(() -> {
            RootShell.Result result = RootShell.run(
                    "set -e; "
                            + "[ \"$(id -u)\" = \"0\" ] || { echo 'Root shell unavailable'; exit 62; }; "
                            + "[ -x " + RootShell.quote(KSUD) + " ] || { "
                            + "echo 'KernelSU native soft reboot is unavailable: /data/adb/ksud missing'; exit 64; }; "
                            + RootShell.quote(KSUD) + " soft-reboot",
                    10);

            String message = result.ok
                    ? "KernelSU accepted the native soft reboot. Android userspace will restart now."
                    : RootShell.shortReason(result);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onResult(result.ok, message);
            });
        });
    }

    public static boolean isAvailable() {
        RootShell.Result result = RootShell.run(
                "[ \"$(id -u)\" = \"0\" ] && [ -x "
                        + RootShell.quote(KSUD) + " ]",
                4);
        return result.ok;
    }
}
