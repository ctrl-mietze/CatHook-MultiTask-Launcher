<p align="center">
  <img src="docs/assets/banner.svg" alt="MultiTask" width="100%">
</p>

<p align="center">
  <strong>English</strong> · <a href="README_DE.md">German</a>
</p>

<p align="center">
  <a href="https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/releases/latest"><img src="https://img.shields.io/github/v/release/ctrl-mietze/CatHook-MultiTask-Launcher?style=for-the-badge&color=F59E42&label=Release" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-9–16-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9–16">
  <img src="https://img.shields.io/badge/LSPosed-required-7C3AED?style=for-the-badge" alt="LSPosed required">
</p>

<p align="center"><strong>Open the same Android app as additional tasks in the same user profile.</strong></p>

MultiTask is an Android multi-task launcher with LSPosed System Framework reinforcement, optional root-assisted launching and a learned fallback pipeline. It resolves more than one possible launcher activity, remembers which method works for each package and only uses slower compatibility checks when requested.

## ✨ V2.0.0.0

V2 is the CatCore generation of MultiTask: two separate launch models, a MultiTask-owned Workspace, an LSPosed-native Android task bridge, a dedicated `:framework` process, live Task Manager and compatibility tooling.

- **Start as my task** — CatCore Workspace + VirtualDisplay windows.
- **Start as app's own task** — verified real Android task targets through the System Framework bridge.
- **Task Manager + Manage Activity** — inspect tasks and move into deeper Workspace management.
- **CatCore Framework** — app catalog, session state, Framework Viewer, Stability Guard and Stability Guard+.
- **Compatibility first** — Full Scan profiles, bounded fallbacks and framework-managed process defaults.
- **Optional Root Module** — explicit systemless root-backed fallback and telemetry controls.
- **Legacy / Easy Mode** — classic app-first workflow using the current V2 backend.
- **UI Created UI (idk i like it)** — complete CatCore visual redesign with custom dialogs and animated screens.

## ▶ How to start

The README stays short on purpose. The setup and start methods each have their own guide:

- **[How to start](docs/tutorials/START_HERE.md)**
- **[Start as my task](docs/tutorials/START_AS_MY_TASK.md)**
- **[Start as app's own task](docs/tutorials/START_AS_APP_OWN_TASK.md)**
- **[Optional Root Module](docs/tutorials/ROOT_MODULE.md)**
- **[Legacy / Easy Mode](docs/tutorials/LEGACY_EASY_MODE.md)**

## 🧩 LSPosed setup

Enable the integrated MultiTask module and keep **MultiTask + System Framework** in scope. A userspace/soft reboot is recommended whenever the already-running System Framework needs to load a changed hook implementation.

Normal target apps do not need to be selected just to use the V2 system task bridge.

> Android still has final authority over activity/task behavior. Restrictive launch modes, secure surfaces and OEM behavior can limit duplicate tasks or VirtualDisplay hosting.

## 🛠️ Build

### Android Studio

```bash
gradle :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Termux

```bash
chmod +x build-termux.sh
./build-termux.sh
```

## ⚙️ Technical details

| Property | Value |
|---|---|
| Package | `com.catcore.ctrlmietze.multitask` |
| Version | `2.0.0.0` (`versionCode 20008`) |
| Android | 9–16 / API 28–36 |
| Language | Java 17 |
| Xposed API | 82 |
| Default LSPosed scope | MultiTask + System Framework |

## 🔒 Privacy

MultiTask contains no ads, tracking or telemetry. It does not replace system files or modify third-party APKs. Optional runtime tuning uses Android's configuration interface and can be reset to system defaults.
