# CatHook Termux Repository

Public package feed for Raphael's Termux tools.

The repository lives on the isolated `termux-repo` branch so the CatHook app source on `main` remains untouched.

## One-time repository registration

```bash
mkdir -p "$PREFIX/etc/apt/sources.list.d"
printf '%s\n' 'deb [trusted=yes] https://raw.githubusercontent.com/ctrl-mietze/CatHook-MultiTask-Launcher/termux-repo/ ./' > "$PREFIX/etc/apt/sources.list.d/cathook-termux.list"
pkg update
```

After that the bridge is a normal Termux package:

```bash
pkg install termux-android-bridge
pkg upgrade
pkg reinstall termux-android-bridge
apt policy termux-android-bridge
```

Current stable package: `termux-android-bridge 0.2.0`.

The first repository iteration uses APT's `trusted=yes` mode. A dedicated signing key can be added later without changing the package name.
