# MultiTask V1.5.0.1

V1.5.0.1 ist das große Launcher-, UI- und Task-Management-Update.

## Highlights

- Neuer mehrstufiger Activity-/Launcher-Resolver mit mehreren Kandidaten und Fallbacks.
- Erfolgreiche Startmethode wird pro Paket gespeichert.
- Startet im aktuellen Android-User statt fest mit User 0.
- Detaillierte Fehlermeldung mit direktem Weg zu den Einstellungen.
- Neues UI und Home-Screen-Name **MultiTask**.
- Neuer **Task Manager** mit View All / View MultiTask.
- Bis zu **8 Tasks** einer App in einem Durchlauf öffnen.
- **Close all MultiTask** zum kontrollierten Zurückführen von Duplikaten.
- **Compatibility Mode** mit kurzer App-Analyse.
- **Max Stability** mit erweiterter Fallback-Kette.
- Experimenteller **Open apps as child tasks**-Modus.
- Optionale Prozesswerte über Android `device_config`; Wiederherstellung nach Boot/App-Start.
- LSPosed-Standard-Scope: **MultiTask + System Framework**. Ziel-Apps müssen für den normalen Launcher nicht einzeln ausgewählt werden.
- Optionaler `Ⅱ`-Schnellbutton bleibt für explizit ausgewählte Ziel-Apps verfügbar.

## Start

MultiTask öffnen → gewünschte App suchen → rechts den **▶ Play-Button** drücken.

Bei einem Fehler zeigt MultiTask den konkreten letzten Startfehler und bietet **Open settings** an.

> Apps mit restriktivem Manifest-LaunchMode können Android weiterhin dazu zwingen, einen vorhandenen Task wiederzuverwenden.
