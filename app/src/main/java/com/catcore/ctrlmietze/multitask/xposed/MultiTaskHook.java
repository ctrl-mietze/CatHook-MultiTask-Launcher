package com.catcore.ctrlmietze.multitask.xposed;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.catcore.ctrlmietze.multitask.TaskLauncher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MultiTaskHook implements IXposedHookLoadPackage {
    private static final String SELF = "com.catcore.ctrlmietze.multitask";

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

        try {
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

    private static void hookSystemFramework(XC_LoadPackage.LoadPackageParam lpparam) {
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

                        int flags = intent.getFlags();
                        flags |= Intent.FLAG_ACTIVITY_NEW_TASK;
                        flags |= Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
                        flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
                        flags |= Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS;
                        flags &= ~Intent.FLAG_ACTIVITY_CLEAR_TOP;
                        flags &= ~Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
                        flags &= ~Intent.FLAG_ACTIVITY_SINGLE_TOP;
                        intent.setFlags(flags);
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
