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

## ✨ V1.5.0.1

- **Smarter app launcher** – resolves launcher aliases and multiple candidate activities instead of trusting one stored MainActivity.
- **Current-user aware** – no hard-coded Android user 0.
- **Learned launch strategy** – the first successful start path is cached per app for faster later launches.
- **Compatibility Mode** – briefly analyzes a problematic app and expands the fallback chain.
- **Max Stability** – enables the widest launch fallback chain.
- **Task Manager** – View All / View MultiTask, inspect running user-app tasks and open 1–8 tasks.
- **Close all MultiTask** – collapses duplicate-task apps back to one reopened instance.
- **Runtime settings** – optional cached/phantom process tuning through Android `device_config`, restorable after boot.
- **LSPosed System Framework scope** – MultiTask-marked launches receive task flags centrally without selecting every target app.
- **Optional per-app quick button** – add a specific target app to the LSPosed scope only when you want the in-app `Ⅱ` shortcut.

## ▶ Start an app

There is no separate “Quick Start” flow anymore.

1. Open **MultiTask**.
2. Find the app you want.
3. Tap the **▶ Play button** next to it.
4. If an app fails, MultiTask shows the reason and offers **Open settings**.
5. Enable **Compatibility Mode** only for apps that need it; use **Max Stability** as the final fallback.

## 🧩 LSPosed setup

Enable the module and keep **MultiTask + System Framework** in its scope. Normal launcher use does **not** require selecting every app individually.

Select a specific target app only if you want MultiTask's optional in-app quick button. SystemUI is not required for launching.

> Android still has the final say over task creation. Apps declaring modes such as `singleTask`, `singleInstance` or `documentLaunchMode="never"` can force task reuse.

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
| Version | `1.5.0.1` (`versionCode 15001`) |
| Android | 9–16 / API 28–36 |
| Language | Java 17 |
| Xposed API | 82 |
| Default LSPosed scope | MultiTask + System Framework |

## 🔒 Privacy

MultiTask contains no ads, tracking or telemetry. It does not replace system files or modify third-party APKs. Optional runtime tuning uses Android's configuration interface and can be reset to system defaults.
