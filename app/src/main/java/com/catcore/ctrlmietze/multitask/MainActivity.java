package com.catcore.ctrlmietze.multitask;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
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
    private boolean rootReady;
    private AlertDialog compatibilityDialog;
    private TextView compatibilityText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (!SettingsStore.onboardingComplete(this)) {
            startActivity(new Intent(this, FirstStartActivity.class));
            finish();
            return;
        }

        CatUi.applyWindow(this);
        if (SettingsStore.frameworkEnabled(this)) {
            try { CatCoreFrameworkService.start(this); } catch (Throwable ignored) {}
        }

        List<AppEntry> apps = loadApps();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), 0);
        root.setBackground(CatUi.background());

        LinearLayout hero = CatUi.card(this);
        hero.setBackground(CatUi.hero(this));
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
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
                13, Color.rgb(205, 213, 235), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(6);
        brand.addView(subtitle, sp);

        TextView version = CatUi.pill(this, "V2", Color.rgb(56, 65, 128));
        heroTop.addView(version, new LinearLayout.LayoutParams(dp(54), dp(34)));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(-1, -2);
        srp.topMargin = dp(16);
        hero.addView(statusRow, srp);

        rootReady = EnvironmentProbe.hasRoot();
        boolean xposedReady = isXposedActive();
        boolean systemHookReady = EnvironmentProbe.isSystemHookActive(this);

        statusRow.addView(statusPill(
                rootReady ? "ROOT" : "NO ROOT",
                rootReady ? CatUi.GOOD : CatUi.BAD));

        statusRow.addView(statusPill(
                xposedReady ? "LSPOSED" : "NO LSPOSED",
                xposedReady ? CatUi.GOOD : CatUi.BAD));

        statusRow.addView(statusPill(
                systemHookReady ? "SYSTEM HOOK" : "HOOK OFF",
                systemHookReady ? CatUi.GOOD : CatUi.WARN));

        frameworkStatus = statusPill(
                isFrameworkRunning() ? "FRAMEWORK" : "FRAMEWORK OFF",
                isFrameworkRunning() ? CatUi.GOOD : CatUi.WARN);
        statusRow.addView(frameworkStatus);

        LinearLayout modeCard = CatUi.card(this);
        LinearLayout.LayoutParams mcp = CatUi.cardParams(this);
        mcp.topMargin = dp(14);
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
        modeCard.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout taskHub = CatUi.card(this);
        taskHub.setBackground(CatUi.dashboard(this));
        LinearLayout.LayoutParams thp = CatUi.cardParams(this);
        thp.topMargin = dp(14);
        root.addView(taskHub, thp);

        LinearLayout taskTop = new LinearLayout(this);
        taskTop.setGravity(Gravity.CENTER_VERTICAL);
        taskHub.addView(taskTop);

        LinearLayout taskText = new LinearLayout(this);
        taskText.setOrientation(LinearLayout.VERTICAL);
        taskTop.addView(taskText, new LinearLayout.LayoutParams(0, -2, 1));

        TextView taskKicker = CatUi.text(this, "LIVE CONTROL", 10,
                Color.rgb(151, 169, 255), true);
        taskKicker.setLetterSpacing(0.11f);
        taskText.addView(taskKicker);

        taskText.addView(CatUi.text(this, "Task Manager", 22, CatUi.TEXT, true));

        taskManagerSummary = CatUi.text(
                this,
                "Reading running apps and tasks…",
                12,
                Color.rgb(190, 202, 229),
                false);
        LinearLayout.LayoutParams tsp = new LinearLayout.LayoutParams(-1, -2);
        tsp.topMargin = dp(5);
        taskText.addView(taskManagerSummary, tsp);

        TextView live = CatUi.pill(this, "LIVE", Color.rgb(33, 100, 78));
        taskTop.addView(live, new LinearLayout.LayoutParams(dp(58), dp(32)));

        TextView taskHint = CatUi.text(
                this,
                "Open live tasks, inspect RAM / CPU / displays, create additional sessions or close duplicate tasks.",
                12, CatUi.MUTED, false);
        LinearLayout.LayoutParams tip = new LinearLayout.LayoutParams(-1, -2);
        tip.topMargin = dp(11);
        taskHub.addView(taskHint, tip);

        Button openTasks = CatUi.primaryButton(this, "Open Task Manager");
        LinearLayout.LayoutParams otp = new LinearLayout.LayoutParams(-1, dp(52));
        otp.topMargin = dp(14);
        taskHub.addView(openTasks, otp);

        View.OnClickListener taskOpen = v ->
                startActivity(new Intent(this, TaskManagerActivity.class));
        taskHub.setOnClickListener(taskOpen);
        openTasks.setOnClickListener(taskOpen);
        CatUi.pressScale(taskHub);

        Button settings = CatUi.secondaryButton(this, "Settings");
        LinearLayout.LayoutParams setp = new LinearLayout.LayoutParams(-1, dp(52));
        setp.topMargin = dp(10);
        root.addView(settings, setp);
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout appsHeader = new LinearLayout(this);
        appsHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ahp = new LinearLayout.LayoutParams(-1, -2);
        ahp.topMargin = dp(22);
        root.addView(appsHeader, ahp);

        appsHeader.addView(CatUi.text(this, "Apps", 20, CatUi.TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        appsHeader.addView(CatUi.pill(this,
                String.valueOf(apps.size()), Color.rgb(40, 47, 66)));

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search apps or packages");
        search.setTextColor(CatUi.TEXT);
        search.setHintTextColor(Color.rgb(118, 131, 156));
        search.setTextSize(14);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(CatUi.stroke(this, CatUi.SURFACE, 18, Color.rgb(42, 49, 69)));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.topMargin = dp(10);
        searchParams.bottomMargin = dp(4);
        root.addView(search, searchParams);

        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setClipToPadding(false);
        list.setPadding(0, dp(2), 0, dp(18));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        AppAdapter adapter = new AppAdapter(this, apps);
        list.setAdapter(adapter);

        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
            }
            public void afterTextChanged(android.text.Editable editable) {}
        });

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
            frameworkStatus.setBackground(CatUi.shape(this,
                    running ? Color.rgb(29, 78, 62) : Color.rgb(88, 67, 28), 999));
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

    public boolean isLaunchFrameworkReady() {
        return rootReady
                && isXposedActive()
                && EnvironmentProbe.isSystemHookActive(this);
    }

    public void showFrameworkRequired() {
        boolean xposed = isXposedActive();
        boolean systemHook = EnvironmentProbe.isSystemHookActive(this);
        int targetStep = !rootReady ? 1 : (!xposed ? 2 : 3);

        String message;
        String title;
        if (!rootReady) {
            title = "Root access required";
            message = "MultiTask no longer has an active root grant. Re-enable it in KernelSU, Magisk or your compatible root manager and verify it in setup.";
        } else if (!xposed) {
            title = "LSPosed activation required";
            message = "MultiTask's built-in LSPosed module is not active. Enable MultiTask and keep MultiTask + System Framework in scope.";
        } else {
            title = "System hook required";
            message = "LSPosed is active, but the Android System Framework hook is not detected for this boot. Check the System Framework scope; after first activation a reboot may be required.";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Close", null)
                .setPositiveButton("Open setup", (d, w) -> {
                    SettingsStore.setOnboardingComplete(this, false);
                    getSharedPreferences("multitask_first_start", MODE_PRIVATE)
                            .edit().putInt("step", targetStep).apply();
                    Intent i = new Intent(this, FirstStartActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finishAffinity();
                })
                .show();
    }

    public void showXposedRequired() {
        new AlertDialog.Builder(this)
                .setTitle("LSPosed activation required")
                .setMessage("Enable MultiTask in LSPosed with MultiTask + System Framework in scope, then restart MultiTask.")
                .setNegativeButton("Close", null)
                .setPositiveButton("Open setup", (d, w) -> {
                    SettingsStore.setOnboardingComplete(this, false);
                    getSharedPreferences("multitask_first_start", MODE_PRIVATE)
                            .edit().putInt("step", 2).apply();
                    Intent i = new Intent(this, FirstStartActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finishAffinity();
                })
                .show();
    }

    public void showCompatibilityProgress(String message) {
        if (compatibilityDialog == null) {
            LinearLayout box = new LinearLayout(this);
            box.setGravity(Gravity.CENTER_VERTICAL);
            box.setPadding(dp(20), dp(16), dp(20), dp(16));

            ProgressBar progress = new ProgressBar(this);
            box.addView(progress, new LinearLayout.LayoutParams(dp(32), dp(32)));

            compatibilityText = CatUi.text(this, message, 13, CatUi.TEXT, false);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
            textParams.leftMargin = dp(14);
            box.addView(compatibilityText, textParams);

            compatibilityDialog = new AlertDialog.Builder(this)
                    .setTitle("Compatibility analysis")
                    .setView(box)
                    .setCancelable(false)
                    .create();
            compatibilityDialog.show();
        } else if (compatibilityText != null) {
            compatibilityText.setText(message);
        }
    }

    public void hideCompatibilityProgress() {
        if (compatibilityDialog != null) {
            compatibilityDialog.dismiss();
            compatibilityDialog = null;
            compatibilityText = null;
        }
    }

    public void showLaunchFailure(String appName, String message) {
        hideCompatibilityProgress();
        new AlertDialog.Builder(this)
                .setTitle("Couldn’t open " + appName)
                .setMessage(message)
                .setNegativeButton("Close", null)
                .setPositiveButton("Open settings", (d, w) ->
                        startActivity(new Intent(this, SettingsActivity.class)))
                .show();
    }

    public void askTaskRule(AppEntry app, Runnable onChanged) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(AppTaskRules.get(this, app.packageName)));
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(20), dp(8), dp(20), dp(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Automatic task count")
                .setMessage("When " + app.label + " is opened normally, how many app tasks should exist? 1 disables the rule.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int count;
                    try { count = Integer.parseInt(input.getText().toString().trim()); }
                    catch (Throwable t) { count = 0; }
                    if (count < 1 || count > 8) {
                        input.setError("Choose 1–8");
                        return;
                    }
                    AppTaskRules.set(this, app.packageName, count);
                    dialog.dismiss();
                    if (onChanged != null) onChanged.run();
                }));

        dialog.show();
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
                        appCount + " running app" + (appCount == 1 ? "" : "s")
                                + " · " + taskCount + " task" + (taskCount == 1 ? "" : "s")
                                + " · " + multiCount + " MultiTask"
                                + " · " + formatRamShort(ramBytes));
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

    private List<AppEntry> loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, AppEntry> unique = new LinkedHashMap<>();

        for (ResolveInfo info : pm.queryIntentActivities(query, PackageManager.MATCH_ALL)) {
            if (info.activityInfo == null) continue;
            ApplicationInfo ai = info.activityInfo.applicationInfo;
            if (getPackageName().equals(ai.packageName)) continue;

            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system && !SystemAppAllowlist.isAllowed(ai.packageName)) continue;

            String activity = info.activityInfo.name;
            try {
                Intent launch = pm.getLaunchIntentForPackage(ai.packageName);
                if (launch != null && launch.getComponent() != null) {
                    activity = launch.getComponent().getClassName();
                }
            } catch (Throwable ignored) {}

            CharSequence label = info.loadLabel(pm);
            AppEntry entry = new AppEntry(
                    label == null ? ai.packageName : label.toString(),
                    ai.packageName,
                    activity,
                    info.loadIcon(pm));
            unique.putIfAbsent(ai.packageName, entry);
        }

        ArrayList<AppEntry> out = new ArrayList<>(unique.values());
        Collator collator = Collator.getInstance();
        out.sort((a, b) -> collator.compare(a.label, b.label));
        return out;
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
