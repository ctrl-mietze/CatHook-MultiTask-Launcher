<p align="center">
  <img src="docs/assets/banner.svg" alt="MultiTask" width="100%">
</p>

<p align="center">
  <a href="README.md">English</a> · <strong>German</strong>
</p>

<p align="center">
  <a href="https://github.com/ctrl-mietze/CatHook-MultiTask-Launcher/releases/latest"><img src="https://img.shields.io/github/v/release/ctrl-mietze/CatHook-MultiTask-Launcher?style=for-the-badge&color=F59E42&label=Release" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-9–16-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9–16">
  <img src="https://img.shields.io/badge/LSPosed-required-7C3AED?style=for-the-badge" alt="LSPosed erforderlich">
</p>

<p align="center"><strong>Öffne dieselbe Android-App im selben Benutzerprofil als zusätzliche Tasks.</strong></p>

MultiTask ist ein Android-MultiTask-Launcher mit LSPosed-Verstärkung im System Framework, optionalem Root-Start und einer lernenden Fallback-Pipeline. Die App prüft mehrere mögliche Launcher-Activities, merkt sich pro Paket die funktionierende Startmethode und nutzt die langsameren Kompatibilitätsprüfungen nur bei Bedarf.

## ✨ V1.5.0.1

- **Deutlich besserer App-Starter** – berücksichtigt Launcher-Aliases und mehrere mögliche Activities statt nur einer gespeicherten MainActivity.
- **Aktueller Android-User** – kein fest verdrahtetes `--user 0` mehr.
- **Gelernte Startmethode** – funktionierende Methoden werden pro App gespeichert und beim nächsten Mal direkt verwendet.
- **Compatibility Mode** – analysiert problematische Apps kurz und erweitert die Fallback-Kette.
- **Max Stability** – aktiviert die umfangreichste Start-Fallback-Kette.
- **Task Manager** – View All / View MultiTask, laufende User-App-Tasks sowie 1–8 neue Tasks.
- **Close all MultiTask** – reduziert Apps mit mehreren Tasks kontrolliert wieder auf eine neu geöffnete Instanz.
- **Runtime-Einstellungen** – optionale Cached-/Phantom-Prozesswerte über Android `device_config`, auf Wunsch nach Neustart wiederhergestellt.
- **LSPosed System Framework** – von MultiTask markierte Starts erhalten die Task-Flags zentral, ohne jede Ziel-App auswählen zu müssen.
- **Optionaler In-App-Schnellbutton** – eine Ziel-App muss nur dann zusätzlich im LSPosed-Scope liegen, wenn dort der `Ⅱ`-Button gewünscht ist.

## ▶ App starten

Der alte „Quick Start“-Ablauf gilt nicht mehr.

1. **MultiTask** öffnen.
2. Gewünschte App suchen.
3. Rechts den **▶ Play-Button** drücken.
4. Schlägt der Start fehl, zeigt MultiTask den konkreten Grund und **Open settings**.
5. **Compatibility Mode** nur für problematische Apps aktivieren; **Max Stability** ist die letzte Stufe.

## 🧩 LSPosed

Modul aktivieren und **MultiTask + System Framework** im Scope lassen. Für den normalen Launcher-Betrieb müssen **nicht** alle Ziel-Apps einzeln ausgewählt werden.

Eine bestimmte Ziel-App nur dann zusätzlich auswählen, wenn dort der optionale In-App-Schnellbutton gewünscht ist. SystemUI ist für den Start nicht erforderlich.

> Android beziehungsweise die Ziel-App entscheidet letztlich über die Task-Erstellung. `singleTask`, `singleInstance` oder `documentLaunchMode="never"` können die Wiederverwendung eines vorhandenen Tasks erzwingen.

## 🛠️ Selbst bauen

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

## ⚙️ Technische Daten

| Eigenschaft | Wert |
|---|---|
| Paket | `com.catcore.ctrlmietze.multitask` |
| Version | `1.5.0.1` (`versionCode 15001`) |
| Android | 9–16 / API 28–36 |
| Sprache | Java 17 |
| Xposed API | 82 |
| Empfohlener Scope | MultiTask + System Framework |

## 🔒 Datenschutz

MultiTask enthält keine Werbung, kein Tracking und keine Telemetrie. Die App ersetzt keine Systemdateien und verändert keine fremden APKs. Optionale Runtime-Werte werden über Androids Konfigurationsschnittstelle gesetzt und können auf Systemstandard zurückgesetzt werden.
