package com.catcore.ctrlmietze.multitask;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public final class AppStarterActivity extends AppCompatActivity {
    private volatile boolean rootReady;
    private AlertDialog compatibilityDialog;
    private TextView compatibilityText;
    private AppAdapter adapter;
    private TextView appCount;
    private EditText searchBox;
    private BroadcastReceiver catalogReceiver;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CatUi.applyWindow(this);
        rootReady = EnvironmentProbe.hasRoot();

        List<AppEntry> apps = AppCatalog.loadCached(this);

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

        titles.addView(CatUi.text(this, "App Starter", 28, CatUi.TEXT, true));
        titles.addView(CatUi.text(this,
                "Choose an app and its exact task target",
                12, CatUi.MUTED, false));

        appCount = CatUi.pill(this,
                apps.isEmpty() ? "…" : String.valueOf(apps.size()),
                Color.rgb(48, 57, 82));
        header.addView(appCount, new LinearLayout.LayoutParams(dp(58), dp(34)));

        LinearLayout info = CatUi.card(this);
        info.setBackground(CatUi.dashboard(this));
        LinearLayout.LayoutParams ip = CatUi.cardParams(this);
        ip.topMargin = dp(14);
        root.addView(info, ip);

        info.addView(CatUi.text(this,
                "Start apps your way", 18, CatUi.TEXT, true));
        TextView hint = CatUi.text(this,
                "Tap ×1…×8 to set the target task count, then START. "
                        + "The selected V2 mode is used automatically.",
                12, Color.rgb(194, 205, 231), false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(5);
        info.addView(hint, hp);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Search apps or packages");
        searchBox.setTextColor(CatUi.TEXT);
        searchBox.setHintTextColor(Color.rgb(118, 131, 156));
        searchBox.setTextSize(14);
        searchBox.setPadding(dp(16), 0, dp(16), 0);
        searchBox.setBackground(CatUi.stroke(
                this, CatUi.SURFACE, 18, Color.rgb(42, 49, 69)));
        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.topMargin = dp(12);
        searchParams.bottomMargin = dp(4);
        root.addView(searchBox, searchParams);

        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setClipToPadding(false);
        list.setPadding(0, dp(2), 0, dp(20));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        adapter = new AppAdapter(this, apps);
        list.setAdapter(adapter);

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
            }
            public void afterTextChanged(android.text.Editable editable) {}
        });

        registerCatalogReceiver();

        if (apps.isEmpty()) {
            AppCatalog.refreshAsync(this, true, this::applyCatalog);
        } else if (SettingsStore.frameworkEnabled(this)) {
            // Framework refreshes out-of-band; the cached list is already visible.
            CatCoreFrameworkService.requestCatalogRefresh(this);
        } else if (AppCatalog.needsRefresh(this)) {
            AppCatalog.refreshAsync(this, true, this::applyCatalog);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> rootReady = EnvironmentProbe.hasRoot(), "CatCore-root-probe").start();

        List<AppEntry> cached = AppCatalog.loadCached(this);
        if (!cached.isEmpty() && adapter != null) {
            applyCatalog(cached);
        }
    }

    @Override
    protected void onDestroy() {
        if (catalogReceiver != null) {
            try { unregisterReceiver(catalogReceiver); } catch (Throwable ignored) {}
            catalogReceiver = null;
        }
        super.onDestroy();
    }

    private void registerCatalogReceiver() {
        catalogReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<AppEntry> latest = AppCatalog.loadCached(AppStarterActivity.this);
                if (!latest.isEmpty()) applyCatalog(latest);
            }
        };

        IntentFilter filter = new IntentFilter(AppCatalog.ACTION_UPDATED);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                registerReceiver(catalogReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(catalogReceiver, filter);
            }
        } catch (Throwable ignored) {
            catalogReceiver = null;
        }
    }

    public boolean isLaunchFrameworkReady() {
        if (!rootReady) rootReady = EnvironmentProbe.hasRoot();
        return rootReady
                && EnvironmentProbe.isXposedActive()
                && EnvironmentProbe.isSystemHookActive(this);
    }

    public void showFrameworkRequired() {
        boolean xposed = EnvironmentProbe.isXposedActive();
        boolean systemHook = EnvironmentProbe.isSystemHookActive(this);
        int targetStep = !rootReady ? 1 : (!xposed ? 2 : 3);

        String title;
        String message;

        if (!rootReady) {
            title = "Root access required";
            message = "MultiTask no longer has an active root grant. Re-enable it in your root manager and verify it in setup.";
        } else if (!xposed) {
            title = "LSPosed activation required";
            message = "MultiTask's built-in LSPosed module is not active. Enable MultiTask and keep MultiTask + System Framework in scope.";
        } else if (!systemHook) {
            title = "System hook required";
            message = "The current system_server does not contain the matching MultiTask hook. Open setup and use Soft reboot after checking the System Framework scope.";
        } else {
            title = "Framework unavailable";
            message = "MultiTask could not verify the current launch framework.";
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
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
            LinearLayout.LayoutParams textParams =
                    new LinearLayout.LayoutParams(0, -2, 1);
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
                .setTitle("Exact task target")
                .setMessage("How many tasks should exist when you press START for "
                        + app.label + "?")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null)
                .create();

        dialog.setOnShowListener(x ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            int value;
                            try {
                                value = Integer.parseInt(
                                        input.getText().toString().trim());
                            } catch (Throwable t) {
                                value = 0;
                            }

                            if (value < 1 || value > 8) {
                                input.setError("Choose 1–8");
                                return;
                            }

                            AppTaskRules.set(this, app.packageName, value);
                            dialog.dismiss();
                            if (onChanged != null) onChanged.run();
                        }));

        dialog.show();
    }

    private void applyCatalog(List<AppEntry> apps) {
        if (apps == null || adapter == null || appCount == null) return;
        adapter.replaceAll(apps);
        String query = searchBox == null || searchBox.getText() == null
                ? "" : searchBox.getText().toString();
        adapter.filter(query);
        appCount.setText(String.valueOf(apps.size()));
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
