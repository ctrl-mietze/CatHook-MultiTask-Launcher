package com.catcore.ctrlmietze.multitask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        if (SettingsStore.frameworkEnabled(context)
                && SettingsStore.onboardingComplete(context)) {
            try {
                CatCoreFrameworkService.start(context);
            } catch (Throwable ignored) {
            }
        }

        AppTaskRules.mirrorToSystem(context);

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
