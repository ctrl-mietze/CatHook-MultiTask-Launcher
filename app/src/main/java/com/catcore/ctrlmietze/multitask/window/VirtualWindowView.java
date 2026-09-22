package com.catcore.ctrlmietze.multitask.window;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Surface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.catcore.ctrlmietze.multitask.EnvironmentProbe;
import com.catcore.ctrlmietze.multitask.TaskLauncher;

import java.util.UUID;

final class VirtualWindowView extends FrameLayout {
    interface Listener {
        void onClosed(VirtualWindowView window);
    }

    private static final int TITLE_DP = 46;
    private static final int MIN_WIDTH_DP = 270;
    private static final int MIN_HEIGHT_DP = 330;

    private final Activity host;
    private final String packageName;
    private final String activityName;
    private final String label;
    private final Listener listener;
    private final Runnable stateChanged;
    private final String sessionId = UUID.randomUUID().toString();

    private final TextureView texture;
    private final TextView stateText;
    private final FrameLayout content;

    private VirtualDisplay virtualDisplay;
    private Surface displaySurface;
    private int virtualWidth;
    private int virtualHeight;
    private boolean maximized;

    private float dragStartRawX;
    private float dragStartRawY;
    private float dragStartX;
    private float dragStartY;

    private float resizeStartRawX;
    private float resizeStartRawY;
    private int resizeStartWidth;
    private int resizeStartHeight;

    private float normalX;
    private float normalY;
    private int normalWidth;
    private int normalHeight;

    VirtualWindowView(Activity host, String packageName, String activityName,
                      String label, Listener listener, Runnable stateChanged) {
        super(host);
        this.host = host;
        this.packageName = packageName;
        this.activityName = activityName == null ? "" : activityName;
        this.label = label == null || label.trim().isEmpty() ? packageName : label;
        this.listener = listener;
        this.stateChanged = stateChanged;

        setClipChildren(true);
        setClipToPadding(true);
        setBackground(frameBackground(Color.rgb(23, 28, 39), dp(18)));
        setElevation(dp(12));
        setClipToOutline(true);
        setFocusable(true);

        LinearLayout shell = new LinearLayout(host);
        shell.setOrientation(LinearLayout.VERTICAL);
        addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout titleBar = new LinearLayout(host);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(12), 0, dp(7), 0);
        titleBar.setBackgroundColor(Color.rgb(31, 37, 50));
        shell.addView(titleBar, new LinearLayout.LayoutParams(-1, dp(TITLE_DP)));

        LinearLayout titleBlock = new LinearLayout(host);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setGravity(Gravity.CENTER_VERTICAL);
        titleBlock.setPadding(0, dp(3), 0, dp(3));
        LinearLayout.LayoutParams titleBlockParams = new LinearLayout.LayoutParams(0, -1, 1);
        titleBar.addView(titleBlock, titleBlockParams);

