<p align="center">
  <img src="docs/assets/banner.svg" alt="CatCore MultiTask" width="100%">
</p>

<p align="center">
  <a href="https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/releases/latest"><img src="https://img.shields.io/github/v/release/ctrl-mietze/CatHook-MultiTask-Launcher?style=for-the-badge&color=F59E42&label=Release" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-9–16-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9–16">
  <img src="https://img.shields.io/badge/Root-required-EA580C?style=for-the-badge" alt="Root required">
  <img src="https://img.shields.io/badge/LSPosed-optional-7C3AED?style=for-the-badge" alt="LSPosed optional">
</p>

<p align="center"><strong>Starte dieselbe Android-App im selben Benutzerprofil als zusätzlichen Task.</strong></p>

CatCore MultiTask ist ein fokussierter Android-Launcher mit Root-Unterstützung und optionalem LSPosed-Schnellbutton. Die App zeigt deine installierten User-Apps in einer durchsuchbaren Oberfläche und startet die ausgewählte Activity mit Androids MultiTask- und Document-Flags.

## ✨ Features

- **Zusätzliche Android-Tasks** – öffnet eine App erneut, ohne APK-Klon oder zweites Benutzerprofil.
- **Root-Start** – verwendet `am start` mit `NEW_TASK`, `MULTIPLE_TASK`, `NEW_DOCUMENT` und `RETAIN_IN_RECENTS`.
- **Sicherer Fallback** – fällt auf einen normalen Android-Intent zurück, wenn Root nicht verfügbar ist.
- **Saubere App-Auswahl** – listet ausschließlich installierte User-Apps und unterstützt Suche nach Name oder Paket.
- **LSPosed Quick Action** – blendet in ausgewählten Apps einen kompakten `Ⅱ`-Button für einen weiteren Task ein.
- **Moderner Dark Mode** – klare Statusanzeige, App-Icons und aufgeräumte Bedienung.

## 📱 Installation

1. Öffne den [neuesten Release](https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/releases/latest).
2. Lade `CatCore-MultiTask-v0.1.0.apk` herunter und installiere die APK.
3. Gewähre beim ersten Start Root-Zugriff.
4. Optional: Aktiviere das Modul in LSPosed und füge **CatCore MultiTask** sowie gewünschte Ziel-Apps zum Scope hinzu.
5. Starte die betroffenen Apps neu.

## 🧩 So funktioniert es

| Modus | Verhalten |
|---|---|
| Launcher | Wähle eine User-App aus; CatCore startet deren Launcher-Activity mit MultiTask-Flags. |
| Root | Führt den Start über `su -c am start` aus, damit alle vorgesehenen Activity-Flags gesetzt werden. |
| LSPosed | Injiziert optional einen kleinen Schnellbutton in die vom Benutzer ausgewählten Ziel-Apps. |

> Android und die jeweilige Ziel-App entscheiden am Ende über die Task-Erstellung. Apps mit `singleTask`, `singleInstance` oder `documentLaunchMode="never"` können einen vorhandenen Task wiederverwenden.

## 🛠️ Selbst bauen

### Android Studio

1. Repository klonen.
2. Projekt in Android Studio öffnen und Gradle synchronisieren.
3. Die Konfiguration `app` bauen.

```bash
gradle :app:assembleDebug
```

Die APK liegt anschließend unter `app/build/outputs/apk/debug/app-debug.apk`.

### Termux

Das enthaltene Build-Script richtet die benötigten Pakete ein, prüft ein API-36-SDK und legt APK sowie Build-Log in `Download` ab.

```bash
chmod +x build-termux.sh
./build-termux.sh
```

## ⚙️ Technische Daten

| Eigenschaft | Wert |
|---|---|
| Paketname | `com.catcore.ctrlmietze.multitask` |
| Version | `0.1.0` (`versionCode 1`) |
| Min SDK | Android 9 / API 28 |
| Target SDK | Android 16 / API 36 |
| Sprache | Java 17 |
| UI | AndroidX AppCompat + RecyclerView |
| Xposed API | 82 |

## 🔒 Datenschutz

CatCore MultiTask enthält keine Werbung, kein Tracking und keine Telemetrie. Die App verarbeitet lokal die Liste installierter Launcher-Apps und überschreibt weder Systemdateien noch fremde APKs.

---

<p align="center">
  <img src="docs/assets/catcore-logo.png" alt="CatCore Logo" width="140"><br>
  <strong>Built for Android multitasking.</strong>
</p>
