# How to start — CatCore MultiTask V2

This is the short setup path. The deeper guides are split into separate pages so the README stays readable.

## 1. Install and open MultiTask

Install the V2 APK as a normal update when you already use a permanently signed V1.5.0.1+ build.

Open **MultiTask** and complete the guided setup.

## 2. LSPosed

Enable the integrated MultiTask module in LSPosed and include:

- **MultiTask**
- **System Framework / Android**

A userspace/soft reboot is recommended after enabling or changing the System Framework hook so the current `system_server` loads the module.

## 3. CatCore Framework

Keep **CatCore MultiTask Framework** enabled. It provides the dedicated `:framework` process, app-catalog refresh, session state, Stability Guard and Stability Guard+.

Tapping the persistent framework notification opens **Framework Viewer** with live framework/session statistics.

## 4. Pick a start method

Use the mode selector on Home:

- [Start as my task](START_AS_MY_TASK.md) — CatCore Workspace / VirtualDisplay windows.
- [Start as app's own task](START_AS_APP_OWN_TASK.md) — real Android app tasks through the LSPosed system task bridge.

## Optional

- [Optional Root Module](ROOT_MODULE.md)
- [Legacy / Easy Mode](LEGACY_EASY_MODE.md)

> Compatibility varies by app and OEM. MultiTask deliberately keeps multiple bounded fallback paths instead of pretending every Android app accepts duplicate tasks or VirtualDisplay hosting.
