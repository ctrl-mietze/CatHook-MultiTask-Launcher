# How to start

## 1. Install MultiTask

Install the signed release APK. Releases from v1.5.0.1 onward use the same permanent CatCore signing key and can be installed as normal updates.

## 2. Enable the LSPosed module

1. Open LSPosed.
2. Enable **MultiTask**.
3. Keep **MultiTask + System Framework** in scope.
4. Perform a soft/userspace reboot when MultiTask reports that the running system hook is stale.

You do **not** need to add every target app to LSPosed scope for normal launching.

## 3. Start the CatCore Framework

Open MultiTask and keep **Keep MultiTask Framework active** enabled. The framework maintains the app catalog, session state, compatibility services and Stability Guards.

Tapping the persistent framework notification opens **Framework Viewer**.

## 4. Start an app

1. Open **App Starter**.
2. Select the desired task count.
3. Press **START**.
4. Use **Task Manager** to inspect the resulting Android tasks.

For the windowed CatCore path, choose **Start as my task**. For native Android tasks owned by the target app, choose **Start as app's own task**.

## 5. Manage it

Open **Task Manager → Manage → Manage Activity** to move from task telemetry into the CatCore Workspace/window surface for deeper activity management.

Next: [Start modes](START_MODES.md)
