package com.catcore.ctrlmietze.multitask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SettingsStore.restoreRuntime(context)) return;
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                RuntimeTuning.applyBlocking(context.getApplicationContext());
            } finally {
                pending.finish();
            }
        }, "MultiTask-Restore").start();
    }
}
