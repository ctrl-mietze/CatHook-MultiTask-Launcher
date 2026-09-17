package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public final class AppAdapter extends RecyclerView.Adapter<AppAdapter.H> {
    private final Context context;
    private final List<AppEntry> all;
    private final List<AppEntry> shown = new ArrayList<>();

    AppAdapter(Context context, List<AppEntry> apps) {
        this.context = context;
        all = apps;
        shown.addAll(apps);
    }

    void filter(String query) {
        shown.clear();
        String text = query.trim().toLowerCase();
        for (AppEntry app : all) {
            if (text.isEmpty() || app.label.toLowerCase().contains(text)
                    || app.packageName.toLowerCase().contains(text)) {
                shown.add(app);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public H onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        row.setBackground(shape(Color.rgb(21, 25, 34), dp(18)));
        RecyclerView.LayoutParams rowParams = new RecyclerView.LayoutParams(-1, -2);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(context);
        row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(13);
        labelParams.rightMargin = dp(8);
        row.addView(labels, labelParams);

        TextView name = new TextView(context);
        name.setTextColor(Color.WHITE);
        name.setTextSize(16);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);

        TextView packageName = new TextView(context);
        packageName.setTextColor(Color.rgb(154, 164, 183));
        packageName.setTextSize(12);
        packageName.setSingleLine(true);
        packageName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(-1, -2);
        packageParams.topMargin = dp(3);
        labels.addView(packageName, packageParams);

        TextView play = new TextView(context);
        play.setText("▶");
        play.setGravity(Gravity.CENTER);
        play.setTextColor(Color.WHITE);
        play.setTextSize(22);
        play.setContentDescription("Als neuen Task starten");
        play.setBackground(shape(Color.rgb(109, 140, 255), dp(15)));
        row.addView(play, new LinearLayout.LayoutParams(dp(50), dp(50)));
        return new H(row, icon, name, packageName, play);
    }

    @Override
    public void onBindViewHolder(H holder, int position) {
        AppEntry app = shown.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.label);
        holder.packageName.setText(app.packageName);
        View.OnClickListener launch = view -> TaskLauncher.launchNewTask(context,
                app.packageName, app.activityName,
                (ok, message) -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
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
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static final class H extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView play;

        H(View view, ImageView icon, TextView name, TextView packageName, TextView play) {
            super(view);
            this.icon = icon;
            this.name = name;
            this.packageName = packageName;
            this.play = play;
        }
    }
}
