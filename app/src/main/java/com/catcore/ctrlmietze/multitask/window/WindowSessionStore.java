package com.catcore.ctrlmietze.multitask.window;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WindowSessionStore {
    private static final String PREFS = "multitask_v2_window_sessions";
    private static final String KEY = "sessions";

    private WindowSessionStore() {}

    static void save(Context context, List<State> states) {
        JSONArray array = new JSONArray();
        for (State state : states) {
            try {
                JSONObject object = new JSONObject();
                object.put("package", state.packageName);
                object.put("activity", state.activityName);
                object.put("label", state.label);
                object.put("x", state.x);
                object.put("y", state.y);
                object.put("width", state.width);
                object.put("height", state.height);
                object.put("maximized", state.maximized);
                array.put(object);
            } catch (Throwable ignored) {
            }
        }
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    static List<State> load(Context context) {
        String encoded = prefs(context).getString(KEY, "[]");
        if (encoded == null || encoded.isEmpty()) return Collections.emptyList();

        List<State> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(encoded);
            for (int i = 0; i < array.length() && out.size() < 8; i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;

                String pkg = o.optString("package", "");
                if (pkg.isEmpty()) continue;

                out.add(new State(
                        pkg,
                        o.optString("activity", ""),
                        o.optString("label", pkg),
                        (float) o.optDouble("x", 0),
                        (float) o.optDouble("y", 0),
                        o.optInt("width", 0),
                        o.optInt("height", 0),
                        o.optBoolean("maximized", false)));
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static final class State {
        final String packageName;
        final String activityName;
        final String label;
        final float x;
        final float y;
        final int width;
        final int height;
        final boolean maximized;

        State(String packageName, String activityName, String label,
              float x, float y, int width, int height, boolean maximized) {
            this.packageName = packageName;
            this.activityName = activityName == null ? "" : activityName;
            this.label = label == null ? packageName : label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.maximized = maximized;
        }
    }
}
