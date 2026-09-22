package com.catcore.ctrlmietze.multitask;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

public final class CatCoreFrameworkService extends Service {
    public static final String CHANNEL_ID = "catcore_multitask_framework";
    public static final int NOTIFICATION_ID = 2042;
    private static final String ACTION_REFRESH_CATALOG =
            "com.catcore.ctrlmietze.multitask.action.REFRESH_CATALOG";
    private static final String ACTION_COMPAT_SCAN =
            "com.catcore.ctrlmietze.multitask.action.COMPAT_SCAN";

    private FrameworkHealthMonitor healthMonitor;
    private BroadcastReceiver packageReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        Notification notification = buildNotification("Framework active");
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        registerPackageWatcher();
        AppCatalog.refreshAsync(this, AppCatalog.needsRefresh(this), null);

        healthMonitor = new FrameworkHealthMonitor(this, this::publishFrameworkStatus);
        healthMonitor.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String detail = "Framework active";
        if (intent != null && intent.hasExtra("status")) {
            detail = intent.getStringExtra("status");
        }

        if (intent != null && ACTION_REFRESH_CATALOG.equals(intent.getAction())) {
            AppCatalog.refreshAsync(this, true, null);
            detail = "Framework active · updating app catalog";
        }

        if (intent != null && ACTION_COMPAT_SCAN.equals(intent.getAction())
                && SettingsStore.fullScanMode(this)) {
            detail = "Framework active · compatibility scan running";
            publishFrameworkStatus(detail);
            new Thread(() -> {
                int count = CompatibilityFullScanner.scan(this);
                publishFrameworkStatus(
                        "Framework active · " + count + " compatibility profiles ready");
            }, "CatCore-compat-scan").start();
            return START_STICKY;
        }

        publishFrameworkStatus(detail);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (healthMonitor != null) {
            healthMonitor.stop();
            healthMonitor = null;
        }

        if (packageReceiver != null) {
            try { unregisterReceiver(packageReceiver); } catch (Throwable ignored) {}
            packageReceiver = null;
        }

        super.onDestroy();
    }

    private void registerPackageWatcher() {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                AppCatalog.refreshAsync(CatCoreFrameworkService.this, true, null);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(packageReceiver, filter);
            }
        } catch (Throwable ignored) {
            packageReceiver = null;
        }
    }

    private void publishFrameworkStatus(String status) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID,
                    buildNotification(status == null ? "Framework active" : status));
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MultiTask Framework",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the CatCore MultiTask framework available for task, app catalog and stability sessions.");
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification buildNotification(String detail) {
        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                2042,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return b.setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("CatCore MultiTask")
                .setContentText(detail == null ? "Framework active" : detail)
                .setColor(Color.rgb(110, 133, 255))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pending)
                .build();
    }

    public static void start(Context context) {
        updateStatus(context, "Framework active");
    }

    public static void updateStatus(Context context, String status) {
        Intent intent = new Intent(context, CatCoreFrameworkService.class);
        intent.putExtra("status", status == null ? "Framework active" : status);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void requestCatalogRefresh(Context context) {
        Intent intent = new Intent(context, CatCoreFrameworkService.class)
                .setAction(ACTION_REFRESH_CATALOG);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void requestCompatibilityScan(Context context) {
        Intent intent = new Intent(context, CatCoreFrameworkService.class)
                .setAction(ACTION_COMPAT_SCAN);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }
}
