# CatCore MultiTask V2.0.0.0

A new generation of MultiTask.

V2 turns the project from a duplicate-task launcher into a complete CatCore task and window environment while keeping Android's own task model available as a first-class start method.

## ✨ Highlights

### Two real start modes

**Start as my task** opens apps inside the CatCore Workspace using MultiTask-owned VirtualDisplay sessions.

**Start as app's own task** uses the LSPosed-native system task bridge to work with Android's real task graph and create only the missing tasks needed to reach the requested ×1–×8 target.

Both modes remain separate. V2 does not silently replace one with the other.

### 🪟 CatCore Workspace

- live app content inside CatCore windows
- move, resize, maximize and close
- focus handling and workspace layouts
- Cascade / Grid / Columns
- edge snapping
- keyboard/text routing
- multi-touch bridge
- persisted workspace sessions
- **Manage Activity** from Task Manager back into the Workspace

### 🧠 CatCore Framework

A dedicated lightweight `:framework` process now owns the long-lived app-side framework state.

- persistent app catalog
- package-change refresh
- session tracking
- Framework Viewer from the framework notification
- live task/session/RAM information
- Stability Guard
- **Stability Guard+** for CatCore window/activity sessions
- conservative recovery with grace periods rather than aggressive task killing

### 🧩 Compatibility first

- Compatibility Mode
- optional Full Scan compatibility profiles
- launcher/MainActivity and activity-behavior analysis
- exact task-count verification
- learned/bounded fallbacks
- manual process controls only when explicitly enabled
- framework-managed defaults when manual tuning is off
- Legacy / Easy Mode for the classic app-first workflow on the modern V2 backend

### 🛠 Task Manager

- live Apps / Tasks / Multi / RAM dashboard
- View All / View MultiTask
- task/process/display telemetry
- open additional sessions
- close duplicates while preserving a primary task
- force stop
- **Manage Activity** for deep CatCore Workspace management
- preserved filter and scroll state

### 🌱 Optional Root Module

The former Root Helper module is now presented as **Optional Root Module**.

The module is systemless and mountless. Root-backed task creation remains an explicit fallback behind:

**Optional Root Module → Root Helper → Root Task Start**

The target app keeps its own Android UID; a root-side start does not magically give the target app root privileges.

### 🎨 UI

**UI Created UI (idk i like it)**

V2 has a complete CatCore visual redesign: compact Home dashboard, custom CatCore dialogs, animated Settings, App Starter, Task Manager, Framework Viewer, Workspace, setup flow and a dedicated Legacy / Easy interface.

## 📖 Tutorials

Start with **[How to start](docs/tutorials/START_HERE.md)**.

The documentation is intentionally split into focused files:

- [Start as my task](docs/tutorials/START_AS_MY_TASK.md)
- [Start as app's own task](docs/tutorials/START_AS_APP_OWN_TASK.md)
- [Optional Root Module](docs/tutorials/ROOT_MODULE.md)
- [Legacy / Easy Mode](docs/tutorials/LEGACY_EASY_MODE.md)

## 🔄 Updating from V1.5.0.1

V2 uses the same permanent CatCore release signing identity introduced with V1.5.0.1, so it is designed to install as a normal update over V1.5.0.1.

After updating, re-check LSPosed scope and perform a userspace/soft reboot when the already-running System Framework process needs to load the new hook implementation.

## 🔒 Design principles

- no third-party APK modification
- no permanent system-partition replacement
- bounded task targets
- system-server bridge protected by the app's signature permission
- root features remain optional
- compatibility fallbacks are explicit
- normal target-app permissions and UID boundaries remain intact

## Technical

- Package: `com.catcore.ctrlmietze.multitask`
- Version: `2.0.0.0`
- versionCode: `20008`
- Android: API 28–36 / Android 9–16
- Java: 17
- Xposed API: 82
- Release signing: permanent CatCore key

Thanks for testing the V2 development builds. This release is the point where the CatCore Framework, Workspace, Task Manager and Android own-task bridge become one coherent MultiTask product. ❤️
