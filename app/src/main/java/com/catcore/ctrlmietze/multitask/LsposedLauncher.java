package com.catcore.ctrlmietze.multitask;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LsposedLauncher {
    public interface Callback {
        void onResult(boolean ok, String message);
    }

    // LSPosed's hidden/parasitic manager secret code. This is the same entry
    // used by the supplied lsposed_shortcut_v2.2 APK.
    private static final String SECRET_CODE = "5776733";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private LsposedLauncher() {}

    public static void open(Activity activity, Callback callback) {
        EXEC.execute(() -> {
            String data = "android_secret_code://" + SECRET_CODE;

            // Android 8+ / current LSPosed path.
            RootShell.Result modern = RootShell.run(
                    "am broadcast --user 0"
                            + " -a android.telephony.action.SECRET_CODE"
                            + " -d " + RootShell.quote(data),
                    5);

            boolean ok = modern.ok;
            String detail = RootShell.shortReason(modern);

            // Keep the legacy Telephony secret-code action as compatibility
            // fallback. The shortcut APK also carries both action names.
            if (!ok) {
                RootShell.Result legacy = RootShell.run(
                        "am broadcast --user 0"
                                + " -a android.provider.Telephony.SECRET_CODE"
                                + " -d " + RootShell.quote(data),
                        5);
                ok = legacy.ok;
                detail = RootShell.shortReason(legacy);
            }

            final boolean success = ok;
            final String message = detail;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onResult(success, message);
            });
        });
    }
}
