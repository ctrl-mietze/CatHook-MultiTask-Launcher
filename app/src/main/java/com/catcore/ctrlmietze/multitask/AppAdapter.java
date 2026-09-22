package com.catcore.ctrlmietze.multitask;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.catcore.ctrlmietze.multitask.window.WindowFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {
    private final MainActivity activity;
    private final List<AppEntry> all;
    private final List<AppEntry> shown = new ArrayList<>();

    AppAdapter(MainActivity activity, List<AppEntry> apps) {
        this.activity = activity;
        this.all = apps;
        shown.addAll(apps);
    }

    void filter(String query) {
        shown.clear();
        String text = query.trim().toLowerCase(Locale.ROOT);
        for (AppEntry app : all) {
            if (text.isEmpty()
                    || app.label.toLowerCase(Locale.ROOT).contains(text)
                    || app.packageName.toLowerCase(Locale.ROOT).contains(text)) {
                shown.add(app);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(11), dp(10), dp(11));
        row.setBackground(shape(Color.rgb(20, 24, 33), dp(17)));

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
        params.topMargin = dp(5);
        params.bottomMargin = dp(5);
        row.setLayoutParams(params);

        ImageView icon = new ImageView(activity);
        row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(12);
        labelParams.rightMargin = dp(8);
        row.addView(labels, labelParams);

        TextView name = new TextView(activity);
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);

        TextView packageName = new TextView(activity);
        packageName.setTextColor(Color.rgb(143, 156, 180));
        packageName.setTextSize(11);
        packageName.setSingleLine(true);
        packageName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(-1, -2);
        packageParams.topMargin = dp(3);
        labels.addView(packageName, packageParams);

        TextView play = new TextView(activity);
        play.setText("▶");
        play.setGravity(Gravity.CENTER);
        play.setTextColor(Color.WHITE);
        play.setTextSize(18);
        play.setContentDescription("Start app");
        play.setBackground(shape(Color.rgb(80, 111, 238), dp(15)));
        row.addView(play, new LinearLayout.LayoutParams(dp(48), dp(48)));

        return new Holder(row, icon, name, packageName, play);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        AppEntry app = shown.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.label);
        holder.packageName.setText(app.packageName);

        View.OnClickListener launch = view -> {
            if (!activity.isXposedActive()) {
                activity.showXposedRequired();
                return;
            }

            WindowFramework.open(
                    activity,
                    app.packageName,
                    app.activityName,
                    app.label);
        };

        holder.play.setOnClickListener(launch);
        holder.itemView.setOnClickListener(launch);
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView play;

        Holder(View itemView, ImageView icon, TextView name,
               TextView packageName, TextView play) {
            super(itemView);
            this.icon = icon;
            this.name = name;
            this.packageName = packageName;
            this.play = play;
        }
    }
}
