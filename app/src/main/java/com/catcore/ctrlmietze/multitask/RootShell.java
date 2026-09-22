package com.catcore.ctrlmietze.multitask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public final class RootShell {
    public static final class Result {
        public final boolean ok;
        public final int code;
        public final String output;

        Result(boolean ok, int code, String output) {
            this.ok = ok;
            this.code = code;
            this.output = output == null ? "" : output.trim();
        }
    }

    private RootShell() {}

    public static Result run(String command, int timeoutSeconds) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() < 12000) out.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(Math.max(2, timeoutSeconds), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new Result(false, -2, "Root command timed out");
            }
            int code = process.exitValue();
            String text = out.toString().trim();
            boolean badText = containsFailure(text);
            return new Result(code == 0 && !badText, code, text);
        } catch (Throwable t) {
            if (process != null) process.destroy();
            return new Result(false, -1, t.getClass().getSimpleName() + ": " + safeMessage(t));
        }
    }

    private static boolean containsFailure(String text) {
        if (text == null) return false;
        String s = text.toLowerCase();
        return s.contains("error:")
                || s.contains("securityexception")
                || s.contains("permission denial")
                || s.contains("unable to resolve intent")
                || s.contains("does not exist")
                || s.contains("exception occurred while executing");
    }

    public static String quote(String value) {
        if (value == null) return "''";
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String shortReason(Result result) {
        if (result == null) return "Unknown error";
        if (result.output.isEmpty()) return "Exit code " + result.code;
        String first = result.output.replace('\r', ' ').replace('\n', ' ').trim();
        return first.length() > 260 ? first.substring(0, 260) + "…" : first;
    }

    private static String safeMessage(Throwable t) {
        return t.getMessage() == null ? "no message" : t.getMessage();
    }
}
