package com.catcore.ctrlmietze.multitask.window;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.catcore.ctrlmietze.multitask.CatCoreFrameworkService;
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
    private TextView layoutButton;
    private int cascade;
    private int layoutMode;
    private boolean restoring;
    private final Handler guardHandler = new Handler(Looper.getMainLooper());
    private final Runnable guardHeartbeat = new Runnable() {
        @Override public void run() {
            WindowGuardBridge.heartbeat(WindowHostActivity.this, windows.size());
            if (!windows.isEmpty()) guardHandler.postDelayed(this, 5000L);
        }
    };

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
        back.setOnClickListener(v -> closeWorkspaceAndFinish());

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

        layoutButton = CatUi.pill(this, "▦", Color.rgb(47, 55, 76));
        layoutButton.setTextSize(15);
        bar.addView(layoutButton, new LinearLayout.LayoutParams(dp(42), dp(34)));
        layoutButton.setOnClickListener(v -> cycleLayout());

        windowCount = CatUi.pill(this, "0 / " + MAX_WINDOWS, Color.rgb(55, 67, 127));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(66), dp(34));
        countParams.leftMargin = dp(6);
        bar.addView(windowCount, countParams);

        canvas = new FrameLayout(this);
        canvas.setClipChildren(true);
        canvas.setClipToPadding(true);
        canvas.setPadding(dp(4), dp(10), dp(4), dp(4));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1);
        cp.topMargin = dp(8);
        root.addView(canvas, cp);

        setContentView(root);

        boolean recreatedByAndroid = state != null;
        canvas.post(() -> {
            restoreSessions();
            if (!recreatedByAndroid) {
                handleIntent(getIntent());
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        guardHandler.removeCallbacks(guardHeartbeat);
        guardHandler.post(guardHeartbeat);
    }

    @Override protected void onPause() {
        WindowGuardBridge.heartbeat(this, windows.size());
        super.onPause();
    }

    @Override protected void onDestroy() {
        guardHandler.removeCallbacks(guardHeartbeat);
        WindowGuardBridge.heartbeat(this, windows.size());
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        closeWorkspaceAndFinish();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String pkg = intent.getStringExtra(WindowFramework.EXTRA_PACKAGE);
        if (pkg == null || pkg.trim().isEmpty()) return;

        String activity = intent.getStringExtra(WindowFramework.EXTRA_ACTIVITY);
        String label = intent.getStringExtra(WindowFramework.EXTRA_LABEL);
        intent.removeExtra(WindowFramework.EXTRA_PACKAGE);

        addWindow(pkg, activity, label, null);
    }

    private void addWindow(String pkg, String activity, String label,
                           WindowSessionStore.State restoreState) {
        if (windows.size() >= MAX_WINDOWS) {
            status.setText("Maximum of " + MAX_WINDOWS + " live windows reached");
            return;
        }

        int screenW = Math.max(dp(320), canvas.getWidth());
        int screenH = Math.max(dp(430), canvas.getHeight());
        int width = restoreState != null && restoreState.width > 0
                ? restoreState.width
                : Math.max(dp(300), Math.min(screenW - dp(28), dp(430)));
        int height = restoreState != null && restoreState.height > 0
                ? restoreState.height
                : Math.max(dp(360), Math.min(screenH - dp(20), dp(650)));

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
                    persistSessions();
                    updateStatus();
                },
                this::persistSessions);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        canvas.addView(window, lp);
        windows.add(window);

        if (SettingsStore.rootHelperEnabled(this)
                && SettingsStore.sessionPriority(this)
                && RootPluginManager.isInstalled()) {
            canvas.postDelayed(() ->
                    RootPluginManager.runAsync("boost", pkg, null), 700L);
        }

        if (restoreState != null) {
            canvas.post(() -> window.restoreState(restoreState));
        } else {
            int offset = dp(20) * (cascade++ % 6);
            window.setX(dp(4) + offset);
            window.setY(dp(4) + offset);
        }

        window.setAlpha(0f);
        window.setScaleX(0.97f);
        window.setScaleY(0.97f);
        window.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start();

        window.bringToFront();
        persistSessions();
        updateStatus();
    }

    private void restoreSessions() {
        if (!SettingsStore.restoreWindows(this)) {
            WindowSessionStore.clear(this);
            return;
        }

        List<WindowSessionStore.State> saved = WindowSessionStore.load(this);
        if (saved.isEmpty()) return;

        restoring = true;
        WindowSessionStore.clear(this);
        for (WindowSessionStore.State state : saved) {
            if (windows.size() >= MAX_WINDOWS) break;
            addWindow(state.packageName, state.activityName, state.label, state);
        }
        restoring = false;
        persistSessions();

        if (!windows.isEmpty()) {
            status.setText("Recovered " + windows.size() + " workspace session"
                    + (windows.size() == 1 ? "" : "s"));
        }
    }

    private void persistSessions() {
        if (restoring || !SettingsStore.restoreWindows(this)) return;
        List<WindowSessionStore.State> states = new ArrayList<>();
        for (VirtualWindowView window : windows) {
            if (window.getWidth() <= 0 || window.getHeight() <= 0) continue;
            states.add(window.snapshot());
        }
        WindowSessionStore.save(this, states);
    }

    private void cycleLayout() {
        if (windows.isEmpty()) return;
        layoutMode = (layoutMode + 1) % 3;
        if (layoutMode == 0) {
            arrangeCascade();
            status.setText("Layout · Cascade");
        } else if (layoutMode == 1) {
            arrangeGrid();
            status.setText("Layout · Grid");
        } else {
            arrangeColumns();
            status.setText("Layout · Columns");
        }
        persistSessions();
    }

    private void arrangeCascade() {
        int screenW = canvas.getWidth();
        int screenH = canvas.getHeight();
        int width = Math.max(dp(300), Math.min(screenW - dp(40), dp(430)));
        int height = Math.max(dp(340), Math.min(screenH - dp(40), dp(620)));

        for (int i = 0; i < windows.size(); i++) {
            int offset = dp(20) * (i % 6);
            windows.get(i).applyBounds(
                    dp(4) + offset,
                    dp(4) + offset,
                    width,
                    height,
                    true);
        }
    }

    private void arrangeGrid() {
        int count = windows.size();
        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil(count / (double) cols);
        int gap = dp(8);
        int width = Math.max(dp(270),
                (canvas.getWidth() - gap * (cols - 1)) / Math.max(1, cols));
        int height = Math.max(dp(330),
                (canvas.getHeight() - gap * (rows - 1)) / Math.max(1, rows));

        for (int i = 0; i < count; i++) {
            int col = i % cols;
            int row = i / cols;
            windows.get(i).applyBounds(
                    col * (width + gap),
                    row * (height + gap),
                    width,
                    height,
                    true);
        }
    }

    private void arrangeColumns() {
        int count = windows.size();
        int gap = dp(6);
        int width = Math.max(dp(270),
                (canvas.getWidth() - gap * (count - 1)) / Math.max(1, count));
        int height = Math.max(dp(330), canvas.getHeight());

        for (int i = 0; i < count; i++) {
            windows.get(i).applyBounds(
                    i * (width + gap),
                    0,
                    width,
                    height,
                    true);
        }
    }

    private void closeWorkspaceAndFinish() {
        List<VirtualWindowView> copy = new ArrayList<>(windows);
        for (VirtualWindowView window : copy) {
            window.close();
        }
        WindowSessionStore.clear(this);
        CatCoreFrameworkService.updateStatus(this, "Framework active · no live windows");
        finish();
    }

    private void updateStatus() {
        int count = windows.size();
        windowCount.setText(count + " / " + MAX_WINDOWS);
        status.setText(count == 0
                ? WindowFramework.capabilitySummary(this)
                : count + " live window" + (count == 1 ? "" : "s")
                        + " · " + WindowFramework.capabilitySummary(this));

        WindowGuardBridge.heartbeat(this, count);
        guardHandler.removeCallbacks(guardHeartbeat);
        if (count > 0) guardHandler.postDelayed(guardHeartbeat, 5000L);

        CatCoreFrameworkService.updateStatus(
                this,
                count == 0
                        ? "Framework active · no live windows"
                        : count + " live MultiTask window" + (count == 1 ? "" : "s"));
    }

    private int dp(int value) {
        return CatUi.dp(this, value);
    }
}