        TextView title = text(this.label, 13, Color.WHITE, true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));

        stateText = text("Preparing virtual display…", 9,
                Color.rgb(145, 162, 190), false);
        stateText.setSingleLine(true);
        stateText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleBlock.addView(stateText, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView keyboard = control("⌨");
        keyboard.setTextSize(14);
        titleBar.addView(keyboard, new LinearLayout.LayoutParams(dp(38), dp(34)));
        keyboard.setOnClickListener(v -> showKeyboardBridge());

        TextView focus = control("◎");
        focus.setTextSize(15);
        LinearLayout.LayoutParams focusParams = new LinearLayout.LayoutParams(dp(38), dp(34));
        focusParams.leftMargin = dp(4);
        titleBar.addView(focus, focusParams);
        focus.setOnClickListener(v -> focusWindow());

        TextView maximize = control("□");
        LinearLayout.LayoutParams maxParams = new LinearLayout.LayoutParams(dp(38), dp(34));
        maxParams.leftMargin = dp(4);
        titleBar.addView(maximize, maxParams);
        maximize.setOnClickListener(v -> toggleMaximize());

        TextView close = control("×");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(38), dp(34));
        closeParams.leftMargin = dp(4);
        titleBar.addView(close, closeParams);
        close.setOnClickListener(v -> close());

        titleBlock.setOnTouchListener(this::dragWindow);

        content = new FrameLayout(host);
        content.setBackgroundColor(Color.BLACK);
        shell.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        texture = new TextureView(host);
        texture.setOpaque(true);
        texture.setFocusable(true);
        texture.setFocusableInTouchMode(true);
        content.addView(texture, new FrameLayout.LayoutParams(-1, -1));
        texture.setSurfaceTextureListener(new TextureListener());
        texture.setOnTouchListener((v, event) -> {
            focusWindow();
            if (virtualDisplay == null) return true;

            int displayId = virtualDisplay.getDisplay().getDisplayId();
            if (event.getPointerCount() > 1 && EnvironmentProbe.isSystemHookActive(host)) {
                FrameworkInputBridge.sendMotion(
                        host, displayId, event, virtualWidth, virtualHeight);
            } else {
                RootInputBridge.get().sendMotion(
                        displayId, event, virtualWidth, virtualHeight);
            }
            return true;
        });

        TextView resize = control("◢");
        resize.setTextSize(12);
        resize.setBackgroundColor(Color.argb(170, 31, 37, 50));
        FrameLayout.LayoutParams resizeParams = new FrameLayout.LayoutParams(dp(34), dp(34));
        resizeParams.gravity = Gravity.END | Gravity.BOTTOM;
        content.addView(resize, resizeParams);
        resize.setOnTouchListener(this::resizeWindow);

        setOnClickListener(v -> focusWindow());
    }

    private void createVirtualDisplay(SurfaceTexture surfaceTexture, int width, int height) {
        releaseVirtualDisplay();

        virtualWidth = Math.max(320, width);
        virtualHeight = Math.max(360, height);
        surfaceTexture.setDefaultBufferSize(virtualWidth, virtualHeight);
        displaySurface = new Surface(surfaceTexture);

        DisplayManager manager = (DisplayManager) host.getSystemService(Activity.DISPLAY_SERVICE);
        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

        try {
            virtualDisplay = manager.createVirtualDisplay(
                    WindowFramework.DISPLAY_PREFIX + sessionId,
                    virtualWidth,
                    virtualHeight,
                    host.getResources().getDisplayMetrics().densityDpi,
                    displaySurface,
                    flags);

            if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
                showFailure("Android did not create the virtual display.");
                return;
            }

            int displayId = virtualDisplay.getDisplay().getDisplayId();
            stateText.setText("Display " + displayId + " · launching…");

            VirtualDisplayLauncher.launch(
                    host, displayId, packageName, activityName,
                    (ok, message) -> {
                        if (ok) {
                            stateText.setText("Live · display " + displayId);
                        } else {
                            stateText.setText("Window launch failed");
                            showFailure(message);
                        }
                    });
        } catch (Throwable t) {
            showFailure(t.getClass().getSimpleName() + ": " + safe(t.getMessage()));
        }
    }

    private void resizeVirtualDisplay(int width, int height) {
        if (virtualDisplay == null || width <= 0 || height <= 0) return;
        virtualWidth = Math.max(320, width);
        virtualHeight = Math.max(360, height);
        try {
            virtualDisplay.resize(
                    virtualWidth,
                    virtualHeight,
                    host.getResources().getDisplayMetrics().densityDpi);
        } catch (Throwable ignored) {
        }
    }

    private boolean dragWindow(View view, MotionEvent event) {
        View parent = (View) getParent();
        if (parent == null) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                focusWindow();
                dragStartRawX = event.getRawX();
                dragStartRawY = event.getRawY();
                dragStartX = getX();
                dragStartY = getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (maximized) return true;
                float nx = dragStartX + event.getRawX() - dragStartRawX;
                float ny = dragStartY + event.getRawY() - dragStartRawY;
                float maxX = Math.max(0, parent.getWidth() - getWidth());
                float maxY = Math.max(0, parent.getHeight() - getHeight());
                setX(clamp(nx, 0, maxX));
                setY(clamp(ny, 0, maxY));
                return true;

            case MotionEvent.ACTION_UP:
                if (!maximized) snapAfterDrag(parent);
                notifyStateChanged();
                return true;

            case MotionEvent.ACTION_CANCEL:
                notifyStateChanged();
                return true;
            default:
                return false;
        }
    }

    private void snapAfterDrag(View parent) {
        int threshold = dp(22);
        if (getY() <= threshold) {
            maximizeNow();
            return;
        }

        int gap = dp(5);
        int half = Math.max(dp(MIN_WIDTH_DP), (parent.getWidth() - gap) / 2);
        int height = Math.max(dp(MIN_HEIGHT_DP), parent.getHeight());

        if (getX() <= threshold) {
            applyBounds(0, 0, half, height, true);
        } else if (getX() + getWidth() >= parent.getWidth() - threshold) {
            applyBounds(parent.getWidth() - half, 0, half, height, true);
        }
    }

    private boolean resizeWindow(View view, MotionEvent event) {
        View parent = (View) getParent();
        if (parent == null || maximized) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                focusWindow();
                resizeStartRawX = event.getRawX();
                resizeStartRawY = event.getRawY();
                resizeStartWidth = getWidth();
                resizeStartHeight = getHeight();
                return true;

            case MotionEvent.ACTION_MOVE:
                int maxWidth = Math.max(dp(MIN_WIDTH_DP), parent.getWidth() - Math.round(getX()));
                int maxHeight = Math.max(dp(MIN_HEIGHT_DP), parent.getHeight() - Math.round(getY()));
                int width = Math.round(resizeStartWidth + event.getRawX() - resizeStartRawX);
                int height = Math.round(resizeStartHeight + event.getRawY() - resizeStartRawY);

                ViewGroup.LayoutParams lp = getLayoutParams();
                lp.width = clamp(width, dp(MIN_WIDTH_DP), maxWidth);
                lp.height = clamp(height, dp(MIN_HEIGHT_DP), maxHeight);
                setLayoutParams(lp);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                notifyStateChanged();
                return true;
            default:
                return false;
        }
    }

    private void toggleMaximize() {
        if (maximized) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.width = normalWidth > 0 ? normalWidth : dp(400);
            lp.height = normalHeight > 0 ? normalHeight : dp(600);
            setLayoutParams(lp);
            setX(normalX);
            setY(normalY);
            maximized = false;
            notifyStateChanged();
        } else {
            maximizeNow();
        }
    }

    private void maximizeNow() {
        View parent = (View) getParent();
        if (parent == null) return;

        if (!maximized) {
            normalX = getX();
            normalY = getY();
            normalWidth = getWidth();
            normalHeight = getHeight();
        }

        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.width = Math.max(dp(MIN_WIDTH_DP), parent.getWidth());
        lp.height = Math.max(dp(MIN_HEIGHT_DP), parent.getHeight());
        setLayoutParams(lp);
        setX(0);
        setY(0);
        maximized = true;
        notifyStateChanged();
    }

    void applyBounds(int x, int y, int width, int height, boolean animate) {
        maximized = false;
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.width = Math.max(dp(MIN_WIDTH_DP), width);
        lp.height = Math.max(dp(MIN_HEIGHT_DP), height);
        setLayoutParams(lp);

        if (animate) {
            animate().x(x).y(y).setDuration(180L).start();
        } else {
            setX(x);
            setY(y);
        }
        notifyStateChanged();
    }

    void restoreState(WindowSessionStore.State state) {
        if (state == null) return;
        View parent = (View) getParent();
        if (parent == null) return;

        int width = state.width > 0 ? state.width : getWidth();
        int height = state.height > 0 ? state.height : getHeight();

        if (state.maximized) {
            normalX = state.x;
            normalY = state.y;
            normalWidth = width;
            normalHeight = height;
            maximizeNow();
        } else {
            int maxX = Math.max(0, parent.getWidth() - width);
            int maxY = Math.max(0, parent.getHeight() - height);
            applyBounds(
                    Math.round(clamp(state.x, 0, maxX)),
                    Math.round(clamp(state.y, 0, maxY)),
                    width,
                    height,
                    false);
        }
    }

    WindowSessionStore.State snapshot() {
        return new WindowSessionStore.State(
                packageName,
                activityName,
                label,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                maximized);
    }

    private void focusWindow() {
        bringToFront();
        requestFocus();
        texture.requestFocus();
        setElevation(dp(18));
        animate().scaleX(1.003f).scaleY(1.003f).setDuration(70L)
                .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(90L).start())
                .start();
    }

    private void showKeyboardBridge() {
        if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
            stateText.setText("Keyboard unavailable until display is live");
            return;
        }

        int displayId = virtualDisplay.getDisplay().getDisplayId();
        EditText input = new EditText(host);
        input.setSingleLine(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Type text for " + label);
        input.setPadding(dp(18), dp(10), dp(18), dp(10));

        AlertDialog dialog = new AlertDialog.Builder(host)
                .setTitle("Send keyboard input")
                .setMessage("Text is injected only into this virtual display.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Enter", (d, w) -> sendKey(displayId, KeyEvent.KEYCODE_ENTER))
                .setPositiveButton("Send", null)
                .create();

        dialog.setOnShowListener(x ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String text = input.getText().toString();
                    if (text.isEmpty()) return;
                    sendText(displayId, text);
                    input.setText("");
                }));
        dialog.show();
    }

    private void sendText(int displayId, String value) {
        if (EnvironmentProbe.isSystemHookActive(host)) {
            FrameworkInputBridge.sendText(host, displayId, value);
        } else {
            RootInputBridge.get().sendText(displayId, value);
        }
    }

    private void sendKey(int displayId, int keyCode) {
        if (EnvironmentProbe.isSystemHookActive(host)) {
            FrameworkInputBridge.sendKey(host, displayId, keyCode);
        } else {
            RootInputBridge.get().sendKey(displayId, keyCode);
        }
    }

    private void showFailure(String message) {
        if (content.findViewWithTag("V2_FAILURE") != null) return;

        LinearLayout box = new LinearLayout(host);
        box.setTag("V2_FAILURE");
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackgroundColor(Color.rgb(13, 16, 22));

        TextView heading = text("This app could not enter the V2 window yet.",
                14, Color.WHITE, true);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);

        TextView detail = text(message, 11, Color.rgb(169, 180, 200), false);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(10);
        detailParams.bottomMargin = dp(16);
        box.addView(detail, detailParams);

        Button fallback = new Button(host);
        fallback.setAllCaps(false);
        fallback.setText("Open with fallback");
        fallback.setTextColor(Color.WHITE);
        fallback.setBackground(frameBackground(Color.rgb(76, 105, 229), dp(14)));
        box.addView(fallback, new LinearLayout.LayoutParams(-1, dp(48)));

        fallback.setOnClickListener(v -> {
            fallback.setEnabled(false);
            fallback.setText("Trying freeform…");
            FreeformFallback.open(host, packageName, activityName, (ok, result) -> {
                if (ok) {
                    close();
                    return;
                }

                fallback.setText("Trying classic task…");
                TaskLauncher.launchNewTask(host, packageName, activityName, (classicOk, classicMessage) -> {
                    if (classicOk) {
                        close();
                    } else {
                        fallback.setEnabled(true);
                        fallback.setText("Open with fallback");
                        detail.setText(result + "\n\nClassic fallback: " + classicMessage);
                    }
                });
            });
        });

        content.addView(box, new FrameLayout.LayoutParams(-1, -1));
    }

    String packageName() {
        return packageName;
    }

    void close() {
        releaseVirtualDisplay();
        if (listener != null) listener.onClosed(this);
    }

    private void notifyStateChanged() {
        if (stateChanged != null) stateChanged.run();
    }

    private void releaseVirtualDisplay() {
        try {
            if (virtualDisplay != null) virtualDisplay.release();
        } catch (Throwable ignored) {
        }
        virtualDisplay = null;

        try {
            if (displaySurface != null) displaySurface.release();
        } catch (Throwable ignored) {
        }
        displaySurface = null;
    }

    private TextView control(String value) {
        TextView t = text(value, 18, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(frameBackground(Color.rgb(51, 60, 80), dp(11)));
        return t;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(host);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable frameBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "no details" : value;
    }

    private final class TextureListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            createVirtualDisplay(surface, width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            resizeVirtualDisplay(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            releaseVirtualDisplay();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }
}
