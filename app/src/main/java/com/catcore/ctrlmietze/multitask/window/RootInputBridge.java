package com.catcore.ctrlmietze.multitask.window;

import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class RootInputBridge {
    private static final RootInputBridge INSTANCE = new RootInputBridge();

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Process process;
    private BufferedWriter writer;
    private long lastMoveAt;

    static RootInputBridge get() {
        return INSTANCE;
    }

    private RootInputBridge() {}

    void sendMotion(int displayId, MotionEvent event, int contentWidth, int contentHeight) {
        if (displayId < 0 || contentWidth <= 0 || contentHeight <= 0) return;

        int action = event.getActionMasked();
        String verb;
        if (action == MotionEvent.ACTION_DOWN) {
            verb = "DOWN";
        } else if (action == MotionEvent.ACTION_MOVE) {
            long now = android.os.SystemClock.uptimeMillis();
            if (now - lastMoveAt < 18L) return;
            lastMoveAt = now;
            verb = "MOVE";
        } else if (action == MotionEvent.ACTION_UP) {
            verb = "UP";
        } else if (action == MotionEvent.ACTION_CANCEL) {
            verb = "CANCEL";
        } else {
            return;
        }

        float x = Math.max(0f, Math.min(contentWidth - 1f, event.getX()));
        float y = Math.max(0f, Math.min(contentHeight - 1f, event.getY()));
        String command = String.format(Locale.US,
                "input -d %d touchscreen motionevent %s %.1f %.1f",
                displayId, verb, x, y);

        io.execute(() -> write(command));
    }

    void sendText(int displayId, String text) {
        if (displayId < 0 || text == null || text.isEmpty()) return;
        String escaped = text
                .replace("%", "%s")
                .replace(" ", "%s")
                .replace("&", "\\&")
                .replace(";", "\\;")
                .replace("|", "\\|")
                .replace("<", "\\<")
                .replace(">", "\\>");
        io.execute(() -> write("input -d " + displayId + " text " + escaped));
    }

    void sendKey(int displayId, int keyCode) {
        if (displayId < 0 || keyCode <= 0) return;
        io.execute(() -> write("input -d " + displayId + " keyevent " + keyCode));
    }

    private synchronized void write(String command) {
        try {
            ensureSession();
            writer.write(command);
            writer.newLine();
            writer.flush();
        } catch (Throwable first) {
            closeSession();
            try {
                ensureSession();
                writer.write(command);
                writer.newLine();
                writer.flush();
            } catch (Throwable ignored) {
                closeSession();
            }
        }
    }

    private void ensureSession() throws Exception {
        if (process != null && process.isAlive() && writer != null) return;

        process = new ProcessBuilder("su")
                .redirectErrorStream(true)
                .start();
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        Process current = process;
        Thread drain = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(current.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Keep the persistent root shell output pipe drained.
                }
            } catch (Throwable ignored) {
            }
        }, "MultiTask-V2-InputDrain");
        drain.setDaemon(true);
        drain.start();
    }

    private void closeSession() {
        try {
            if (writer != null) writer.close();
        } catch (Throwable ignored) {}
        try {
            if (process != null) process.destroy();
        } catch (Throwable ignored) {}
        writer = null;
        process = null;
    }
}
