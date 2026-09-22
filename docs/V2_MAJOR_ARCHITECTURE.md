# CatCore MultiTask V2 — Major Architecture

V2 is a major-generation rewrite. The earlier `v2-window-framework` branch is preserved as the first window prototype; this branch (`v2-major-rework`) builds the full V2 product around it.

## Product identity

- Android launcher name: **MultiTask**
- In-app product name: **CatCore MultiTask**
- Package: `com.catcore.ctrlmietze.multitask`
- Development version: `2.0.0.0-dev1`

## Privileged stack

V2 deliberately separates privileges instead of permanently modifying Android system files.

### App root

The first-start flow verifies a normal `su` grant. KernelSU, Magisk and compatible su managers remain responsible for the grant.

### Integrated LSPosed module

The Xposed hook code ships inside the MultiTask APK. The user still activates the module in LSPosed. Recommended scope is MultiTask + Android/System Framework; normal target apps do not need to be selected for the system-server launch path.

The system-server hook is responsible for:

- reinforcing marked MultiTask launches;
- allowing activity placement only on MultiTask-owned V2 virtual displays;
- reporting System Framework hook health for the current boot;
- applying per-app automatic task-count rules to normal launcher starts.

### CatCore MultiTask Framework

`CatCoreFrameworkService` runs inside the same APK in a dedicated `:framework` process.

It is a low-priority foreground service with no polling loop. It owns framework notifications and provides a long-lived app-side anchor for V2 task/window sessions.

### Optional KernelSU Root Helper

The optional module ID is `catcore_multitask_root`.

It is deliberately mountless (`skip_mount`) and does not overlay system partitions. Its broker supports:

- root-backed task starts;
- temporary per-session process priority boost;
- restoring original nice / oom_score_adj values;
- process telemetry snapshots;
- memory trim requests;
- package stop helper.

The target Android application retains its own Android UID. V2 does not pretend that launching an app from root changes that app into UID 0.

## Main start modes

### Start as my task

Uses the MultiTask-owned V2 session path and window framework. The current backend creates a MultiTask-owned VirtualDisplay for each live window and renders it inside the CatCore Workspace.

### Start as app's own task

Keeps the target inside its normal Android task identity. MultiTask uses a learned launch chain with root, explicit components, direct intents and compatibility fallbacks. The optional Root Helper can be preferred for the root-backed start.

## Per-app task rules

Each app can have an automatic task count from 1 to 8.

A count of 1 disables the rule. Rules are stored by MultiTask and mirrored into a root-writable Settings.Global key so the System Framework hook can react when the app is started normally from the launcher.

## Task Manager

V2 no longer depends on one historical dumpsys format.

The inspector understands modern TASK lines and legacy Task/ActivityRecord layouts. It correlates tasks with process telemetry and exposes:

- task count;
- display ID / V2 display state;
- RAM resident-set estimate;
- CPU percentage;
- process count;
- open-more controls;
- individual duplicate-task removal;
- force stop;
- close-all-MultiTask-duplicates.

## Developer Options

Developer Options are guarded by an acknowledgement dialog.

Individual launch methods can be disabled without editing code:

- V2 direct display launch
- V2 root display launch
- Android freeform fallback
- root package/component full launches
- direct component launch
- Android launch intent
- basic compatibility root launches
- final launcher fallback

## First-start flow

1. Welcome
2. Root verification
3. LSPosed activation
4. System Framework hook verification
5. Enable/install the in-APK CatCore Framework
6. Android permissions
7. Main start-mode selection

The setup can be launched again from Settings.

## Stability principle

V2 keeps the stable V1 launch path as a fallback rather than deleting working behavior. No V2 development branch is merged into stable `main` until hardware testing and release-readiness checks are complete.
