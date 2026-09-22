package com.catcore.ctrlmietze.multitask;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

public final class CatCoreFrameworkService extends Service {
    public static final String CHANNEL_ID = "catcore_multitask_framework";
    public static final int NOTIFICATION_ID = 2042;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String detail = "Framework active";
        if (intent != null && intent.hasExtra("status")) {
            detail = intent.getStringExtra("status");
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(detail));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MultiTask Framework",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the CatCore MultiTask framework available for task and window sessions.");
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
        Intent intent = new Intent(context, CatCoreFrameworkService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }
}
