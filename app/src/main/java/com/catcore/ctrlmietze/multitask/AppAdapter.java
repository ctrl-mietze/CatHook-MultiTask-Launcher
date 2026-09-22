package com.catcore.ctrlmietze.multitask;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.catcore.ctrlmietze.multitask.window.WindowFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {
    private final AppStarterActivity activity;
    private final List<AppEntry> all;
    private final List<AppEntry> shown = new ArrayList<>();

    AppAdapter(AppStarterActivity activity, List<AppEntry> apps) {
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
        LinearLayout row = CatUi.card(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(11), dp(10), dp(11));

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
        params.topMargin = dp(5);
        params.bottomMargin = dp(5);
        row.setLayoutParams(params);

        ImageView icon = new ImageView(activity);
        row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(12);
        labelParams.rightMargin = dp(8);
        row.addView(labels, labelParams);

        TextView name = CatUi.text(activity, "", 15, CatUi.TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);

        TextView packageName = CatUi.text(activity, "", 10, CatUi.MUTED, false);
        packageName.setSingleLine(true);
        packageName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(-1, -2);
        packageParams.topMargin = dp(3);
        labels.addView(packageName, packageParams);

        LinearLayout controls = new LinearLayout(activity);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(controls);

        TextView count = CatUi.pill(activity, "×1", Color.rgb(45, 52, 72));
        controls.addView(count, new LinearLayout.LayoutParams(dp(46), dp(38)));

        TextView play = CatUi.pill(activity, "START", Color.rgb(69, 91, 198));
        play.setTextSize(10);
        play.setLetterSpacing(0.06f);
        play.setContentDescription("Start app");
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(68), dp(44));
        pp.leftMargin = dp(7);
        controls.addView(play, pp);

        CatUi.pressScale(play);
        CatUi.pressScale(count);
        return new Holder(row, icon, name, packageName, count, play);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        AppEntry app = shown.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.label);
        holder.packageName.setText(app.packageName);

        int rule = AppTaskRules.get(activity, app.packageName);
        holder.count.setText("×" + rule);
        holder.count.setBackground(CatUi.shape(activity,
                rule > 1 ? Color.rgb(61, 75, 145) : Color.rgb(45, 52, 72), 999));

        holder.count.setOnClickListener(v ->
                activity.askTaskRule(app, () -> {
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) notifyItemChanged(adapterPosition);
                }));

        View.OnClickListener launch = view -> {
            if (!activity.isLaunchFrameworkReady()) {
                activity.showFrameworkRequired();
                return;
            }

            if (SettingsStore.startMode(activity) == SettingsStore.MODE_MY_TASK) {
                WindowFramework.open(activity, app.packageName, app.activityName, app.label);
                return;
            }

            int desired = Math.max(1, Math.min(
                    8, AppTaskRules.get(activity, app.packageName)));

            activity.showCompatibilityProgress(
                    "LSPosed is preparing " + desired
                            + (desired == 1 ? " task" : " tasks")
                            + " for " + app.label + "…");

            SystemTaskBridge.ensureTaskCount(
                    activity,
                    app.packageName,
                    app.activityName,
                    desired,
                    (ok, before, after, target, message) -> {
                        activity.hideCompatibilityProgress();
                        if (ok) {
                            Toast.makeText(
                                    activity,
                                    target == 1
                                            ? "Opened " + app.label
                                            : "Ready · " + after + "/" + target + " tasks",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (SettingsStore.compatibilityMode(activity)) {
                            activity.showCompatibilityProgress(
                                    "Native LSPosed path failed · trying safe compatibility fallback…");
                            TaskLauncher.launchNewTask(
                                    activity,
                                    app.packageName,
                                    app.activityName,
                                    activity::showCompatibilityProgress,
                                    (fallbackOk, fallbackMessage) -> {
                                        activity.hideCompatibilityProgress();
                                        if (fallbackOk) {
                                            Toast.makeText(
                                                    activity,
                                                    "Opened with compatibility fallback",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            activity.showLaunchFailure(
                                                    app.label,
                                                    message + "\n\nFallback: " + fallbackMessage);
                                        }
                                    });
                        } else {
                            activity.showLaunchFailure(
                                    app.label,
                                    message
                                            + "\n\nThe native LSPosed task bridge is the default V2 path. "
                                            + "Enable Compatibility mode only if this app needs a fallback.");
                        }
                    });
        };

        holder.play.setOnClickListener(launch);
        holder.itemView.setOnClickListener(launch);
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    private int dp(int value) {
        return CatUi.dp(activity, value);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView count;
        final TextView play;

        Holder(View itemView, ImageView icon, TextView name,
               TextView packageName, TextView count, TextView play) {
            super(itemView);
            this.icon = icon;
            this.name = name;
            this.packageName = packageName;
            this.count = count;
            this.play = play;
        }
    }
}
