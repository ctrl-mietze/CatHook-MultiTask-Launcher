package com.catcore.ctrlmietze.multitask;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class CatUi {
    public static final int BG = Color.rgb(7, 9, 14);
    public static final int SURFACE = Color.rgb(15, 19, 29);
    public static final int SURFACE_2 = Color.rgb(23, 29, 43);
    public static final int SURFACE_3 = Color.rgb(31, 39, 57);
    public static final int TEXT = Color.rgb(247, 249, 255);
    public static final int MUTED = Color.rgb(157, 170, 194);
    public static final int ACCENT = Color.rgb(110, 133, 255);
    public static final int ACCENT_2 = Color.rgb(116, 83, 255);
    public static final int GOOD = Color.rgb(71, 214, 156);
    public static final int WARN = Color.rgb(255, 190, 82);
    public static final int BAD = Color.rgb(255, 101, 124);

    private CatUi() {}

    public static void applyWindow(Activity activity) {
        Window w = activity.getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
    }

    public static GradientDrawable background() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(5, 7, 12), Color.rgb(9, 12, 22), Color.rgb(11, 9, 20)});
        return d;
    }

    public static GradientDrawable hero(Activity a) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(35, 44, 86), Color.rgb(47, 34, 96), Color.rgb(20, 28, 54)});
        d.setCornerRadius(dp(a, 28));
        d.setStroke(dp(a, 1), Color.rgb(68, 80, 132));
        return d;
    }

    public static GradientDrawable dashboard(Activity a) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(20, 31, 63),
                        Color.rgb(28, 29, 69),
                        Color.rgb(18, 48, 61)
                });
        d.setCornerRadius(dp(a, 26));
        d.setStroke(dp(a, 1), Color.rgb(61, 83, 132));
        return d;
    }

    public static GradientDrawable shape(Activity a, int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(a, radius));
        return d;
    }

    public static GradientDrawable stroke(Activity a, int color, int radius, int stroke) {
        GradientDrawable d = shape(a, color, radius);
        d.setStroke(dp(a, 1), stroke);
        return d;
    }

    public static TextView text(Activity a, String value, int sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        else t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }

    public static TextView pill(Activity a, String value, int color) {
        TextView t = text(a, value, 11, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(a, 10), 0, dp(a, 10), 0);
        t.setBackground(shape(a, color, 999));
        t.setMinHeight(dp(a, 30));
        return t;
    }

    public static Button primaryButton(Activity a, String label) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ACCENT, ACCENT_2});
        d.setCornerRadius(dp(a, 18));
        b.setBackground(d);
        b.setStateListAnimator(null);
        b.setElevation(dp(a, 2));
        return b;
    }

    public static Button secondaryButton(Activity a, String label) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setGravity(Gravity.CENTER);
        b.setBackground(stroke(a, SURFACE_2, 18, Color.rgb(60, 70, 96)));
        b.setStateListAnimator(null);
        return b;
    }

    public static LinearLayout card(Activity a) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a, 16), dp(a, 15), dp(a, 16), dp(a, 15));
        card.setBackground(stroke(a, SURFACE, 22, Color.rgb(36, 43, 60)));
        card.setElevation(dp(a, 2));
        return card;
    }

    public static TextView section(Activity a, String value) {
        TextView t = text(a, value.toUpperCase(), 11, Color.rgb(137, 155, 255), true);
        t.setLetterSpacing(0.08f);
        return t;
    }

    public static LinearLayout.LayoutParams cardParams(Activity a) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(a, 10);
        return p;
    }

    public static int dp(Activity a, int value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    public static void pressScale(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.985f).scaleY(0.985f).setDuration(80).start();
                    break;
                case android.view.MotionEvent.ACTION_CANCEL:
                case android.view.MotionEvent.ACTION_UP:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                    break;
            }
            return false;
        });
    }
}
