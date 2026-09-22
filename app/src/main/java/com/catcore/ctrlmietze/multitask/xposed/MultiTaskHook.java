package com.catcore.ctrlmietze.multitask.xposed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.catcore.ctrlmietze.multitask.AppTaskRules;
import com.catcore.ctrlmietze.multitask.EnvironmentProbe;
import com.catcore.ctrlmietze.multitask.TaskLauncher;
import com.catcore.ctrlmietze.multitask.window.WindowFramework;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MultiTaskHook implements IXposedHookLoadPackage {
    private static final String SELF = "com.catcore.ctrlmietze.multitask";
    private static volatile long stabilityWindowUntil;
    private static volatile long lastSystemHeartbeatWrite;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (SELF.equals(lpparam.packageName)) {
            try {
                XposedHelpers.findAndHookMethod(
                        SELF + ".EnvironmentProbe",
                        lpparam.classLoader,
                        "isXposedActive",
                        XC_MethodReplacement.returnConstant(true));
                XposedHelpers.findAndHookMethod(
                        SELF + ".MainActivity",
                        lpparam.classLoader,
                        "isXposedActive",
                        XC_MethodReplacement.returnConstant(true));
                XposedBridge.log("MultiTask V2: app-side LSPosed bridge active");
            } catch (Throwable t) {
                XposedBridge.log("MultiTask self probe: " + t);
            }
            return;
        }

        if ("android".equals(lpparam.packageName) || "system".equals(lpparam.packageName)) {
            hookSystemFramework(lpparam);
            return;
        }

        hookScopedTargetApp();
    }

    private static void hookSystemFramework(XC_LoadPackage.LoadPackageParam lpparam) {
        hookV2VirtualWindowPermission(lpparam);

        try {
            Class<?> service = XposedHelpers.findClassIfExists(
                    "com.android.server.wm.ActivityTaskManagerService",
                    lpparam.classLoader);
            if (service == null) {
                XposedBridge.log("MultiTask: ActivityTaskManagerService not found");
                return;
            }

            XposedBridge.hookAllConstructors(service, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    markSystemHookActive(param.thisObject);
                }
            });

            XC_MethodHook markerHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    markSystemHookActive(param.thisObject);

                    for (Object arg : param.args) {
                        if (!(arg instanceof Intent)) continue;
                        Intent intent = (Intent) arg;

                        if (intent.getBooleanExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, false)) {
                            reinforce(intent);
                            break;
                        }

                        scheduleAutomaticTaskRule(param.thisObject, intent);
                        break;
                    }
                }
            };

            XposedBridge.hookAllMethods(service, "startActivityAsUser", markerHook);
            XposedBridge.hookAllMethods(service, "startActivity", markerHook);
            XposedBridge.log("MultiTask: System Framework task reinforcement active");
        } catch (Throwable t) {
            XposedBridge.log("MultiTask framework hook: " + t);
        }
    }

    private static void markSystemHookActive(Object service) {
        try {
            long now = SystemClock.elapsedRealtime();
            long last = lastSystemHeartbeatWrite;
            if (last > 0L && now - last < 30_000L) return;

            synchronized (MultiTaskHook.class) {
                now = SystemClock.elapsedRealtime();
                last = lastSystemHeartbeatWrite;
                if (last > 0L && now - last < 30_000L) return;

                Context context = (Context) XposedHelpers.getObjectField(service, "mContext");
                int boot = Settings.Global.getInt(
                        context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
                Settings.Global.putInt(
                        context.getContentResolver(), EnvironmentProbe.GLOBAL_HOOK_BOOT, boot);
                Settings.Global.putLong(
                        context.getContentResolver(), EnvironmentProbe.GLOBAL_HOOK_UPTIME, now);
                lastSystemHeartbeatWrite = now;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void scheduleAutomaticTaskRule(Object service, Intent original) {
        try {
            if (original == null
                    || original.getBooleanExtra(TaskLauncher.EXTRA_RULE_SPAWN, false)
                    || original.getBooleanExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, false)) {
                return;
            }

            if (!Intent.ACTION_MAIN.equals(original.getAction())
                    || !original.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                return;
            }

            Context context = (Context) XposedHelpers.getObjectField(service, "mContext");
            String pkg = null;

            if (original.getComponent() != null) {
                pkg = original.getComponent().getPackageName();
            } else if (original.getPackage() != null) {
                pkg = original.getPackage();
            } else {
                ResolveInfo resolved = context.getPackageManager()
                        .resolveActivity(original, PackageManager.MATCH_DEFAULT_ONLY);
                if (resolved != null && resolved.activityInfo != null) {
                    pkg = resolved.activityInfo.packageName;
                }
            }

            if (pkg == null || SELF.equals(pkg)) return;

            String rules = Settings.Global.getString(
                    context.getContentResolver(), AppTaskRules.GLOBAL_RULES);
            int desired = AppTaskRules.parseRule(rules, pkg);
            if (desired <= 1) return;

            int callingUid = Binder.getCallingUid();
            int userId = Math.max(0, callingUid / 100000);
            final Object user = resolveUserHandle(userId);
            final String targetPackage = pkg;
            final int targetCount = Math.max(1, Math.min(8, desired));
            final Intent launchTemplate = new Intent(original);
            Handler handler = new Handler(context.getMainLooper());

            // Let the original launcher start settle first. Then fill only the
            // missing task count instead of blindly adding N-1 on every tap.
            handler.postDelayed(() -> ensureAutomaticTaskCount(
                    service,
                    context,
                    targetPackage,
                    targetCount,
                    launchTemplate,
                    user,
                    handler), 320L);
        } catch (Throwable t) {
            XposedBridge.log("MultiTask V2 task rule hook: " + t);
        }
    }

    private static void ensureAutomaticTaskCount(
            Object service,
            Context context,
            String pkg,
            int desired,
            Intent template,
            Object user,
            Handler handler) {
        int counted = countTasksForPackage(service, pkg);
        int effectiveCurrent = counted < 0 ? 1 : Math.max(1, counted);
        int missing = Math.max(0, desired - effectiveCurrent);

        if (missing == 0) {
            XposedBridge.log("MultiTask V2: task rule already satisfied for "
                    + pkg + " (" + effectiveCurrent + "/" + desired + ")");
            return;
        }

        for (int i = 0; i < missing; i++) {
            Intent clone = new Intent(template);
            reinforce(clone);
            clone.putExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, true);
            clone.putExtra(TaskLauncher.EXTRA_RULE_SPAWN, true);
            int delay = 150 * i;

            handler.postDelayed(() -> {
                try {
                    XposedHelpers.callMethod(context, "startActivityAsUser", clone, user);
                } catch (Throwable hiddenApi) {
                    try {
                        clone.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(clone);
                    } catch (Throwable t) {
                        XposedBridge.log("MultiTask V2 automatic task rule: " + t);
                    }
                }
            }, delay);
        }

        XposedBridge.log("MultiTask V2: filling " + missing
                + " missing task(s) for " + pkg + " toward " + desired);
    }

    private static int countTasksForPackage(Object service, String pkg) {
        try {
            Object root = XposedHelpers.getObjectField(service, "mRootWindowContainer");
            AtomicInteger count = new AtomicInteger();

            Consumer<Object> consumer = task -> {
                try {
                    if (pkg.equals(packageNameForTask(task))) {
                        count.incrementAndGet();
                    }
                } catch (Throwable ignored) {
                }
            };

            try {
                XposedHelpers.callMethod(root, "forAllLeafTasks", consumer, true);
            } catch (Throwable noLeafTraversal) {
                XposedHelpers.callMethod(root, "forAllTasks", consumer, true);
            }
            return count.get();
        } catch (Throwable t) {
            XposedBridge.log("MultiTask V2 task count fallback for " + pkg + ": " + t);
            return -1;
        }
    }

    private static String packageNameForTask(Object task) {
        for (String field : new String[]{"realActivity", "origActivity"}) {
            try {
                Object value = XposedHelpers.getObjectField(task, field);
                if (value instanceof ComponentName) {
                    return ((ComponentName) value).getPackageName();
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            Object top = XposedHelpers.callMethod(task, "getTopNonFinishingActivity");
            if (top != null) {
                Object value = XposedHelpers.getObjectField(top, "packageName");
                if (value instanceof String) return (String) value;
            }
        } catch (Throwable ignored) {
        }

        try {
            Object info = XposedHelpers.callMethod(task, "getTaskInfo");
            for (String field : new String[]{"baseActivity", "topActivity", "origActivity", "realActivity"}) {
                try {
                    Object value = XposedHelpers.getObjectField(info, field);
                    if (value instanceof ComponentName) {
                        return ((ComponentName) value).getPackageName();
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object resolveUserHandle(int userId) {
        try {
            return XposedHelpers.callStaticMethod(UserHandle.class, "of", userId);
        } catch (Throwable noFactory) {
            return XposedHelpers.newInstance(UserHandle.class, userId);
        }
    }

    private static void hookV2VirtualWindowPermission(
            XC_LoadPackage.LoadPackageParam lpparam) {
        hookV2SupervisorClass(
                "com.android.server.wm.ActivityTaskSupervisor",
                lpparam.classLoader);
        hookV2SupervisorClass(
                "com.android.server.wm.ActivityStackSupervisor",
                lpparam.classLoader);
    }

    private static void hookV2SupervisorClass(String className, ClassLoader loader) {
        try {
            Class<?> supervisor = XposedHelpers.findClassIfExists(className, loader);
            if (supervisor == null) return;

            XposedBridge.hookAllMethods(
                    supervisor,
                    "isCallerAllowedToLaunchOnDisplay",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 4) return;
                            if (!(param.args[1] instanceof Integer)
                                    || !(param.args[2] instanceof Integer)) {
                                return;
                            }

                            int callingUid = (Integer) param.args[1];
                            int displayId = (Integer) param.args[2];
                            if (displayId <= 0) return;

                            if (isOwnedV2WindowDisplay(param.thisObject, displayId, callingUid)) {
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log("MultiTask V2: virtual-window permission hook active on "
                    + className);
        } catch (Throwable t) {
            XposedBridge.log("MultiTask V2 display permission hook: " + t);
        }
    }

    private static boolean isOwnedV2WindowDisplay(
            Object supervisor, int displayId, int callingUid) {
        try {
            Object root;
            try {
                root = XposedHelpers.getObjectField(supervisor, "mRootWindowContainer");
            } catch (Throwable oldAndroid) {
                root = XposedHelpers.getObjectField(supervisor, "mRootActivityContainer");
            }

            Object displayContent = XposedHelpers.callMethod(
                    root, "getDisplayContentOrCreate", displayId);
            if (displayContent == null) return false;

            Object display = XposedHelpers.getObjectField(displayContent, "mDisplay");
            String name = String.valueOf(XposedHelpers.callMethod(display, "getName"));
            if (!name.startsWith(WindowFramework.DISPLAY_PREFIX)) return false;

            int ownerUid = -1;
            try {
                ownerUid = ((Number) XposedHelpers.callMethod(
                        display, "getOwnerUid")).intValue();
            } catch (Throwable hiddenMethod) {
                Object info = XposedHelpers.callMethod(display, "getDisplayInfo");
                ownerUid = XposedHelpers.getIntField(info, "ownerUid");
            }

            if (ownerUid < 0) return false;

            Object service = XposedHelpers.getObjectField(supervisor, "mService");
            android.content.Context context = (android.content.Context)
                    XposedHelpers.getObjectField(service, "mContext");

            String[] packages = context.getPackageManager().getPackagesForUid(ownerUid);
            boolean ownedByMultiTask = false;
            if (packages != null) {
                for (String pkg : packages) {
                    if (SELF.equals(pkg)) {
                        ownedByMultiTask = true;
                        break;
                    }
                }
            }

            if (!ownedByMultiTask) return false;

            // The normal V2 path calls from the MultiTask UID itself. Root/shell are
            // accepted only as a compatibility fallback and only for our owned display.
            return callingUid == ownerUid || callingUid == 0 || callingUid == 2000;
        } catch (Throwable t) {
            XposedBridge.log("MultiTask V2 display ownership check: " + t);
            return false;
        }
    }

    private static void hookScopedTargetApp() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            Intent incoming = activity.getIntent();
                            if (incoming != null
                                    && incoming.getBooleanExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, false)
                                    && incoming.getBooleanExtra(TaskLauncher.EXTRA_MAX_STABILITY, false)) {
                                stabilityWindowUntil = SystemClock.elapsedRealtime() + 5000L;
                                XposedBridge.log("MultiTask: Max Stability redirect window active for "
                                        + activity.getPackageName());
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "startActivityForResult",
                    Intent.class,
                    int.class,
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (SystemClock.elapsedRealtime() > stabilityWindowUntil) return;
                            Activity activity = (Activity) param.thisObject;
                            Intent next = (Intent) param.args[0];
                            if (next == null || !belongsToSamePackage(activity, next)) return;

                            reinforce(next);
                            next.putExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, true);
                            next.putExtra(TaskLauncher.EXTRA_MAX_STABILITY, true);
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onPostResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                injectQuickButton((Activity) param.thisObject);
                            } catch (Throwable t) {
                                XposedBridge.log("MultiTask quick button: " + t);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("MultiTask target hook: " + t);
        }
    }

    private static boolean belongsToSamePackage(Activity activity, Intent intent) {
        try {
            if (intent.getComponent() != null) {
                return activity.getPackageName().equals(intent.getComponent().getPackageName());
            }
            if (intent.getPackage() != null) {
                return activity.getPackageName().equals(intent.getPackage());
            }
            ResolveInfo info = activity.getPackageManager()
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return info != null
                    && info.activityInfo != null
                    && activity.getPackageName().equals(info.activityInfo.packageName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void reinforce(Intent intent) {
        int flags = intent.getFlags();
        flags |= Intent.FLAG_ACTIVITY_NEW_TASK;
        flags |= Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        flags |= Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS;
        flags &= ~Intent.FLAG_ACTIVITY_CLEAR_TOP;
        flags &= ~Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
        flags &= ~Intent.FLAG_ACTIVITY_SINGLE_TOP;
        intent.setFlags(flags);
    }

    private static void injectQuickButton(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;

        ViewGroup root = (ViewGroup) content;
        if (root.findViewWithTag("MULTITASK_QUICK_BUTTON") != null) return;

        TextView button = new TextView(activity);
        button.setTag("MULTITASK_QUICK_BUTTON");
        button.setText("Ⅱ");
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(18);
        button.setElevation(dp(activity, 8));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(238, 76, 105, 229));
        background.setCornerRadius(dp(activity, 16));
        button.setBackground(background);

        ViewGroup.LayoutParams params;
        if (root instanceof FrameLayout) {
            FrameLayout.LayoutParams frame = new FrameLayout.LayoutParams(
                    dp(activity, 48), dp(activity, 48));
            frame.gravity = Gravity.TOP | Gravity.END;
            frame.topMargin = dp(activity, 14);
            frame.rightMargin = dp(activity, 14);
            params = frame;
        } else {
            params = new ViewGroup.LayoutParams(dp(activity, 48), dp(activity, 48));
        }

        root.addView(button, params);
        button.setOnClickListener(v -> {
            Intent launch = activity.getPackageManager()
                    .getLaunchIntentForPackage(activity.getPackageName());
            if (launch == null || launch.getComponent() == null) return;

            Intent proxy = new Intent();
            proxy.setClassName(SELF, SELF + ".LaunchProxyActivity");
            proxy.putExtra("target_package", launch.getComponent().getPackageName());
            proxy.putExtra("target_activity", launch.getComponent().getClassName());
            proxy.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(proxy);
        });
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
