package com.catcore.ctrlmietze.multitask;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
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
    private SwitchCompat compatibility;
    private SwitchCompat maxStability;
    private SwitchCompat childTasks;
    private SwitchCompat restoreRuntime;
    private EditText maxCached;
    private EditText maxPhantom;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(10, 12, 17));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("Settings", 28, Color.WHITE, true);
        root.addView(title);

        TextView subtitle = text("Tune how MultiTask resolves, starts and restores apps.", 13,
                Color.rgb(157, 169, 191), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        subtitleParams.bottomMargin = dp(18);
        root.addView(subtitle, subtitleParams);

        compatibility = addSwitch(root, "Compatibility mode",
                "Only enable this when an app does not start reliably. MultiTask briefly checks its launcher entries and tries additional fallbacks.",
                SettingsStore.compatibilityMode(this));

        maxStability = addSwitch(root, "Max Stability",
                "Uses the full fallback chain. For stubborn apps you can additionally add that app to the LSPosed scope.",
                SettingsStore.maxStability(this));

        childTasks = addSwitch(root, "Open apps as child tasks",
                "Experimental best-effort task mode. Android may still reuse a task depending on the target app's manifest.",
                SettingsStore.childTasks(this));

        restoreRuntime = addSwitch(root, "Restore runtime values",
                "Re-applies the values below when MultiTask starts and after boot. No system files are replaced.",
                SettingsStore.restoreRuntime(this));

        addSection(root, "Runtime limits");

        maxCached = addNumber(root, "Max cached processes",
                "0 = Android default · allowed range 0–512",
                SettingsStore.maxCachedProcesses(this));

        maxPhantom = addNumber(root, "Max phantom processes",
                "0 = Android default · allowed range 0–128",
                SettingsStore.maxPhantomProcesses(this));

        TextView runtimeNote = text(
                "These values use Android device_config. OEM firmware can ignore or clamp them.",
                12, Color.rgb(142, 154, 177), false);
        LinearLayout.LayoutParams runtimeNoteParams = new LinearLayout.LayoutParams(-1, -2);
        runtimeNoteParams.topMargin = dp(8);
        root.addView(runtimeNote, runtimeNoteParams);

        addSection(root, "LSPosed");

        LinearLayout scopeCard = card();
        root.addView(scopeCard, cardParams());
        scopeCard.addView(text("Recommended scope", 16, Color.WHITE, true));
        TextView scopeText = text(
                "For normal use select MultiTask + System Framework. You do not need to select every app. "
                        + "Only add a specific app when you want the in-app quick button or are testing Max Stability. "
                        + "SystemUI is not required for launching.",
                13, Color.rgb(173, 184, 204), false);
        LinearLayout.LayoutParams scopeTextParams = new LinearLayout.LayoutParams(-1, -2);
        scopeTextParams.topMargin = dp(7);
        scopeCard.addView(scopeText, scopeTextParams);

        Button clearCache = button("Clear learned app start methods");
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, dp(50));
        clearParams.topMargin = dp(12);
        root.addView(clearCache, clearParams);
        clearCache.setOnClickListener(v -> {
            SettingsStore.launchCache(this).edit().clear().apply();
            Toast.makeText(this, "Learned start methods cleared.", Toast.LENGTH_SHORT).show();
        });

        Button save = button("Save & apply");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(54));
        saveParams.topMargin = dp(20);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> save());

        setContentView(scroll);
    }

    private void save() {
        int cached = parse(maxCached, 0, 512);
        int phantom = parse(maxPhantom, 0, 128);
        if (cached < 0 || phantom < 0) return;

        SettingsStore.save(this,
                compatibility.isChecked(),
                maxStability.isChecked(),
                childTasks.isChecked(),
                restoreRuntime.isChecked(),
                cached,
                phantom);

        RuntimeTuning.applyAsync(this, true, (ok, message) -> {
            new AlertDialog.Builder(this)
                    .setTitle(ok ? "Settings applied" : "Settings saved")
                    .setMessage(message + (ok ? "" :
                            "\n\nSome runtime values could not be applied. Check root access."))
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private int parse(EditText input, int min, int max) {
        try {
            int value = Integer.parseInt(input.getText().toString().trim());
            if (value < min || value > max) {
                input.setError("Allowed: " + min + "–" + max);
                return -1;
            }
            return value;
        } catch (Throwable t) {
            input.setError("Enter a number");
            return -1;
        }
    }

    private SwitchCompat addSwitch(LinearLayout root, String title, String subtitle,
                                   boolean checked) {
        LinearLayout card = card();
        root.addView(card, cardParams());

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setText(title);
        toggle.setTextColor(Color.WHITE);
        toggle.setTextSize(16);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setChecked(checked);
        card.addView(toggle, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = text(subtitle, 12, Color.rgb(156, 168, 190), false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(5);
        card.addView(hint, p);
        return toggle;
    }

    private EditText addNumber(LinearLayout root, String title, String subtitle, int value) {
        LinearLayout card = card();
        root.addView(card, cardParams());
        card.addView(text(title, 15, Color.WHITE, true));

        TextView hint = text(subtitle, 12, Color.rgb(156, 168, 190), false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(4);
        card.addView(hint, hintParams);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(value));
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setBackground(shape(Color.rgb(34, 40, 55), dp(12)));
        input.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(48));
        inputParams.topMargin = dp(10);
        card.addView(input, inputParams);
        return input;
    }

    private void addSection(LinearLayout root, String value) {
        TextView heading = text(value.toUpperCase(), 12, Color.rgb(121, 145, 255), true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(22);
        p.bottomMargin = dp(5);
        root.addView(heading, p);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(shape(Color.rgb(21, 25, 34), dp(18)));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(8);
        return p;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(Color.rgb(78, 108, 232), dp(15)));
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
