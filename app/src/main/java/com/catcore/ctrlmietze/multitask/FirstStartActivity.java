package com.catcore.ctrlmietze.multitask;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public final class FirstStartActivity extends AppCompatActivity {
    private static final String PREFS = "multitask_first_start";
    private static final String K_STEP = "step";

    private LinearLayout body;
    private TextView kicker;
    private TextView title;
    private TextView subtitle;
    private Button primary;
    private Button secondary;
    private int step;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CatUi.applyWindow(this);

        step = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(K_STEP, 0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(22));
        root.setBackground(CatUi.background());

        TextView brand = CatUi.pill(this, "CATCORE · MULTITASK V2", Color.rgb(48, 58, 104));
        root.addView(brand, new LinearLayout.LayoutParams(-2, dp(32)));

        kicker = CatUi.text(this, "", 12, Color.rgb(143, 158, 255), true);
        LinearLayout.LayoutParams kickerParams = new LinearLayout.LayoutParams(-1, -2);
        kickerParams.topMargin = dp(34);
        root.addView(kicker, kickerParams);

        title = CatUi.text(this, "", 34, CatUi.TEXT, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        root.addView(title, titleParams);

        subtitle = CatUi.text(this, "", 15, CatUi.MUTED, false);
        subtitle.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(10);
        subParams.bottomMargin = dp(22);
        root.addView(subtitle, subParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.topMargin = dp(14);
        root.addView(actions, actionsParams);

        primary = CatUi.primaryButton(this, "Continue");
        actions.addView(primary, new LinearLayout.LayoutParams(-1, dp(56)));
        CatUi.pressScale(primary);

        secondary = CatUi.secondaryButton(this, "");
        LinearLayout.LayoutParams secondaryParams = new LinearLayout.LayoutParams(-1, dp(52));
        secondaryParams.topMargin = dp(9);
        actions.addView(secondary, secondaryParams);
        CatUi.pressScale(secondary);

        setContentView(root);
        render();
    }

    private void render() {
        body.removeAllViews();
        secondary.setVisibility(View.GONE);

        switch (step) {
            case 0:
                welcome();
                break;
            case 1:
                rootStep();
                break;
            case 2:
                xposedStep();
                break;
            case 3:
                systemHookStep();
                break;
            case 4:
                frameworkStep();
                break;
            case 5:
                permissionsStep();
                break;
            default:
                modeStep();
                break;
        }
    }

    private void welcome() {
        setHeader("WELCOME", "Meet CatCore MultiTask",
                "V2 adds its own task and window framework on top of Android. "
                        + "This setup checks the privileged pieces once, then MultiTask keeps the normal daily flow fast.");

        addFeature("Independent framework",
                "MultiTask keeps its window and task controller in a separate lightweight process.");
        addFeature("Integrated LSPosed module",
                "The hook code ships inside this APK. LSPosed still has to activate it from the device.");
        addFeature("No permanent system patches",
                "Runtime hooks and helper sessions disappear when the module or app is disabled.");

        primary.setText("Start setup");
        primary.setOnClickListener(v -> next(true));
    }

    private void rootStep() {
        setHeader("1 OF 6 · ROOT", "Give MultiTask its control channel",
                "V2 uses root for reliable task inspection, system settings mirroring and optional Root Helper features. "
                        + "Your root manager remains in control of the permission.");

        boolean root = EnvironmentProbe.hasRoot();
        addStatus(root ? "Root access verified" : "Root access not granted yet",
                root ? CatUi.GOOD : CatUi.WARN,
                root ? "The MultiTask UID can open a root shell."
                        : "Tap verify. KernelSU, Magisk or another compatible su manager should ask you.");

        primary.setText(root ? "Continue" : "Grant & verify root");
        primary.setOnClickListener(v -> {
            primary.setEnabled(false);
            new Thread(() -> {
                boolean ok = EnvironmentProbe.hasRoot();
                runOnUiThread(() -> {
                    primary.setEnabled(true);
                    if (ok) next(true);
                    else {
                        new AlertDialog.Builder(this)
                                .setTitle("Root not available yet")
                                .setMessage("No UID 0 shell was returned. Grant MultiTask root in your root manager and try again.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }, "MultiTask-RootProbe").start();
        });
    }

    private void xposedStep() {
        setHeader("2 OF 6 · LSPOSED", "Activate the built-in hook module",
                "MultiTask already contains its Xposed hook code. Enable the MultiTask module in LSPosed and keep "
                        + "MultiTask + System Framework in scope. Target apps do not normally need to be selected.");

        boolean active = EnvironmentProbe.isXposedActive();
        addStatus(active ? "LSPosed injection active" : "Waiting for LSPosed activation",
                active ? CatUi.GOOD : CatUi.WARN,
                active ? "The MultiTask app process is currently hooked."
                        : "Open LSPosed, enable MultiTask and return here.");

        primary.setText(active ? "Continue" : "Restart & verify");
        primary.setOnClickListener(v -> {
            if (active) next(true);
            else restartStep(2);
        });

        secondary.setVisibility(View.VISIBLE);
        secondary.setText("Open LSPosed");
        secondary.setOnClickListener(v -> openLsposed());
    }

    private void systemHookStep() {
        setHeader("3 OF 6 · SYSTEM", "Connect the Android task service",
                "The System Framework scope is what lets MultiTask reinforce starts before Android collapses them back "
                        + "into an existing task. It is also the bridge used by the V2 window backend.");

        boolean active = EnvironmentProbe.isSystemHookActive(this);
        addStatus(active ? "System Framework hook active" : "System Framework hook not detected",
                active ? CatUi.GOOD : CatUi.WARN,
                active ? "The system_server hook was loaded during this boot."
                        : "Make sure Android/System Framework is selected in LSPosed. A reboot can be required after first activation.");

        primary.setText(active ? "Continue" : "Check again");
        primary.setOnClickListener(v -> {
            if (EnvironmentProbe.isSystemHookActive(this)) next(true);
            else render();
        });

        secondary.setVisibility(View.VISIBLE);
        secondary.setText("Open LSPosed");
        secondary.setOnClickListener(v -> openLsposed());
    }

    private void frameworkStep() {
        setHeader("4 OF 6 · FRAMEWORK", "Install the CatCore MultiTask Framework",
                "The framework is already packaged inside MultiTask. “Install” enables its dedicated :framework process "
                        + "and persistent low-priority service. There is no second APK.");

        addStatus("Ready to install", CatUi.ACCENT,
                "The service has no polling loop. It stays available for sessions and wakes on commands.");

        primary.setText("Install framework");
        primary.setOnClickListener(v -> {
            SettingsStore.setFrameworkEnabled(this, true);
            try {
                CatCoreFrameworkService.start(this);
                next(true);
            } catch (Throwable t) {
                new AlertDialog.Builder(this)
                        .setTitle("Framework could not start")
                        .setMessage(t.getClass().getSimpleName() + ": " + t.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void permissionsStep() {
        setHeader("5 OF 6 · PERMISSIONS", "Finish the Android permissions",
                "MultiTask only asks for permissions that support visible framework behavior. Root and LSPosed remain separate grants.");

        addFeature("Notifications",
                "Used by the foreground framework notification and MultiTask status messages.");
        addFeature("Boot restore",
                "Restarts the framework and your chosen runtime values after Android has finished booting.");

        primary.setText("Allow notifications & continue");
        primary.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 500);
            } else {
                next(true);
            }
        });

        secondary.setVisibility(View.VISIBLE);
        secondary.setText("Continue without notification permission");
        secondary.setOnClickListener(v -> next(true));
    }

    private void modeStep() {
        setHeader("6 OF 6 · START MODE", "Choose how MultiTask opens apps",
                "Neither mode is globally “better”. You can switch at any time in Settings.");

        LinearLayout mine = selectableCard(
                "Start as my task",
                "Stable",
                "Runs the app inside a MultiTask-owned task/window session. This is the V2 framework path.",
                SettingsStore.startMode(this) == SettingsStore.MODE_MY_TASK);
        body.addView(mine, CatUi.cardParams(this));
        mine.setOnClickListener(v -> {
            SettingsStore.setStartMode(this, SettingsStore.MODE_MY_TASK);
            render();
        });

        LinearLayout own = selectableCard(
                "Start as app's own task",
                "Native identity",
                "Keeps the target app in its own Android task and uses the stronger root + LSPosed launch chain.",
                SettingsStore.startMode(this) == SettingsStore.MODE_APP_OWN_TASK);
        body.addView(own, CatUi.cardParams(this));
        own.setOnClickListener(v -> {
            SettingsStore.setStartMode(this, SettingsStore.MODE_APP_OWN_TASK);
            render();
        });

        primary.setText("Enter CatCore MultiTask");
        primary.setOnClickListener(v -> {
            SettingsStore.setOnboardingComplete(this, true);
            SettingsStore.setFrameworkEnabled(this, true);
            AppTaskRules.mirrorToSystem(this);
            try { CatCoreFrameworkService.start(this); } catch (Throwable ignored) {}
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(K_STEP, 0).apply();

            Intent main = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(main);
            finishAffinity();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 500) next(true);
    }

    private void next(boolean restart) {
        step++;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(K_STEP, step).apply();
        if (restart && (step == 2 || step == 3)) restartStep(step);
        else render();
    }

    private void restartStep(int target) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(K_STEP, target).apply();
        Intent i = new Intent(this, FirstStartActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finishAffinity();
    }

    private void openLsposed() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("org.lsposed.manager");
        try {
            if (launch != null) startActivity(launch);
            else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable ignored) {
        }
    }

    private void setHeader(String k, String t, String s) {
        kicker.setText(k);
        title.setText(t);
        subtitle.setText(s);
    }

    private void addFeature(String name, String detail) {
        LinearLayout card = CatUi.card(this);
        body.addView(card, CatUi.cardParams(this));
        card.addView(CatUi.text(this, name, 16, CatUi.TEXT, true));
        TextView d = CatUi.text(this, detail, 13, CatUi.MUTED, false);
        d.setLineSpacing(0f, 1.1f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(5);
        card.addView(d, p);
    }

    private void addStatus(String name, int color, String detail) {
        LinearLayout card = CatUi.card(this);
        body.addView(card, CatUi.cardParams(this));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);

        View dot = new View(this);
        dot.setBackground(CatUi.shape(this, color, 999));
        top.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));

        TextView n = CatUi.text(this, name, 16, CatUi.TEXT, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1);
        np.leftMargin = dp(10);
        top.addView(n, np);

        TextView d = CatUi.text(this, detail, 13, CatUi.MUTED, false);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
        dpv.topMargin = dp(7);
        card.addView(d, dpv);
    }

    private LinearLayout selectableCard(String name, String badge, String detail, boolean checked) {
        LinearLayout card = CatUi.card(this);
        card.setBackground(CatUi.stroke(this,
                checked ? Color.rgb(28, 35, 62) : CatUi.SURFACE,
                22,
                checked ? CatUi.ACCENT : Color.rgb(38, 45, 63)));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);

        row.addView(CatUi.text(this, name, 17, CatUi.TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(CatUi.pill(this, checked ? "SELECTED" : badge,
                checked ? Color.rgb(58, 80, 170) : Color.rgb(46, 53, 72)));

        TextView d = CatUi.text(this, detail, 13, CatUi.MUTED, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(9);
        card.addView(d, p);
        return card;
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
