package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SystemTaskBridge {
    public static final String ACTION =
            "com.catcore.ctrlmietze.multitask.action.SYSTEM_TASK";
    public static final int RESULT_OK = 1;
    public static final int RESULT_ERROR = -1;

    public interface Callback {
        void onResult(boolean ok, int before, int after, int desired, String message);
    }

    private SystemTaskBridge() {}

    public static void ensureTaskCount(
            Context context,
            String packageName,
            String activityName,
            int desired,
            Callback callback) {
        Context app = context.getApplicationContext();
        int target = Math.max(1, Math.min(8, desired));
        Handler main = new Handler(Looper.getMainLooper());
        AtomicBoolean finished = new AtomicBoolean(false);

        ResultReceiver receiver = new ResultReceiver(main) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (!finished.compareAndSet(false, true)) return;
                int before = resultData == null ? -1 : resultData.getInt("before", -1);
                int after = resultData == null ? -1 : resultData.getInt("after", -1);
                int wanted = resultData == null ? target : resultData.getInt("desired", target);
                String message = resultData == null
                        ? "No result details."
                        : resultData.getString("message", "No result details.");
                if (callback != null) {
                    callback.onResult(resultCode == RESULT_OK, before, after, wanted, message);
                }
            }
        };

        Intent request = new Intent(ACTION)
                .putExtra("request_id", UUID.randomUUID().toString())
                .putExtra("package", packageName)
                .putExtra("activity", activityName == null ? "" : activityName)
                .putExtra("desired", target)
                .putExtra("result", receiver)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        try {
            app.sendBroadcast(request);
        } catch (Throwable t) {
            finished.set(true);
            if (callback != null) {
                callback.onResult(false, -1, -1, target,
                        t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            return;
        }

        main.postDelayed(() -> {
            if (!finished.compareAndSet(false, true)) return;
            if (callback != null) {
                callback.onResult(false, -1, -1, target,
                        "The LSPosed system task bridge did not answer within 7 seconds.");
            }
        }, 7000L);
    }
}
