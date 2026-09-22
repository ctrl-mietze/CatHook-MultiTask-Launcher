package com.catcore.ctrlmietze.multitask;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TaskManagerActivity extends AppCompatActivity {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private LinearLayout listHost;
    private TextView status;
    private boolean multiOnly;
    private List<TaskInspector.TaskInfo> current = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Task Manager");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(10, 12, 17));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text("Task Manager", 26, Color.WHITE, true);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button refresh = button("Refresh");
        refresh.setOnClickListener(v -> refresh());
        top.addView(refresh);

        TextView hint = text("Running user-app tasks only. System apps are hidden.", 13,
                Color.rgb(158, 169, 190), false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(5);
        root.addView(hint, hintParams);

        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.HORIZONTAL);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(-1, -2);
        modeParams.topMargin = dp(16);
        root.addView(modes, modeParams);

        RadioButton all = new RadioButton(this);
        all.setId(View.generateViewId());
        all.setText("View All");
        all.setTextColor(Color.WHITE);
        all.setChecked(true);
        modes.addView(all, new RadioGroup.LayoutParams(0, -2, 1));

        RadioButton multi = new RadioButton(this);
        multi.setId(View.generateViewId());
        multi.setText("View MultiTask");
        multi.setTextColor(Color.WHITE);
        modes.addView(multi, new RadioGroup.LayoutParams(0, -2, 1));

        modes.setOnCheckedChangeListener((group, checkedId) -> {
            multiOnly = checkedId == multi.getId();
            render();
        });

        Button closeMulti = button("Close all MultiTask");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(48));
        closeParams.topMargin = dp(12);
        root.addView(closeMulti, closeParams);
        closeMulti.setOnClickListener(v -> confirmCollapse());

        status = text("Reading running tasks…", 13, Color.rgb(158, 169, 190), false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(14);
        root.addView(status, statusParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(8);
        root.addView(scroll, scrollParams);

        listHost = new LinearLayout(this);
        listHost.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listHost, new ScrollView.LayoutParams(-1, -2));

        setContentView(root);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (listHost != null) refresh();
    }

    private void refresh() {
        status.setText("Reading running tasks…");
        exec.execute(() -> {
            List<TaskInspector.TaskInfo> tasks = TaskInspector.readUserTasks(this);
            runOnUiThread(() -> {
                current = tasks;
                render();
            });
        });
    }

    private void render() {
        if (listHost == null) return;
        listHost.removeAllViews();

        Map<String, Integer> counts = TaskInspector.countByPackage(current);
        Map<String, TaskInspector.TaskInfo> first = new LinkedHashMap<>();
        for (TaskInspector.TaskInfo task : current) first.putIfAbsent(task.packageName, task);

        int shown = 0;
        for (Map.Entry<String, TaskInspector.TaskInfo> entry : first.entrySet()) {
            String pkg = entry.getKey();
            int count = counts.getOrDefault(pkg, 1);
            if (multiOnly && count < 2) continue;
            shown++;
            listHost.addView(taskRow(entry.getValue(), count));
        }

        status.setText(shown + " app" + (shown == 1 ? "" : "s")
                + " · " + current.size() + " active task"
                + (current.size() == 1 ? "" : "s"));

        if (shown == 0) {
            TextView empty = text(multiOnly
                            ? "No app currently has more than one task."
                            : "No running user-app tasks were found.",
                    14, Color.rgb(158, 169, 190), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            listHost.addView(empty);
        }
    }

    private View taskRow(TaskInspector.TaskInfo task, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        row.setBackground(shape(Color.rgb(22, 26, 36), dp(18)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        try {
            Drawable d = getPackageManager().getApplicationIcon(task.packageName);
            icon.setImageDrawable(d);
        } catch (Throwable ignored) {}
        row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelsParams.leftMargin = dp(12);
        row.addView(labels, labelsParams);

        labels.addView(text(task.label, 16, Color.WHITE, true));
        labels.addView(text(count + " running task" + (count == 1 ? "" : "s"),
                12, Color.rgb(151, 164, 188), false));

        Button open = button("Open windows");
        open.setOnClickListener(v -> askWindowCount(task.packageName));
        row.addView(open);
        return row;
    }

    private void askWindowCount(String pkg) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1–8");
        input.setText("2");
        input.setSelectAllOnFocus(true);
        int pad = dp(20);
        input.setPadding(pad, dp(10), pad, dp(10));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Open windows")
                .setMessage("How many tasks should MultiTask open? Maximum: 8.")
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
                        input.setError("Enter a number from 1 to 8");
                        return;
                    }
                    dialog.dismiss();
                    status.setText("Opening " + number + " tasks…");
                    TaskLauncher.launchMultiple(this, pkg, preferredActivity(pkg), number,
                            msg -> status.setText(msg),
                            (requested, started, message) -> {
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                refresh();
                            });
                }));
        dialog.show();
    }

    private void confirmCollapse() {
        Map<String, Integer> counts = TaskInspector.countByPackage(current);
        List<String> packages = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > 1) packages.add(e.getKey());
        }

        if (packages.isEmpty()) {
            Toast.makeText(this, "No duplicate tasks are open.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Close all MultiTask?")
                .setMessage("Duplicate-task apps will be stopped once and one normal instance will be reopened.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Close duplicates", (d, which) -> collapse(packages))
                .show();
    }

    private void collapse(List<String> packages) {
        status.setText("Closing duplicate tasks…");
        exec.execute(() -> {
            for (String pkg : packages) {
                RootShell.run("am force-stop --user current " + RootShell.quote(pkg), 6);
            }
            runOnUiThread(() -> {
                for (String pkg : packages) {
                    TaskLauncher.launchNewTask(this, pkg, preferredActivity(pkg), null);
                }
                Toast.makeText(this, "Duplicate tasks closed.", Toast.LENGTH_SHORT).show();
                listHost.postDelayed(this::refresh, 700L);
            });
        });
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

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(shape(Color.rgb(79, 108, 231), dp(14)));
        return b;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
