package com.catcore.ctrlmietze.multitask;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public final class DeveloperOptionsActivity extends AppCompatActivity {
    private LinearLayout root;

    private static final String[][] METHODS = {
            {SettingsStore.METHOD_VIRTUAL_DIRECT, "V2 direct display launch",
                    "Use ActivityOptions.setLaunchDisplayId before any root fallback."},
            {SettingsStore.METHOD_VIRTUAL_ROOT, "V2 root display launch",
                    "Use root ActivityManager to target a MultiTask-owned virtual display."},
            {SettingsStore.METHOD_FREEFORM, "Android freeform fallback",
                    "Use Android freeform/windowing mode when the V2 display backend cannot host an app."},
            {SettingsStore.METHOD_ROOT_PACKAGE_FULL, "Root package launch · full flags",
                    "Launch the package through root with the full MultiTask task flag set."},
            {SettingsStore.METHOD_ROOT_COMPONENT_FULL, "Root component launch · full flags",
                    "Try each resolved launcher activity explicitly through root."},
            {SettingsStore.METHOD_DIRECT_COMPONENT_FULL, "Direct component launch",
                    "Start a resolved component directly from the MultiTask app process."},
            {SettingsStore.METHOD_DIRECT_LAUNCH_INTENT, "Android launch intent",
                    "Use PackageManager's default launch intent with MultiTask flags."},
            {SettingsStore.METHOD_ROOT_COMPONENT_BASIC, "Root component compatibility",
                    "Reduced root flag set for apps that reject the full strategy."},
            {SettingsStore.METHOD_ROOT_PACKAGE_BASIC, "Root package compatibility",
                    "Reduced package-level root fallback."},
            {SettingsStore.METHOD_MONKEY, "Final launcher fallback",
                    "Use Android's launcher-style monkey fallback only as the last compatibility method."}
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CatUi.applyWindow(this);
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(CatUi.background());

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(36));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        Button back = CatUi.secondaryButton(this, "‹");
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        back.setOnClickListener(v -> finish());

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.leftMargin = dp(12);
        header.addView(labels, lp);
        labels.addView(CatUi.text(this, "Developer Options", 27, CatUi.TEXT, true));
        labels.addView(CatUi.text(this, "Control the launch chain method by method", 12, CatUi.MUTED, false));

        LinearLayout warning = CatUi.card(this);
        warning.setBackground(CatUi.stroke(this, Color.rgb(48, 38, 23), 22, Color.rgb(116, 82, 38)));
        LinearLayout.LayoutParams wp = CatUi.cardParams(this);
        wp.topMargin = dp(18);
        root.addView(warning, wp);
        warning.addView(CatUi.text(this, "Advanced launch controls", 16, CatUi.TEXT, true));
        TextView wt = CatUi.text(this,
                "Disabling methods can make individual apps impossible to open. MultiTask still keeps diagnostics so you can see which path was unavailable.",
                12, Color.rgb(221, 195, 151), false);
        LinearLayout.LayoutParams wtp = new LinearLayout.LayoutParams(-1, -2);
        wtp.topMargin = dp(6);
        warning.addView(wt, wtp);

        for (String[] method : METHODS) addMethod(method[0], method[1], method[2]);

        TextView diagnosticsHeading = CatUi.section(this, "DIAGNOSTICS");
        LinearLayout.LayoutParams dhp = new LinearLayout.LayoutParams(-1, -2);
        dhp.topMargin = dp(24);
        root.addView(diagnosticsHeading, dhp);

        Button diagnostics = CatUi.primaryButton(this, "Export compatibility report");
        LinearLayout.LayoutParams dip = new LinearLayout.LayoutParams(-1, dp(52));
        dip.topMargin = dp(10);
        root.addView(diagnostics, dip);
        diagnostics.setOnClickListener(v -> {
            diagnostics.setEnabled(false);
            diagnostics.setText("Collecting diagnostics…");
            DiagnosticsManager.export(this, (ok, location) -> {
                diagnostics.setEnabled(true);
                diagnostics.setText("Export compatibility report");
                Toast.makeText(this,
                        ok ? "Saved to " + location : location,
                        Toast.LENGTH_LONG).show();
            });
        });

        Button reset = CatUi.secondaryButton(this, "Reset all methods");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(52));
        rp.topMargin = dp(18);
        root.addView(reset, rp);
        reset.setOnClickListener(v -> {
            for (String[] method : METHODS) {
                SettingsStore.setMethodEnabled(this, method[0], true);
            }
            build();
        });

        setContentView(scroll);
    }

    private void addMethod(String key, String title, String subtitle) {
        LinearLayout card = CatUi.card(this);
        root.addView(card, CatUi.cardParams(this));

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setText(title);
        toggle.setTextColor(CatUi.TEXT);
        toggle.setTextSize(15);
        toggle.setChecked(SettingsStore.methodEnabled(this, key));
        card.addView(toggle);

        TextView hint = CatUi.text(this, subtitle, 12, CatUi.MUTED, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(6);
        card.addView(hint, hp);

        toggle.setOnCheckedChangeListener((b, checked) ->
                SettingsStore.setMethodEnabled(this, key, checked));
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
