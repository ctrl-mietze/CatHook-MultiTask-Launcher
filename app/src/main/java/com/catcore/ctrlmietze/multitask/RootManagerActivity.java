package com.catcore.ctrlmietze.multitask;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public final class RootManagerActivity extends AppCompatActivity {
    private LinearLayout root;
    private boolean pluginInstalled;

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
        pluginInstalled = EnvironmentProbe.hasRoot() && RootPluginManager.isInstalled();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(CatUi.background());

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(34));
        scroll.removeAllViews();
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
        titles.addView(CatUi.text(this, "Root Manager", 27, CatUi.TEXT, true));
        titles.addView(CatUi.text(this, "Optional KernelSU helper features", 12, CatUi.MUTED, false));

        LinearLayout hero = CatUi.card(this);
        hero.setBackground(CatUi.hero(this));
        LinearLayout.LayoutParams hp = CatUi.cardParams(this);
        hp.topMargin = dp(18);
        root.addView(hero, hp);

        hero.addView(CatUi.text(this,
                pluginInstalled ? "CatCore Root Helper connected" : "Root Helper plugin not installed",
                20, CatUi.TEXT, true));
        TextView heroText = CatUi.text(this,
                pluginInstalled
                        ? "The KernelSU helper is available. Every feature below is temporary and session-based."
                        : "MultiTask still works without this plugin. Install it only if you want the advanced root-backed task controls.",
                13, Color.rgb(205, 212, 232), false);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(-1, -2);
        htp.topMargin = dp(8);
        hero.addView(heroText, htp);

        if (!pluginInstalled) {
            Button github = CatUi.primaryButton(this, "Download Root Helper from GitHub");
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(52));
            gp.topMargin = dp(15);
            hero.addView(github, gp);
            github.setOnClickListener(v -> openGithub());
        }

        addSection("ROOT TASK CONTROL");

        addSwitch("Root Helper",
                "Uses the KernelSU helper for privileged task operations while MultiTask is active.",
                SettingsStore.rootHelperEnabled(this),
                value -> SettingsStore.setRootHelperEnabled(this, value));

        addSwitch("Root Task Start",
                "Starts Android ActivityManager commands through root for stronger task placement. "
                        + "The target app still keeps its own Android UID; it is not permanently turned into UID 0.",
                SettingsStore.rootTaskStart(this),
                value -> SettingsStore.setRootTaskStart(this, value));

        addSwitch("Session priority",
                "Temporarily raises process scheduling priority and lowers oom_score_adj for active MultiTask sessions. "
                        + "The helper restores normal values when the session closes.",
                SettingsStore.sessionPriority(this),
                value -> SettingsStore.setSessionPriority(this, value));

        addSwitch("Root resource telemetry",
                "Lets Task Manager read /proc data for more accurate process, RAM and CPU information.",
                SettingsStore.rootTelemetry(this),
                value -> SettingsStore.setRootTelemetry(this, value));

        addSwitch("Trim app after closing its last MultiTask window",
                "Sends a temporary memory trim hint after the final managed task closes. It does not freeze or disable the app.",
                SettingsStore.autoTrim(this),
                value -> SettingsStore.setAutoTrim(this, value));

        Button test = CatUi.secondaryButton(this, "Test Root Helper");
        test.setEnabled(pluginInstalled);
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(-1, dp(52));
        testParams.topMargin = dp(16);
        root.addView(test, testParams);
        test.setOnClickListener(v -> RootPluginManager.runAsync("status", null,
                (ok, message) -> Toast.makeText(this,
                        ok ? "Root Helper ready" : message,
                        Toast.LENGTH_LONG).show()));

        setContentView(scroll);
    }

    private void addSection(String value) {
        TextView heading = CatUi.section(this, value);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(24);
        root.addView(heading, p);
    }

    private void addSwitch(String title, String subtitle, boolean checked, ToggleAction action) {
        LinearLayout card = CatUi.card(this);
        root.addView(card, CatUi.cardParams(this));

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setText(title);
        toggle.setTextColor(CatUi.TEXT);
        toggle.setTextSize(16);
        toggle.setChecked(checked);
        toggle.setEnabled(pluginInstalled);
        card.addView(toggle, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = CatUi.text(this, subtitle, 12, CatUi.MUTED, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(6);
        card.addView(hint, hp);

        toggle.setOnCheckedChangeListener((button, value) -> action.apply(value));
    }

    private void openGithub() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/actions/workflows/kernelsu-helper.yml")));
        } catch (Throwable ignored) {
        }
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }

    private interface ToggleAction {
        void apply(boolean value);
    }
}
