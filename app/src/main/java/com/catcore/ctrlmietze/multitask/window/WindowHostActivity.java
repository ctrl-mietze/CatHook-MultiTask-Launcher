package com.catcore.ctrlmietze.multitask.window;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public final class WindowHostActivity extends AppCompatActivity {
    private static final int MAX_WINDOWS = 8;

    private final List<VirtualWindowView> windows = new ArrayList<>();
    private FrameLayout canvas;
    private TextView status;
    private int cascade;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 10, 14));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(14), dp(10));
        bar.setBackgroundColor(Color.rgb(17, 21, 29));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(64)));

        TextView back = pill("‹", 24);
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        back.setOnClickListener(v -> finish());

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(12);
        bar.addView(titles, titleParams);

        TextView title = text("MultiTask Window Workspace", 18, Color.WHITE, true);
        titles.addView(title);

        status = text(WindowFramework.capabilitySummary(this), 11,
                Color.rgb(142, 157, 184), false);
        titles.addView(status);

        TextView hint = pill("V2", 12);
        bar.addView(hint, new LinearLayout.LayoutParams(dp(46), dp(34)));

        canvas = new FrameLayout(this);
        canvas.setClipChildren(true);
        canvas.setClipToPadding(true);
        canvas.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(canvas, new LinearLayout.LayoutParams(-1, 0, 1));

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
            status.setText("Maximum of " + MAX_WINDOWS + " windows reached.");
            return;
        }

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int width = Math.max(dp(300), Math.min(screenW - dp(28), dp(430)));
        int height = Math.max(dp(360), Math.min(screenH - dp(130), dp(650)));

        VirtualWindowView window = new VirtualWindowView(
                this, pkg, activity, label,
                closed -> {
                    windows.remove(closed);
                    canvas.removeView(closed);
                    updateStatus();
                });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        canvas.addView(window, lp);
        windows.add(window);

        int offset = dp(22) * (cascade++ % 6);
        window.setX(dp(8) + offset);
        window.setY(dp(8) + offset);
        window.bringToFront();
        updateStatus();
    }

    private void updateStatus() {
        status.setText(windows.size() + " active window"
                + (windows.size() == 1 ? "" : "s")
                + " · " + WindowFramework.capabilitySummary(this));
    }

    private TextView pill(String value, int sp) {
        TextView t = text(value, sp, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(shape(Color.rgb(50, 61, 84), dp(13)));
        return t;
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
