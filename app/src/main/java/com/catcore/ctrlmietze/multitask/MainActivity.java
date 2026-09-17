package com.catcore.ctrlmietze.multitask;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends AppCompatActivity {
    private TextView statusTitle;
    private TextView statusHint;
    private View statusDot;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        List<AppEntry> apps = load();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), 0);
        root.setBackgroundColor(Color.rgb(11, 13, 18));

        TextView eyebrow = new TextView(this);
        eyebrow.setText("CATCORE");
        eyebrow.setTextColor(Color.rgb(109, 140, 255));
        eyebrow.setTextSize(12);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText("CatCore MultiTask");
        title.setTextColor(Color.WHITE);
        title.setTextSize(29);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(4);
        root.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Dieselbe App. Derselbe User. Ein neuer Android-Task.");
        subtitle.setTextColor(Color.rgb(174, 183, 202));
        subtitle.setTextSize(14);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        root.addView(subtitle, subtitleParams);

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(16), dp(15), dp(16), dp(15));
        status.setBackground(shape(Color.rgb(24, 29, 41), dp(18)));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(22);
        root.addView(status, statusParams);

        statusDot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(11), dp(11));
        dotParams.rightMargin = dp(13);
        status.addView(statusDot, dotParams);

        LinearLayout statusTexts = new LinearLayout(this);
        statusTexts.setOrientation(LinearLayout.VERTICAL);
        status.addView(statusTexts, new LinearLayout.LayoutParams(0, -2, 1));

        statusTitle = new TextView(this);
        statusTitle.setTextColor(Color.WHITE);
        statusTitle.setTextSize(15);
        statusTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusTexts.addView(statusTitle);

        statusHint = new TextView(this);
        statusHint.setTextColor(Color.rgb(153, 165, 185));
        statusHint.setTextSize(12);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(3);
        statusTexts.addView(statusHint, hintParams);
        updateXposedStatus();

        TextView listHeading = new TextView(this);
        listHeading.setText("DEINE APPS  ·  " + apps.size());
        listHeading.setTextColor(Color.rgb(174, 183, 202));
        listHeading.setTextSize(12);
        listHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(24);
        root.addView(listHeading, headingParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Apps durchsuchen …");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.rgb(135, 145, 165));
        search.setTextSize(15);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(shape(Color.rgb(32, 38, 55), dp(16)));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.topMargin = dp(12);
        searchParams.bottomMargin = dp(8);
        root.addView(search, searchParams);

        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setClipToPadding(false);
        list.setPadding(0, 0, 0, dp(14));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        AppAdapter adapter = new AppAdapter(this, apps);
        list.setAdapter(adapter);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence x, int start, int count, int after) {}
            public void onTextChanged(CharSequence x, int start, int before, int count) {
                adapter.filter(x == null ? "" : x.toString());
            }
            public void afterTextChanged(android.text.Editable e) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusTitle != null) updateXposedStatus();
    }

    public boolean isXposedActive() {
        return false;
    }

    private void updateXposedStatus() {
        boolean active = isXposedActive();
        statusDot.setBackground(shape(active ? Color.rgb(58, 211, 139) : Color.rgb(239, 91, 100), dp(6)));
        statusTitle.setText(active ? "XPosed Verbunden!" : "XPosed nicht verbunden, Modul nicht aktiviert!");
        statusHint.setText(active
                ? "Das Modul ist in CatCore MultiTask aktiv."
                : "Modul in LSPosed aktivieren und CatCore MultiTask zum Scope hinzufügen.");
    }

    private List<AppEntry> load() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        ArrayList<AppEntry> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(query, PackageManager.MATCH_ALL)) {
            if (r.activityInfo == null) continue;
            ApplicationInfo ai = r.activityInfo.applicationInfo;
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (getPackageName().equals(ai.packageName)) continue;
            CharSequence label = r.loadLabel(pm);
            out.add(new AppEntry(label == null ? ai.packageName : label.toString(),
                    ai.packageName, r.activityInfo.name, r.loadIcon(pm)));
        }
        Collator collator = Collator.getInstance();
        out.sort((a, b) -> collator.compare(a.label, b.label));
        return out;
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
