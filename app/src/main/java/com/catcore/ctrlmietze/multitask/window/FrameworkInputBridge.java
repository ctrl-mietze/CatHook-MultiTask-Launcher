package com.catcore.ctrlmietze.multitask.window;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;

public final class FrameworkInputBridge {
    public static final String ACTION =
            "com.catcore.ctrlmietze.multitask.action.FRAMEWORK_INPUT";
    public static final String PERMISSION =
            "com.catcore.ctrlmietze.multitask.permission.FRAMEWORK_BRIDGE";

    public static final String TYPE_TOUCH = "touch";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_KEY = "key";

    private static long lastMove;

    private FrameworkInputBridge() {}

    static void sendMotion(Context context, int displayId, MotionEvent event,
                           int contentWidth, int contentHeight) {
        if (displayId < 0 || event == null || contentWidth <= 0 || contentHeight <= 0) {
            return;
        }

        int actionMasked = event.getActionMasked();
        if (actionMasked == MotionEvent.ACTION_MOVE) {
            long now = SystemClock.uptimeMillis();
            if (now - lastMove < 28L) return;
            lastMove = now;
        }

        int count = Math.min(10, event.getPointerCount());
        int[] ids = new int[count];
        int[] tools = new int[count];
        float[] xs = new float[count];
        float[] ys = new float[count];
        float[] pressures = new float[count];
        float[] sizes = new float[count];

        for (int i = 0; i < count; i++) {
            ids[i] = event.getPointerId(i);
            tools[i] = event.getToolType(i);
            xs[i] = clamp(event.getX(i), 0f, contentWidth - 1f);
            ys[i] = clamp(event.getY(i), 0f, contentHeight - 1f);
            pressures[i] = event.getPressure(i);
            sizes[i] = event.getSize(i);
        }

        Intent intent = base(TYPE_TOUCH, displayId)
                .putExtra("action_masked", actionMasked)
                .putExtra("action_index", Math.min(event.getActionIndex(), count - 1))
                .putExtra("down_time", event.getDownTime())
                .putExtra("event_time", event.getEventTime())
                .putExtra("meta_state", event.getMetaState())
                .putExtra("button_state", event.getButtonState())
                .putExtra("pointer_ids", ids)
                .putExtra("tool_types", tools)
                .putExtra("xs", xs)
                .putExtra("ys", ys)
                .putExtra("pressures", pressures)
                .putExtra("sizes", sizes);
        context.sendBroadcast(intent);
    }

    static void sendText(Context context, int displayId, String text) {
        if (displayId < 0 || text == null || text.isEmpty()) return;
        context.sendBroadcast(base(TYPE_TEXT, displayId).putExtra("text", text));
    }

    static void sendKey(Context context, int displayId, int keyCode) {
        if (displayId < 0 || keyCode <= 0) return;
        context.sendBroadcast(base(TYPE_KEY, displayId).putExtra("key_code", keyCode));
    }

    private static Intent base(String type, int displayId) {
        return new Intent(ACTION)
                .putExtra("type", type)
                .putExtra("display_id", displayId)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
