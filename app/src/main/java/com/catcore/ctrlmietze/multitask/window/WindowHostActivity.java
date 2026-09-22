package com.catcore.ctrlmietze.multitask.window;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.catcore.ctrlmietze.multitask.CatUi;
import com.catcore.ctrlmietze.multitask.RootPluginManager;
import com.catcore.ctrlmietze.multitask.SettingsStore;

import java.util.ArrayList;
import java.util.List;

public final class WindowHostActivity extends AppCompatActivity {
    private static final int MAX_WINDOWS = 8;

    private final List<VirtualWindowView> windows = new ArrayList<>();
    private FrameLayout canvas;
    private TextView status;
    private TextView windowCount;
    private int cascade;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CatUi.applyWindow(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackground(CatUi.background());

        LinearLayout bar = CatUi.card(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(70)));

        TextView back = CatUi.pill(this, "‹", Color.rgb(47, 55, 76));
        back.setTextSize(23);
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        back.setOnClickListener(v -> finish());

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(12);
        bar.addView(titles, titleParams);

        titles.addView(CatUi.text(this, "CatCore Workspace", 18, CatUi.TEXT, true));

        status = CatUi.text(this, WindowFramework.capabilitySummary(this), 10,
                CatUi.MUTED, false);
        status.setSingleLine(true);
        titles.addView(status);

        windowCount = CatUi.pill(this, "0 / " + MAX_WINDOWS, Color.rgb(55, 67, 127));
        bar.addView(windowCount, new LinearLayout.LayoutParams(dp(66), dp(34)));

        canvas = new FrameLayout(this);
        canvas.setClipChildren(true);
        canvas.setClipToPadding(true);
        canvas.setPadding(dp(4), dp(10), dp(4), dp(4));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1);
        cp.topMargin = dp(8);
        root.addView(canvas, cp);

        setContentView(root);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String pkg = intent.getStringExtra(WindowFramework.EXTRA_PACKAGE);
        if (pkg == null || pkg.trim().isEmpty()) return;

        String activity = intent.getStringExtra(WindowFramework.EXTRA_ACTIVITY);
        String label = intent.getStringExtra(WindowFramework.EXTRA_LABEL);
        intent.removeExtra(WindowFramework.EXTRA_PACKAGE);

        addWindow(pkg, activity, label);
    }

    private void addWindow(String pkg, String activity, String label) {
        if (windows.size() >= MAX_WINDOWS) {
            status.setText("Maximum of " + MAX_WINDOWS + " live windows reached");
            return;
        }

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int width = Math.max(dp(300), Math.min(screenW - dp(28), dp(430)));
        int height = Math.max(dp(360), Math.min(screenH - dp(150), dp(650)));

        VirtualWindowView window = new VirtualWindowView(
                this, pkg, activity, label,
                closed -> {
                    String closedPackage = closed.packageName();
                    windows.remove(closed);
                    canvas.removeView(closed);

                    boolean stillOpen = false;
                    for (VirtualWindowView other : windows) {
                        if (closedPackage.equals(other.packageName())) {
                            stillOpen = true;
                            break;
                        }
                    }

                    if (!stillOpen
                            && SettingsStore.rootHelperEnabled(this)
                            && RootPluginManager.isInstalled()) {
                        RootPluginManager.runAsync("restore", closedPackage, null);
                        if (SettingsStore.autoTrim(this)) {
                            RootPluginManager.runAsync("trim", closedPackage, null);
                        }
                    }
                    updateStatus();
                });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        canvas.addView(window, lp);
        windows.add(window);

        if (SettingsStore.rootHelperEnabled(this)
                && SettingsStore.sessionPriority(this)
                && RootPluginManager.isInstalled()) {
            canvas.postDelayed(() ->
                    RootPluginManager.runAsync("boost", pkg, null), 700L);
        }

        int offset = dp(20) * (cascade++ % 6);
        window.setX(dp(4) + offset);
        window.setY(dp(4) + offset);
        window.bringToFront();
        updateStatus();
    }

    private void updateStatus() {
        int count = windows.size();
        windowCount.setText(count + " / " + MAX_WINDOWS);
        status.setText(count == 0
                ? WindowFramework.capabilitySummary(this)
                : count + " live window" + (count == 1 ? "" : "s")
                        + " · " + WindowFramework.capabilitySummary(this));
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
