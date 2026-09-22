package com.catcore.ctrlmietze.multitask;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public final class SettingsActivity extends AppCompatActivity {
    private LinearLayout root;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable runtimeApply;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CatUi.applyWindow(this);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(CatUi.background());

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(38));
        scroll.addView(root);

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

        titles.addView(CatUi.text(this, "Settings", 28, CatUi.TEXT, true));
        titles.addView(CatUi.text(this, "Changes apply instantly", 12, CatUi.MUTED, false));

        addSection("MAIN START METHOD");
        addModeCard(
                "Start as my task",
                "Stable framework mode",
                "Hosts launches through the CatCore MultiTask framework and V2 window/task layer.",
                SettingsStore.MODE_MY_TASK);
        addModeCard(
                "Start as app's own task",
                "Native app task",
                "Uses MultiTask's native LSPosed system_server bridge first. Root launch paths stay behind explicit Max Stability compatibility mode.",
                SettingsStore.MODE_APP_OWN_TASK);

        addSection("CATCORE FRAMEWORK");
        LinearLayout framework = CatUi.card(this);
        root.addView(framework, CatUi.cardParams(this));

        SwitchCompat frameworkToggle = new SwitchCompat(this);
        frameworkToggle.setText("Keep MultiTask Framework active");
        frameworkToggle.setTextColor(CatUi.TEXT);
        frameworkToggle.setTextSize(16);
        frameworkToggle.setChecked(SettingsStore.frameworkEnabled(this));
        framework.addView(frameworkToggle);

        TextView frameworkHint = CatUi.text(this,
                "Runs the dedicated :framework process used by live windows, task sessions and framework notifications. Recommended.",
                12, CatUi.MUTED, false);
        LinearLayout.LayoutParams fhp = new LinearLayout.LayoutParams(-1, -2);
        fhp.topMargin = dp(6);
        framework.addView(frameworkHint, fhp);

        frameworkToggle.setOnCheckedChangeListener((button, checked) -> {
            SettingsStore.setFrameworkEnabled(this, checked);
            if (checked) {
                try { CatCoreFrameworkService.start(this); }
                catch (Throwable t) {
                    Toast.makeText(this, "Framework could not start: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            } else {
                button.setChecked(true);
                CatDialog.show(this,
                        "FRAMEWORK PROTECTION",
                        "Disable CatCore Framework?",
                        "Not recommended. App catalog caching, live framework sessions, Stability Guard and recovery monitoring will stop until the framework is enabled again.",
                        "Keep active",
                        "Disable anyway",
                        () -> {
                            button.setOnCheckedChangeListener(null);
                            button.setChecked(false);
                            SettingsStore.setFrameworkEnabled(this, false);
                            stopService(new Intent(this, CatCoreFrameworkService.class));
                        });
            }
        });

        addSwitch(
                "Restore live workspace after process death",
                "Keeps only app/window session metadata in app preferences. Virtual displays themselves are recreated; no Android system files are modified.",
                SettingsStore.restoreWindows(this),
                value -> SettingsStore.setRestoreWindows(this, value));

        addSection("COMPATIBILITY");
        addSwitch(
                "Compatibility mode",
                "Only needed for apps that do not open reliably. MultiTask performs a short activity analysis and unlocks additional fallbacks.",
                SettingsStore.compatibilityMode(this),
                value -> SettingsStore.setCompatibilityMode(this, value));

        addSwitch(
                "Full-scan compatibility",
                "Off by default. CatCore Framework inventories launcher activities and task-relevant manifest behavior for user apps and the supported Google/system-app allowlist, then stores the report only in MultiTask app data.",
                SettingsStore.fullScanMode(this),
                value -> {
                    SettingsStore.setFullScanMode(this, value);
                    if (value && SettingsStore.frameworkEnabled(this)) {
                        CatCoreFrameworkService.requestCompatibilityScan(this);
                    }
                });

        addSwitch(
                "Max Stability",
                "Uses the full launch chain and stronger LSPosed reinforcement for difficult apps.",
                SettingsStore.maxStability(this),
                value -> SettingsStore.setMaxStability(this, value));

        addSwitch(
                "Open apps as child tasks",
                "Experimental native-task behavior. Apps can still override it with their own manifest launchMode.",
                SettingsStore.childTasks(this),
                value -> SettingsStore.setChildTasks(this, value));

        addSection("RUNTIME");
        addSwitch(
                "Restore runtime values after reboot",
                "Re-applies the values below once Android has booted. No system partition files are replaced.",
                SettingsStore.restoreRuntime(this),
                value -> SettingsStore.setRestoreRuntime(this, value));

        addSwitch(
                "Manual process management",
                "Off = CatCore Framework manages process compatibility automatically. On = the controls below become editable. Turning it off hands control back to the framework.",
                SettingsStore.manualProcessTuning(this),
                value -> {
                    SettingsStore.setManualProcessTuning(this, value);
                    RuntimeTuning.applyAsync(this, true, null);
                    build();
                });

        boolean manualProcesses = SettingsStore.manualProcessTuning(this);

        addRuntimeNumber(
                "Max cached processes",
                "0 = Android default · 0–512",
                SettingsStore.maxCachedProcesses(this),
                0, 512, 0, manualProcesses);

        addRuntimeNumber(
                "Max phantom processes",
                "0 = Android default · 0–128",
                SettingsStore.maxPhantomProcesses(this),
                0, 128, 1, manualProcesses);

        addRuntimeNumber(
                "Background process target",
                "0 = framework/default · 0–64",
                SettingsStore.backgroundProcessLimit(this),
                0, 64, 2, manualProcesses);

        addRuntimeNumber(
                "Empty-process reserve",
                "0 = framework/default · 0–100%",
                SettingsStore.emptyProcessPercent(this),
                0, 100, 3, manualProcesses);

        addSection("LSPOSED / XPOSED");
        LinearLayout xposed = CatUi.card(this);
        root.addView(xposed, CatUi.cardParams(this));

        String xTitle = EnvironmentProbe.isXposedActive()
                ? "LSPosed connected"
                : "LSPosed not active in this process";
        int xColor = EnvironmentProbe.isXposedActive() ? CatUi.GOOD : CatUi.WARN;
        xposed.addView(CatUi.pill(this, xTitle, Color.rgb(
                Color.red(xColor) / 3,
                Color.green(xColor) / 3,
                Color.blue(xColor) / 3)));

        TextView xText = CatUi.text(this,
                "The hook module is built into this APK. Recommended scope: MultiTask + System Framework. "
                        + "You do not need to select every target app for normal launches.",
                13, CatUi.MUTED, false);
        LinearLayout.LayoutParams xp = new LinearLayout.LayoutParams(-1, -2);
        xp.topMargin = dp(10);
        xposed.addView(xText, xp);

        addSection("ROOT");
        boolean pluginInstalled = EnvironmentProbe.hasRoot()
                && RootPluginManager.isInstalled();

        LinearLayout rootStatus = CatUi.card(this);
        rootStatus.setBackground(CatUi.stroke(
                this,
                pluginInstalled ? Color.rgb(21, 47, 42) : CatUi.SURFACE,
                22,
                pluginInstalled ? Color.rgb(46, 132, 108) : Color.rgb(55, 63, 82)));
        root.addView(rootStatus, CatUi.cardParams(this));

        LinearLayout rootHeader = new LinearLayout(this);
        rootHeader.setGravity(Gravity.CENTER_VERTICAL);
        rootStatus.addView(rootHeader);

        rootHeader.addView(CatUi.text(
                this,
                pluginInstalled ? "Optional Root Module connected" : "Optional Root Module",
                16, CatUi.TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        rootHeader.addView(CatUi.pill(
                this,
                pluginInstalled ? "READY" : "OPTIONAL",
                pluginInstalled ? Color.rgb(34, 96, 78) : Color.rgb(52, 59, 78)));

        TextView rootInfo = CatUi.text(
                this,
                pluginInstalled
                        ? "Optional Root Module is active. Tap this card for module details and root-backed session controls."
                        : "MultiTask V2 works without the optional root module. Install/activate it in KernelSU-compatible module management to unlock the Root Helper broker.",
                12, CatUi.MUTED, false);
        LinearLayout.LayoutParams rip = new LinearLayout.LayoutParams(-1, -2);
        rip.topMargin = dp(8);
        rootStatus.addView(rootInfo, rip);
        rootStatus.setOnClickListener(v -> {
            if (pluginInstalled) startActivity(new Intent(this, RootManagerActivity.class));
            else CatDialog.show(this, "OPTIONAL ROOT MODULE", "Module not active",
                    "Install and activate the CatCore root module first. The Root Helper master switch stays locked until the module broker is detected.",
                    "Close", null, null);
        });
        CatUi.pressScale(rootStatus);

        if (!pluginInstalled) {
            Button helperBuilds = CatUi.secondaryButton(this, "Open Root Module builds");
            LinearLayout.LayoutParams hbp = new LinearLayout.LayoutParams(-1, dp(48));
            hbp.topMargin = dp(12);
            rootStatus.addView(helperBuilds, hbp);
            helperBuilds.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/actions/workflows/kernelsu-helper.yml")));
                } catch (Throwable ignored) {
                }
            });
        }

        addSwitch(
                "Root Helper",
                "Master switch for the optional KernelSU broker.",
                SettingsStore.rootHelperEnabled(this),
                pluginInstalled,
                value -> {
                    SettingsStore.setRootHelperEnabled(this, value);
                    build();
                });

        boolean rootFeatures = pluginInstalled && SettingsStore.rootHelperEnabled(this);

        addSwitch(
                "Root Task Start",
                "Compatibility-only root ActivityManager fallback. The LSPosed system bridge remains the normal V2 own-task path.",
                SettingsStore.rootTaskStart(this),
                rootFeatures,
                value -> SettingsStore.setRootTaskStart(this, value));

        addSwitch(
                "Session priority",
                "Temporarily adjusts nice/oom values while a MultiTask session is live and restores them afterwards.",
                SettingsStore.sessionPriority(this),
                rootFeatures,
                value -> SettingsStore.setSessionPriority(this, value));

        addSwitch(
                "Root resource telemetry",
                "Lets Task Manager use the optional root broker for additional process telemetry.",
                SettingsStore.rootTelemetry(this),
                rootFeatures,
                value -> SettingsStore.setRootTelemetry(this, value));

        addSwitch(
                "Trim after last managed window",
                "Requests a memory trim after the final MultiTask window for that app closes.",
                SettingsStore.autoTrim(this),
                rootFeatures,
                value -> SettingsStore.setAutoTrim(this, value));

        if (pluginInstalled) {
            Button testRoot = CatUi.secondaryButton(this, "Test Root Helper");
            root.addView(testRoot, buttonParams());
            testRoot.setOnClickListener(v ->
                    RootPluginManager.runAsync("status", null,
                            (ok, message) -> Toast.makeText(
                                    this,
                                    ok ? "Root Helper ready" : message,
                                    Toast.LENGTH_LONG).show()));
        }

        addSection("MAINTENANCE");
        Button clear = CatUi.secondaryButton(this, "Clear learned app start methods");
        root.addView(clear, buttonParams());
        clear.setOnClickListener(v -> {
            SettingsStore.launchCache(this).edit().clear().apply();
            Toast.makeText(this, "Learned start methods cleared.", Toast.LENGTH_SHORT).show();
        });

        Button onboarding = CatUi.secondaryButton(this, "Run setup again");
        root.addView(onboarding, buttonParams());
        onboarding.setOnClickListener(v -> {
            SettingsStore.setOnboardingComplete(this, false);
            getSharedPreferences("multitask_first_start", MODE_PRIVATE).edit().putInt("step", 0).apply();
            Intent i = new Intent(this, FirstStartActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finishAffinity();
        });

        addSection("ADVANCED");
        Button developer = CatUi.secondaryButton(this, "Developer Options");
        root.addView(developer, buttonParams());
        developer.setOnClickListener(v -> openDeveloperOptions());

        setContentView(scroll);
        scroll.setAlpha(0f);
        scroll.setTranslationY(dp(18));
        scroll.animate().alpha(1f).translationY(0f).setDuration(260L).start();
    }

    private void addModeCard(String name, String badge, String detail, int mode) {
        boolean selected = SettingsStore.startMode(this) == mode;

        LinearLayout card = CatUi.card(this);
        card.setBackground(CatUi.stroke(this,
                selected ? Color.rgb(27, 35, 63) : CatUi.SURFACE,
                22,
                selected ? CatUi.ACCENT : Color.rgb(38, 45, 63)));
        root.addView(card, CatUi.cardParams(this));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);

        row.addView(CatUi.text(this, name, 16, CatUi.TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(CatUi.pill(this,
                selected ? "ACTIVE" : badge,
                selected ? Color.rgb(57, 77, 163) : Color.rgb(43, 50, 68)));

        TextView d = CatUi.text(this, detail, 12, CatUi.MUTED, false);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
        dpv.topMargin = dp(8);
        card.addView(d, dpv);

        card.setOnClickListener(v -> {
            SettingsStore.setStartMode(this, mode);
            build();
        });
    }

    private void addRuntimeNumber(String title, String subtitle, int value,
                                  int min, int max, int kind, boolean enabled) {
        LinearLayout card = CatUi.card(this);
        root.addView(card, CatUi.cardParams(this));
        card.addView(CatUi.text(this, title, 15, CatUi.TEXT, true));

        TextView hint = CatUi.text(this, subtitle, 12, CatUi.MUTED, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(4);
        card.addView(hint, hp);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(value));
        input.setEnabled(enabled);
        input.setAlpha(enabled ? 1f : 0.45f);
        input.setTextColor(enabled ? CatUi.TEXT : CatUi.MUTED);
        input.setHintTextColor(CatUi.MUTED);
        input.setSingleLine(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(CatUi.shape(this, CatUi.SURFACE_3, 14));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(50));
        ip.topMargin = dp(10);
        card.addView(input, ip);

        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (enabled) scheduleRuntimeSave(input, min, max, kind);
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    private void scheduleRuntimeSave(EditText input, int min, int max, int kind) {
        if (runtimeApply != null) debounce.removeCallbacks(runtimeApply);
        runtimeApply = () -> {
            int value;
            try {
                value = Integer.parseInt(input.getText().toString().trim());
            } catch (Throwable t) {
                return;
            }

            if (value < min || value > max) {
                input.setError("Allowed: " + min + "–" + max);
                return;
            }

            if (kind == 0) SettingsStore.setMaxCachedProcesses(this, value);
            else if (kind == 1) SettingsStore.setMaxPhantomProcesses(this, value);
            else if (kind == 2) SettingsStore.setBackgroundProcessLimit(this, value);
            else SettingsStore.setEmptyProcessPercent(this, value);

            RuntimeTuning.applyAsync(this, true, null);
        };
        debounce.postDelayed(runtimeApply, 650L);
    }

    private void addSwitch(String title, String subtitle, boolean checked, ToggleAction action) {
        addSwitch(title, subtitle, checked, true, action);
    }

    private void addSwitch(String title, String subtitle, boolean checked,
                           boolean enabled, ToggleAction action) {
        LinearLayout card = CatUi.card(this);
        root.addView(card, CatUi.cardParams(this));

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setText(title);
        toggle.setTextColor(enabled ? CatUi.TEXT : CatUi.MUTED);
        toggle.setTextSize(16);
        toggle.setChecked(checked);
        toggle.setEnabled(enabled);
        card.addView(toggle);

        TextView hint = CatUi.text(this, subtitle, 12,
                enabled ? CatUi.MUTED : Color.rgb(105, 116, 139), false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(6);
        card.addView(hint, hp);

        toggle.setOnCheckedChangeListener((b, value) -> action.apply(value));
    }

    private void openDeveloperOptions() {
        if (SettingsStore.developerWarningAccepted(this)) {
            startActivity(new Intent(this, DeveloperOptionsActivity.class));
            return;
        }
        CatDialog.show(this, "ADVANCED", "Developer Options",
                "These controls can disable individual launch methods and make apps fail to open. They are intended for debugging and compatibility research.",
                "Cancel", "I understand", () -> {
                    SettingsStore.setDeveloperWarningAccepted(this, true);
                    startActivity(new Intent(this, DeveloperOptionsActivity.class));
                });
    }

    private void addSection(String value) {
        TextView heading = CatUi.section(this, value);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(24);
        root.addView(heading, p);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.topMargin = dp(10);
        return p;
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }

    private interface ToggleAction {
        void apply(boolean value);
    }
}
