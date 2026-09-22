package com.catcore.ctrlmietze.multitask;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.catcore.ctrlmietze.multitask.window.WindowFramework;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TaskManagerActivity extends AppCompatActivity {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler autoRefresh = new Handler(Looper.getMainLooper());
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            refresh(false);
            autoRefresh.postDelayed(this, 2500L);
        }
    };

    private LinearLayout listHost;
    private TextView summary;
    private TextView allTab;
    private TextView multiTab;
    private TextView metricApps;
    private TextView metricTasks;
    private TextView metricMulti;
    private TextView metricRam;
    private boolean multiOnly;
    private boolean loading;
    private List<TaskInspector.TaskInfo> current = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CatUi.applyWindow(this);
        buildUi();
        refresh(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoRefresh.removeCallbacks(refreshTick);
        autoRefresh.postDelayed(refreshTick, 1200L);
    }

    @Override
    protected void onPause() {
        autoRefresh.removeCallbacks(refreshTick);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), 0);
        root.setBackground(CatUi.background());

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        Button back = CatUi.secondaryButton(this, "‹");
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        back.setOnClickListener(v -> finish());

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.leftMargin = dp(12);
        header.addView(titles, tp);
        titles.addView(CatUi.text(this, "Task Manager", 28, CatUi.TEXT, true));
        titles.addView(CatUi.text(this,
                "Live user-app tasks and process resources",
                12, CatUi.MUTED, false));

        Button refresh = CatUi.secondaryButton(this, "↻");
        header.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(46)));
        refresh.setOnClickListener(v -> refresh(true));

        LinearLayout hero = CatUi.card(this);
        hero.setBackground(CatUi.hero(this));
        LinearLayout.LayoutParams hp = CatUi.cardParams(this);
        hp.topMargin = dp(16);
        root.addView(hero, hp);

        summary = CatUi.text(this, "Reading Android tasks…", 18, CatUi.TEXT, true);
        hero.addView(summary);
        TextView heroHint = CatUi.text(this,
                "CPU is process CPU usage; RAM is resident memory across the app's current processes.",
                11, Color.rgb(199, 208, 232), false);
        LinearLayout.LayoutParams hhp = new LinearLayout.LayoutParams(-1, -2);
        hhp.topMargin = dp(6);
        hero.addView(heroHint, hhp);

        LinearLayout metricRow = new LinearLayout(this);
        metricRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mrp = new LinearLayout.LayoutParams(-1, dp(62));
        mrp.topMargin = dp(14);
        hero.addView(metricRow, mrp);

        metricApps = addHeroMetric(metricRow, "APPS");
        metricTasks = addHeroMetric(metricRow, "TASKS");
        metricMulti = addHeroMetric(metricRow, "MULTI");
        metricRam = addHeroMetric(metricRow, "RAM");

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(CatUi.shape(this, CatUi.SURFACE, 18));
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, dp(50));
        tabsParams.topMargin = dp(14);
        root.addView(tabs, tabsParams);

        allTab = tab("View all", true);
        multiTab = tab("View MultiTask", false);
        tabs.addView(allTab, new LinearLayout.LayoutParams(0, -1, 1));
        tabs.addView(multiTab, new LinearLayout.LayoutParams(0, -1, 1));

        allTab.setOnClickListener(v -> {
            multiOnly = false;
            updateTabs();
            render();
        });
        multiTab.setOnClickListener(v -> {
            multiOnly = true;
            updateTabs();
            render();
        });

        Button closeAll = CatUi.secondaryButton(this, "Close all MultiTask duplicates");
        LinearLayout.LayoutParams cap = new LinearLayout.LayoutParams(-1, dp(50));
        cap.topMargin = dp(10);
        root.addView(closeAll, cap);
        closeAll.setOnClickListener(v -> confirmCloseAll());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(8);
        root.addView(scroll, scrollParams);

        listHost = new LinearLayout(this);
        listHost.setOrientation(LinearLayout.VERTICAL);
        listHost.setPadding(0, 0, 0, dp(24));
        scroll.addView(listHost, new ScrollView.LayoutParams(-1, -2));

        setContentView(root);
    }

    private TextView addHeroMetric(LinearLayout row, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(CatUi.shape(this, Color.argb(100, 20, 26, 48), 15));

        TextView value = CatUi.text(this, "--", 16, CatUi.TEXT, true);
        value.setGravity(Gravity.CENTER);
        box.addView(value);

        TextView caption = CatUi.text(
                this, label, 8, Color.rgb(155, 171, 214), true);
        caption.setLetterSpacing(0.08f);
        caption.setGravity(Gravity.CENTER);
        box.addView(caption);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        p.rightMargin = dp(6);
        row.addView(box, p);
        return value;
    }

    private TextView tab(String label, boolean active) {
        TextView t = CatUi.text(this, label, 13, CatUi.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(CatUi.shape(this,
                active ? Color.rgb(62, 76, 158) : Color.TRANSPARENT, 14));
        return t;
    }

    private void updateTabs() {
        allTab.setBackground(CatUi.shape(this,
                multiOnly ? Color.TRANSPARENT : Color.rgb(62, 76, 158), 14));
        multiTab.setBackground(CatUi.shape(this,
                multiOnly ? Color.rgb(62, 76, 158) : Color.TRANSPARENT, 14));
    }

    private void refresh(boolean explicit) {
        if (loading) return;
        loading = true;
        if (explicit) summary.setText("Reading Android tasks…");

        exec.execute(() -> {
            List<TaskInspector.TaskInfo> tasks = TaskInspector.readUserTasks(this);
            runOnUiThread(() -> {
                loading = false;
                current = tasks;
                render();
            });
        });
    }

    private void render() {
        if (listHost == null) return;
        listHost.removeAllViews();

        Map<String, List<TaskInspector.TaskInfo>> grouped = new LinkedHashMap<>();
        for (TaskInspector.TaskInfo task : current) {
            grouped.computeIfAbsent(task.packageName, key -> new ArrayList<>()).add(task);
        }

        int appCount = 0;
        int multiCount = 0;
        long totalRam = 0L;
        double totalCpu = 0d;

        for (Map.Entry<String, List<TaskInspector.TaskInfo>> entry : grouped.entrySet()) {
            List<TaskInspector.TaskInfo> tasks = entry.getValue();
            TaskInspector.TaskInfo first = tasks.get(0);
            boolean managed = tasks.size() > 1 || hasSecondaryDisplay(tasks);
            if (managed) multiCount++;
            if (multiOnly && !managed) continue;

            appCount++;
            totalRam += first.rssBytes;
            totalCpu += first.cpuPercent;
            listHost.addView(taskRow(first, tasks));
        }

        summary.setText("Live Android task state · "
                + String.format(Locale.US, "%.1f%% CPU", totalCpu));

        metricApps.setText(String.valueOf(appCount));
        metricTasks.setText(String.valueOf(current.size()));
        metricMulti.setText(String.valueOf(multiCount));
        metricRam.setText(formatRamCompact(totalRam));

        if (appCount == 0) {
            LinearLayout empty = CatUi.card(this);
            LinearLayout.LayoutParams ep = CatUi.cardParams(this);
            ep.topMargin = dp(18);
            listHost.addView(empty, ep);

            TextView text = CatUi.text(this,
                    multiOnly
                            ? "No app currently has multiple or MultiTask-managed tasks."
                            : "No running user-app tasks were found yet.",
                    14, CatUi.MUTED, false);
            text.setGravity(Gravity.CENTER);
            text.setPadding(dp(10), dp(30), dp(10), dp(30));
            empty.addView(text);
        }
    }

    private View taskRow(TaskInspector.TaskInfo first, List<TaskInspector.TaskInfo> tasks) {
        LinearLayout card = CatUi.card(this);
        LinearLayout.LayoutParams cp = CatUi.cardParams(this);
        cp.topMargin = dp(7);
        card.setLayoutParams(cp);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);

        ImageView icon = new ImageView(this);
        try {
            Drawable d = getPackageManager().getApplicationIcon(first.packageName);
            icon.setImageDrawable(d);
        } catch (Throwable ignored) {
        }
        top.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.leftMargin = dp(12);
        top.addView(labels, lp);

        labels.addView(CatUi.text(this, first.label, 16, CatUi.TEXT, true));
        TextView pkg = CatUi.text(this, first.packageName, 10, CatUi.MUTED, false);
        pkg.setSingleLine(true);
        labels.addView(pkg);

        top.addView(CatUi.pill(this,
                tasks.size() + (tasks.size() == 1 ? " TASK" : " TASKS"),
                tasks.size() > 1 ? Color.rgb(60, 75, 145) : Color.rgb(43, 50, 68)));

        LinearLayout metrics = new LinearLayout(this);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = dp(12);
        card.addView(metrics, mp);

        addMetric(metrics, "RAM", formatRam(first.rssBytes));
        addMetric(metrics, "CPU", String.format(Locale.US, "%.1f%%", first.cpuPercent));
        addMetric(metrics, "PROC", String.valueOf(first.processCount));
        addMetric(metrics, "DISPLAY", displaySummary(tasks));

        LinearLayout actions = new LinearLayout(this);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(12);
        card.addView(actions, ap);

        Button open = CatUi.primaryButton(this, "Open more");
        actions.addView(open, new LinearLayout.LayoutParams(0, dp(48), 1));
        open.setOnClickListener(v -> askWindowCount(first.packageName, tasks.size()));

        Button manage = CatUi.secondaryButton(this, "Manage");
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        manageParams.leftMargin = dp(8);
        actions.addView(manage, manageParams);
        manage.setOnClickListener(v -> showTaskDetails(first, tasks));

        return card;
    }

    private void addMetric(LinearLayout row, String label, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(5), dp(6), dp(5), dp(6));
        box.setBackground(CatUi.shape(this, CatUi.SURFACE_2, 12));

        box.addView(CatUi.text(this, label, 9, Color.rgb(126, 140, 169), true));
        TextView v = CatUi.text(this, value, 12, CatUi.TEXT, true);
        v.setGravity(Gravity.CENTER);
        box.addView(v);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
        p.rightMargin = dp(5);
        row.addView(box, p);
    }

    private void askWindowCount(String pkg, int currentCount) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText("2");
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(20), dp(8), dp(20), dp(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Open more")
                .setMessage("How many additional tasks/windows should MultiTask open? Maximum: 8.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int number;
                    try { number = Integer.parseInt(input.getText().toString().trim()); }
                    catch (Throwable t) { number = 0; }

                    if (number < 1 || number > 8) {
                        input.setError("Choose 1–8");
                        return;
                    }
                    dialog.dismiss();
                    openMore(pkg, currentCount, number);
                }));

        dialog.show();
    }

    private void openMore(String pkg, int currentCount, int number) {
        String activity = preferredActivity(pkg);

        if (SettingsStore.startMode(this) == SettingsStore.MODE_MY_TASK) {
            String label = pkg;
            try {
                CharSequence l = getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(pkg, 0));
                if (l != null) label = l.toString();
            } catch (Throwable ignored) {
            }

            final String finalLabel = label;
            for (int i = 0; i < number; i++) {
                int delay = i * 150;
                autoRefresh.postDelayed(() ->
                        WindowFramework.open(this, pkg, activity, finalLabel), delay);
            }
            autoRefresh.postDelayed(() -> refresh(true), number * 170L + 500L);
            return;
        }

        int target = Math.max(1, Math.min(8,
                Math.max(0, currentCount) + number));

        SystemTaskBridge.ensureTaskCount(
                this,
                pkg,
                activity,
                target,
                (ok, before, after, desired, message) -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    refresh(true);
                });
    }

    private void showTaskDetails(TaskInspector.TaskInfo first, List<TaskInspector.TaskInfo> tasks) {
        StringBuilder detail = new StringBuilder();
        for (TaskInspector.TaskInfo task : tasks) {
            detail.append("Task ").append(task.taskId)
                    .append(" · display ").append(task.displayId)
                    .append("\n");
        }
        detail.append("\nRAM ").append(formatRam(first.rssBytes))
                .append(" · CPU ")
                .append(String.format(Locale.US, "%.1f%%", first.cpuPercent))
                .append(" · ")
                .append(first.processCount).append(" process")
                .append(first.processCount == 1 ? "" : "es");

        new AlertDialog.Builder(this)
                .setTitle(first.label)
                .setMessage(detail.toString())
                .setNegativeButton("Close", null)
                .setNeutralButton("Force stop", (d, w) -> forceStop(first.packageName))
                .setPositiveButton("Close duplicate tasks", (d, w) ->
                        closeExtras(first.packageName, tasks))
                .show();
    }

    private void closeExtras(String pkg, List<TaskInspector.TaskInfo> tasks) {
        if (tasks.size() <= 1) {
            Toast.makeText(this, "No duplicate tasks for this app.", Toast.LENGTH_SHORT).show();
            return;
        }

        exec.execute(() -> {
            int keepTaskId = chooseTaskToKeep(tasks);
            for (TaskInspector.TaskInfo task : tasks) {
                if (task.taskId == keepTaskId) continue;
                TaskInspector.removeTask(task.taskId);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Duplicate tasks closed.", Toast.LENGTH_SHORT).show();
                refresh(true);
            });
        });
    }

    private void forceStop(String pkg) {
        exec.execute(() -> {
            RootShell.Result result = RootShell.run(
                    "am force-stop --user current " + RootShell.quote(pkg), 6);
            runOnUiThread(() -> {
                Toast.makeText(this,
                        result.ok ? "App stopped." : RootShell.shortReason(result),
                        Toast.LENGTH_LONG).show();
                refresh(true);
            });
        });
    }

    private void confirmCloseAll() {
        Map<String, List<TaskInspector.TaskInfo>> grouped = new LinkedHashMap<>();
        for (TaskInspector.TaskInfo task : current) {
            grouped.computeIfAbsent(task.packageName, key -> new ArrayList<>()).add(task);
        }

        int duplicateApps = 0;
        for (List<TaskInspector.TaskInfo> tasks : grouped.values()) {
            if (tasks.size() > 1 || hasSecondaryDisplay(tasks)) duplicateApps++;
        }

        if (duplicateApps == 0) {
            Toast.makeText(this, "No MultiTask duplicates are open.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Close MultiTask duplicates?")
                .setMessage("Keeps one normal display task per app when possible and removes additional managed tasks.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Close duplicates", (d, w) -> closeAllDuplicates(grouped))
                .show();
    }

    private void closeAllDuplicates(Map<String, List<TaskInspector.TaskInfo>> grouped) {
        exec.execute(() -> {
            for (List<TaskInspector.TaskInfo> tasks : grouped.values()) {
                if (tasks.size() <= 1 && !hasSecondaryDisplay(tasks)) continue;

                int keepTaskId = chooseTaskToKeep(tasks);
                for (TaskInspector.TaskInfo task : tasks) {
                    if (task.taskId == keepTaskId) continue;
                    TaskInspector.removeTask(task.taskId);
                }
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "MultiTask duplicates closed.", Toast.LENGTH_SHORT).show();
                refresh(true);
            });
        });
    }

    private static int chooseTaskToKeep(List<TaskInspector.TaskInfo> tasks) {
        if (tasks == null || tasks.isEmpty()) return -1;
        for (TaskInspector.TaskInfo task : tasks) {
            if (task.displayId == 0) return task.taskId;
        }
        return tasks.get(0).taskId;
    }

    private static boolean hasSecondaryDisplay(List<TaskInspector.TaskInfo> tasks) {
        for (TaskInspector.TaskInfo task : tasks) {
            if (task.displayId > 0) return true;
        }
        return false;
    }

    private static String displaySummary(List<TaskInspector.TaskInfo> tasks) {
        int secondary = 0;
        for (TaskInspector.TaskInfo task : tasks) {
            if (task.displayId > 0) secondary++;
        }
        return secondary > 0 ? secondary + " V2" : "MAIN";
    }

    private String preferredActivity(String pkg) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            ComponentName component = launch == null ? null : launch.getComponent();
            return component == null ? null : component.getClassName();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String formatRamCompact(long bytes) {
        if (bytes <= 0L) return "--";
        double mib = bytes / 1048576d;
        if (mib >= 1024d) {
            return String.format(Locale.US, "%.1fG", mib / 1024d);
        }
        return String.format(Locale.US, "%.0fM", mib);
    }

    private static String formatRam(long bytes) {
        if (bytes <= 0L) return "—";
        double mb = bytes / (1024d * 1024d);
        if (mb >= 1024d) return String.format(Locale.US, "%.1f GB", mb / 1024d);
        return String.format(Locale.US, "%.0f MB", mb);
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
