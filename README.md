<p align="center">
  <img src="docs/assets/banner.svg" alt="CatCore MultiTask" width="100%">
</p>

<p align="center">
  <strong>English</strong> · <a href="README_DE.md">German</a>
</p>

<p align="center">
  <a href="https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/releases/latest"><img src="https://img.shields.io/github/v/release/ctrl-mietze/CatHook-MultiTask-Launcher?style=for-the-badge&color=F59E42&label=Release" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-9–16-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9–16">
  <img src="https://img.shields.io/badge/Root-required-EA580C?style=for-the-badge" alt="Root required">
  <img src="https://img.shields.io/badge/LSPosed-optional-7C3AED?style=for-the-badge" alt="LSPosed optional">
</p>

<p align="center"><strong>Launch the same Android app as an additional task within the same user profile.</strong></p>

CatCore MultiTask is a focused Android launcher with root support and an optional LSPosed quick-action button. It displays your installed user apps in a searchable interface and launches the selected activity using Android's MultiTask and Document flags.

## ✨ Features

- **Additional Android tasks** – opens an app again without cloning the APK or creating another user profile.
- **Root launch** – uses `am start` with `NEW_TASK`, `MULTIPLE_TASK`, `NEW_DOCUMENT` and `RETAIN_IN_RECENTS`.
- **Safe fallback** – falls back to a standard Android intent when root is unavailable.
- **Clean app selection** – lists installed user apps only and supports searching by name or package.
- **LSPosed Quick Action** – adds a compact `Ⅱ` button to selected apps for launching another task.
- **Modern dark mode** – clear status information, app icons and a clean interface.

## 📱 Installation

1. Open the [latest release](https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/releases/latest).
2. Download `CatCore-MultiTask-v0.1.0.apk` and install the APK.
3. Grant root access on the first launch.
4. Optional: Enable the module in LSPosed and add **CatCore MultiTask** and your desired target apps to the scope.
5. Restart the affected apps.

## 🧩 How it works

| Mode | Behavior |
|---|---|
| Launcher | Select a user app; CatCore launches its launcher activity with MultiTask flags. |
| Root | Performs the launch through `su -c am start` so that all intended activity flags are applied. |
| LSPosed | Optionally injects a small quick-action button into the target apps selected by the user. |

> Android and the target app ultimately decide whether a new task is created. Apps using `singleTask`, `singleInstance` or `documentLaunchMode="never"` may reuse an existing task.

## 🛠️ Build it yourself

### Android Studio

1. Clone the repository.
2. Open the project in Android Studio and sync Gradle.
3. Build the `app` configuration.

```bash
gradle :app:assembleDebug
```

The APK will be available at `app/build/outputs/apk/debug/app-debug.apk`.

### Termux

The included build script installs the required packages, checks for an API 36 SDK and places the APK and build log in `Download`.

```bash
chmod +x build-termux.sh
./build-termux.sh
```

## ⚙️ Technical details

| Property | Value |
|---|---|
| Package name | `com.catcore.ctrlmietze.multitask` |
| Version | `0.1.0` (`versionCode 1`) |
| Min SDK | Android 9 / API 28 |
| Target SDK | Android 16 / API 36 |
| Language | Java 17 |
| UI | AndroidX AppCompat + RecyclerView |
| Xposed API | 82 |

## 🔒 Privacy

CatCore MultiTask contains no advertisements, tracking or telemetry. The app processes the list of installed launcher apps locally and does not overwrite system files or third-party APKs.

---

<p align="center">
  <img src="docs/assets/catcore-logo.png" alt="CatCore Logo" width="140"><br>
  <strong>Built for Android multitasking.</strong>
</p>
