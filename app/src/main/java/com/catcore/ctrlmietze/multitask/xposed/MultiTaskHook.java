package com.catcore.ctrlmietze.multitask.xposed;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.catcore.ctrlmietze.multitask.TaskLauncher;
import com.catcore.ctrlmietze.multitask.window.WindowFramework;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MultiTaskHook implements IXposedHookLoadPackage {
    private static final String SELF = "com.catcore.ctrlmietze.multitask";
    private static volatile long stabilityWindowUntil;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (SELF.equals(lpparam.packageName)) {
            try {
                XposedHelpers.findAndHookMethod(
                        SELF + ".MainActivity",
                        lpparam.classLoader,
                        "isXposedActive",
                        XC_MethodReplacement.returnConstant(true));
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

            XC_MethodHook markerHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    for (Object arg : param.args) {
                        if (!(arg instanceof Intent)) continue;
                        Intent intent = (Intent) arg;
                        if (!intent.getBooleanExtra(TaskLauncher.EXTRA_FORCE_MULTITASK, false)) {
                            continue;
                        }
                        reinforce(intent);
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
