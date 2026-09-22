package com.catcore.ctrlmietze.multitask;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private TextView frameworkStatus;
    private TextView taskManagerSummary;
    private final ExecutorService dashboardExec = Executors.newSingleThreadExecutor();
    private volatile boolean rootReady;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (!SettingsStore.onboardingComplete(this)) {
            startActivity(new Intent(this, FirstStartActivity.class));
            finish();
            return;
        }

        if (SettingsStore.legacyEasyMode(this)) {
            startActivity(new Intent(this, LegacyHomeActivity.class));
            finish();
            return;
        }

        CatUi.applyWindow(this);
        if (SettingsStore.frameworkEnabled(this)) {
            try { CatCoreFrameworkService.start(this); } catch (Throwable ignored) {}
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(30), dp(16), dp(18));
        root.setBackground(CatUi.background());

        LinearLayout hero = CatUi.card(this);
        hero.setBackground(CatUi.hero(this));
        hero.setPadding(dp(18), dp(18), dp(18), dp(16));
        root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        hero.addView(heroTop);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        heroTop.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView kicker = CatUi.text(this, "CATCORE", 11, Color.rgb(165, 177, 255), true);
        kicker.setLetterSpacing(0.12f);
        brand.addView(kicker);
        brand.addView(CatUi.text(this, "CatCore MultiTask", 29, CatUi.TEXT, true));

        TextView subtitle = CatUi.text(this,
                "Independent task sessions, framework windows and per-app launch rules.",
                12, Color.rgb(205, 213, 235), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(5);
        brand.addView(subtitle, sp);

        TextView version = CatUi.pill(this, "V2 DEV8", Color.rgb(56, 65, 128));
        heroTop.addView(version, new LinearLayout.LayoutParams(dp(82), dp(34)));

        rootReady = EnvironmentProbe.hasRoot();
        boolean xposedReady = isXposedActive();
        boolean systemHookReady = EnvironmentProbe.isSystemHookActive(this);

        LinearLayout statusGrid = new LinearLayout(this);
        statusGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sgp = new LinearLayout.LayoutParams(-1, -2);
        sgp.topMargin = dp(14);
        hero.addView(statusGrid, sgp);

        LinearLayout statusTop = new LinearLayout(this);
        statusTop.setGravity(Gravity.CENTER_VERTICAL);
        statusGrid.addView(statusTop, new LinearLayout.LayoutParams(-1, dp(32)));
        statusTop.addView(statusPill(
                rootReady ? "ROOT READY" : "ROOT MISSING",
                rootReady ? CatUi.GOOD : CatUi.BAD));
        statusTop.addView(statusPill(
                xposedReady ? "LSPOSED ACTIVE" : "LSPOSED OFF",
                xposedReady ? CatUi.GOOD : CatUi.BAD));

        LinearLayout statusBottom = new LinearLayout(this);
        statusBottom.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(-1, dp(32));
        sbp.topMargin = dp(6);
        statusGrid.addView(statusBottom, sbp);
        statusBottom.addView(statusPill(
                systemHookReady ? "SYSTEM HOOK" : "HOOK OFF",
                systemHookReady ? CatUi.GOOD : CatUi.WARN));

        frameworkStatus = statusPill(
                isFrameworkRunning() ? "FRAMEWORK" : "FRAMEWORK OFF",
                isFrameworkRunning() ? CatUi.GOOD : CatUi.WARN);
        statusBottom.addView(frameworkStatus);

        // Task Manager now lives inside the CatCore header instead of consuming
        // another full dashboard card on Home.
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(73, 79, 125));
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, dp(1));
        dpv.topMargin = dp(14);
        hero.addView(divider, dpv);

        LinearLayout taskRow = new LinearLayout(this);
        taskRow.setGravity(Gravity.CENTER_VERTICAL);
        taskRow.setPadding(0, dp(12), 0, 0);
        hero.addView(taskRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout taskText = new LinearLayout(this);
        taskText.setOrientation(LinearLayout.VERTICAL);
        taskRow.addView(taskText, new LinearLayout.LayoutParams(0, -2, 1));

        TextView taskKicker = CatUi.text(this, "LIVE CONTROL · TASK MANAGER",
                9, Color.rgb(159, 176, 255), true);
        taskKicker.setLetterSpacing(0.08f);
        taskText.addView(taskKicker);

        taskManagerSummary = CatUi.text(
                this, "Reading running tasks…", 12, Color.rgb(214, 220, 241), false);
        LinearLayout.LayoutParams tsp = new LinearLayout.LayoutParams(-1, -2);
        tsp.topMargin = dp(4);
        taskText.addView(taskManagerSummary, tsp);

        TextView openTaskManager = CatUi.pill(this, "OPEN", Color.rgb(42, 104, 83));
        openTaskManager.setContentDescription("Open Task Manager");
        taskRow.addView(openTaskManager, new LinearLayout.LayoutParams(dp(68), dp(34)));

        View.OnClickListener taskOpen = v ->
                startActivity(new Intent(this, TaskManagerActivity.class));
        taskRow.setOnClickListener(taskOpen);
        openTaskManager.setOnClickListener(taskOpen);
        CatUi.pressScale(taskRow);

        LinearLayout modeCard = CatUi.card(this);
        LinearLayout.LayoutParams mcp = CatUi.cardParams(this);
        mcp.topMargin = dp(12);
        root.addView(modeCard, mcp);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        modeCard.addView(modeRow);

        LinearLayout modeText = new LinearLayout(this);
        modeText.setOrientation(LinearLayout.VERTICAL);
        modeRow.addView(modeText, new LinearLayout.LayoutParams(0, -2, 1));

        boolean mine = SettingsStore.startMode(this) == SettingsStore.MODE_MY_TASK;
        modeText.addView(CatUi.text(this,
                mine ? "Start as my task" : "Start as app's own task",
                16, CatUi.TEXT, true));
        modeText.addView(CatUi.text(this,
                mine ? "MultiTask-owned V2 session" : "LSPosed native system task bridge",
                12, CatUi.MUTED, false));

        TextView modePill = CatUi.pill(this, "CHANGE", Color.rgb(51, 61, 86));
        modeRow.addView(modePill, new LinearLayout.LayoutParams(dp(76), dp(34)));
        modeCard.setOnClickListener(v -> showModeSelector());
        CatUi.pressScale(modeCard);

        LinearLayout appStarter = CatUi.card(this);
        LinearLayout.LayoutParams asp = CatUi.cardParams(this);
        asp.topMargin = dp(10);
        root.addView(appStarter, asp);

        LinearLayout appRow = new LinearLayout(this);
        appRow.setGravity(Gravity.CENTER_VERTICAL);
        appStarter.addView(appRow);

        LinearLayout appText = new LinearLayout(this);
        appText.setOrientation(LinearLayout.VERTICAL);
        appRow.addView(appText, new LinearLayout.LayoutParams(0, -2, 1));
        appText.addView(CatUi.text(this, "App Starter", 17, CatUi.TEXT, true));
        appText.addView(CatUi.text(this,
                "Apps · search · exact task count · start",
                12, CatUi.MUTED, false));

        TextView appOpen = CatUi.pill(this, "OPEN", Color.rgb(55, 67, 112));
        appRow.addView(appOpen, new LinearLayout.LayoutParams(dp(68), dp(34)));

        View.OnClickListener starterOpen = v ->
                startActivity(new Intent(this, AppStarterActivity.class));
        appStarter.setOnClickListener(starterOpen);
        appOpen.setOnClickListener(starterOpen);
        CatUi.pressScale(appStarter);

        Button settings = CatUi.secondaryButton(this, "Settings");
        LinearLayout.LayoutParams setp = new LinearLayout.LayoutParams(-1, dp(52));
        setp.topMargin = dp(10);
        root.addView(settings, setp);
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        setContentView(root);
        refreshTaskManagerSummary();

        if (SettingsStore.restoreRuntime(this)) {
            RuntimeTuning.applyAsync(this, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (frameworkStatus != null) {
            boolean running = isFrameworkRunning();
            frameworkStatus.setText(running ? "FRAMEWORK" : "FRAMEWORK OFF");
            frameworkStatus.setBackground(CatUi.shape(
                    this,
                    running ? Color.rgb(29, 78, 62) : Color.rgb(88, 67, 28),
                    999));
        }

        if (!dashboardExec.isShutdown()) {
            dashboardExec.execute(() -> rootReady = EnvironmentProbe.hasRoot());
        }
        refreshTaskManagerSummary();
    }

    @Override
    protected void onDestroy() {
        dashboardExec.shutdownNow();
        super.onDestroy();
    }

    public boolean isXposedActive() {
        return EnvironmentProbe.isXposedActive();
    }

    private void refreshTaskManagerSummary() {
        if (taskManagerSummary == null || dashboardExec.isShutdown()) return;

        dashboardExec.execute(() -> {
            List<TaskInspector.TaskInfo> tasks;
            try {
                tasks = TaskInspector.readUserTasks(this);
            } catch (Throwable t) {
                tasks = new ArrayList<>();
            }

            Map<String, Integer> counts = new LinkedHashMap<>();
            long ram = 0L;
            Map<String, Long> ramByPackage = new LinkedHashMap<>();

            for (TaskInspector.TaskInfo task : tasks) {
                counts.put(task.packageName,
                        counts.getOrDefault(task.packageName, 0) + 1);
                Long known = ramByPackage.get(task.packageName);
                if (known == null || task.rssBytes > known) {
                    ramByPackage.put(task.packageName, task.rssBytes);
                }
            }

            for (Long value : ramByPackage.values()) {
                if (value != null) ram += value;
            }

            int multi = 0;
            for (Integer count : counts.values()) {
                if (count != null && count > 1) multi++;
            }

            final int appCount = counts.size();
            final int taskCount = tasks.size();
            final int multiCount = multi;
            final long ramBytes = ram;

            runOnUiThread(() -> {
                if (taskManagerSummary == null) return;
                taskManagerSummary.setText(
                        appCount + " apps · "
                                + taskCount + " tasks · "
                                + multiCount + " MultiTask · "
                                + formatRamShort(ramBytes));
            });
        });
    }

    private static String formatRamShort(long bytes) {
        if (bytes <= 0L) return "RAM --";
        double mib = bytes / 1048576d;
        if (mib >= 1024d) {
            return String.format(java.util.Locale.US, "%.1f GB RAM", mib / 1024d);
        }
        return String.format(java.util.Locale.US, "%.0f MB RAM", mib);
    }

    private TextView statusPill(String value, int color) {
        int bg = Color.rgb(
                Math.max(20, Color.red(color) / 3),
                Math.max(20, Color.green(color) / 3),
                Math.max(20, Color.blue(color) / 3));
        TextView pill = CatUi.pill(this, value, bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(30), 1);
        p.rightMargin = dp(5);
        pill.setLayoutParams(p);
        pill.setTextSize(9);
        return pill;
    }

    private void showModeSelector() {
        final String[] modes = {"Start as my task", "Start as app's own task"};
        int current = SettingsStore.startMode(this) == SettingsStore.MODE_APP_OWN_TASK ? 1 : 0;
        CatDialog.selector(this, "Main start method", modes, current, which -> {
            SettingsStore.setStartMode(this,
                    which == 1 ? SettingsStore.MODE_APP_OWN_TASK : SettingsStore.MODE_MY_TASK);
            recreate();
        });
    }

    private boolean isFrameworkRunning() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am == null) return false;
            String wanted = getPackageName() + ":framework";
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (wanted.equals(info.processName)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
