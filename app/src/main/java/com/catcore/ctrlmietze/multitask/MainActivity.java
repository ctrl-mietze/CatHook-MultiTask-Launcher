package com.catcore.ctrlmietze.multitask;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

public final class MainActivity extends AppCompatActivity {
    private TextView statusTitle;
    private TextView statusHint;
    private View statusDot;
    private AlertDialog compatibilityDialog;
    private TextView compatibilityText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        List<AppEntry> apps = loadApps();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), 0);
        root.setBackgroundColor(Color.rgb(10, 12, 17));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        header.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1));

        TextView title = text("MultiTask", 30, Color.WHITE, true);
        headerText.addView(title);

        TextView subtitle = text("Open apps inside independent live windows.", 13,
                Color.rgb(160, 171, 192), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(2);
        headerText.addView(subtitle, subtitleParams);

        Button settings = button("⚙");
        settings.setContentDescription("Settings");
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        header.addView(settings, settingsParams);
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.topMargin = dp(16);
        root.addView(actions, actionsParams);

        Button taskManager = button("Task Manager");
        LinearLayout.LayoutParams managerParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        actions.addView(taskManager, managerParams);
        taskManager.setOnClickListener(v -> {
            if (!isXposedActive()) {
                showXposedRequired();
                return;
            }
            startActivity(new Intent(this, TaskManagerActivity.class));
        });

        Button quickSettings = button("Settings");
        LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        quickParams.leftMargin = dp(10);
        actions.addView(quickSettings, quickParams);
        quickSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(15), dp(14), dp(15), dp(14));
        status.setBackground(shape(Color.rgb(21, 25, 34), dp(18)));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(14);
        root.addView(status, statusParams);

        statusDot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.rightMargin = dp(12);
        status.addView(statusDot, dotParams);

        LinearLayout statusTexts = new LinearLayout(this);
        statusTexts.setOrientation(LinearLayout.VERTICAL);
        status.addView(statusTexts, new LinearLayout.LayoutParams(0, -2, 1));

        statusTitle = text("", 15, Color.WHITE, true);
        statusTexts.addView(statusTitle);

        statusHint = text("", 12, Color.rgb(151, 164, 188), false);
        LinearLayout.LayoutParams statusHintParams = new LinearLayout.LayoutParams(-1, -2);
        statusHintParams.topMargin = dp(3);
        statusTexts.addView(statusHint, statusHintParams);
        updateXposedStatus();

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchRowParams = new LinearLayout.LayoutParams(-1, -2);
        searchRowParams.topMargin = dp(18);
        root.addView(searchRow, searchRowParams);

        TextView appHeading = text("Apps", 18, Color.WHITE, true);
        searchRow.addView(appHeading, new LinearLayout.LayoutParams(0, -2, 1));

        TextView count = text(String.valueOf(apps.size()), 13,
                Color.rgb(142, 156, 183), true);
        count.setGravity(Gravity.CENTER);
        count.setBackground(shape(Color.rgb(29, 35, 49), dp(12)));
        searchRow.addView(count, new LinearLayout.LayoutParams(dp(48), dp(30)));

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search apps or packages");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.rgb(128, 141, 166));
        search.setTextSize(14);
        search.setPadding(dp(15), 0, dp(15), 0);
        search.setBackground(shape(Color.rgb(27, 32, 44), dp(15)));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(50));
        searchParams.topMargin = dp(10);
        searchParams.bottomMargin = dp(6);
        root.addView(search, searchParams);

        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setClipToPadding(false);
        list.setPadding(0, 0, 0, dp(16));
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

        if (SettingsStore.restoreRuntime(this)) {
            RuntimeTuning.applyAsync(this, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusTitle != null) updateXposedStatus();
    }

    public boolean isXposedActive() {
        return false;
    }

    public void showXposedRequired() {
        new AlertDialog.Builder(this)
                .setTitle("LSPosed required")
                .setMessage("Enable MultiTask in LSPosed and keep MultiTask + System Framework in its scope. "
                        + "You do not need to select every target app.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open settings", (d, w) ->
                        startActivity(new Intent(this, SettingsActivity.class)))
                .show();
    }

    public void showCompatibilityProgress(String message) {
        if (compatibilityDialog == null) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.HORIZONTAL);
            box.setGravity(Gravity.CENTER_VERTICAL);
            box.setPadding(dp(20), dp(14), dp(20), dp(14));

            ProgressBar progress = new ProgressBar(this);
            box.addView(progress, new LinearLayout.LayoutParams(dp(32), dp(32)));

            compatibilityText = text(message, 13, Color.WHITE, false);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
            textParams.leftMargin = dp(14);
            box.addView(compatibilityText, textParams);

            compatibilityDialog = new AlertDialog.Builder(this)
                    .setTitle("Compatibility check")
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

    private void updateXposedStatus() {
        boolean active = isXposedActive();
        statusDot.setBackground(shape(
                active ? Color.rgb(52, 211, 153) : Color.rgb(244, 105, 117), dp(6)));
        statusTitle.setText(active ? "LSPosed connected" : "LSPosed is not active");
        statusHint.setText(active
                ? "V2 window framework ready. Apps open inside MultiTask-owned displays."
                : "Enable the module first. Recommended scope: MultiTask + System Framework.");
    }

    private List<AppEntry> loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, AppEntry> unique = new LinkedHashMap<>();

        for (ResolveInfo info : pm.queryIntentActivities(query, PackageManager.MATCH_ALL)) {
            if (info.activityInfo == null) continue;
            ApplicationInfo ai = info.activityInfo.applicationInfo;
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (getPackageName().equals(ai.packageName)) continue;

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

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(Color.rgb(76, 105, 229), dp(14)));
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
