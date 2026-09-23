# MultiTask V2 Window Framework

This branch is an early V2 development line. It is intentionally kept separate from the stable V1 release.

## Goal

V2 introduces a MultiTask-owned window layer instead of relying only on repeated Android task launches.

The first implementation uses one virtual display per MultiTask window:

- MultiTask owns the display and renders it into a TextureView.
- Target apps are launched onto that display.
- Each window can be moved, resized, maximized and closed inside the MultiTask workspace.
- Touch is routed to the matching display ID.
- Up to eight windows can coexist in the current workspace.
- If the V2 display path is unavailable, MultiTask can fall back to Android freeform and then the V1 task launcher.

## System Framework hook

Android normally restricts launching arbitrary third-party activities onto app-owned virtual displays.

The V2 LSPosed hook changes this only for displays that satisfy all of these conditions:

1. The display name starts with `CatCore.MultiTask.Window:`.
2. The display owner UID belongs to `com.catcore.ctrlmietze.multitask`.
3. The caller is the MultiTask UID itself, root, or shell.

No system files are patched and the behavior disappears when the LSPosed module is disabled.

## Current development status

Implemented:

- WindowFramework API
- WindowHostActivity workspace
- draggable/resizable/maximizable windows
- virtual-display rendering
- display-targeted app launch
- display-targeted touch forwarding
- strict MultiTask-owned display permission hook
- freeform fallback
- classic V1 task fallback
- maximum 8 concurrent workspace windows

Implemented in the V2 major branch:

- keyboard/text routing into a selected virtual display
- signature-protected temporary system_server input bridge for multi-touch
- workspace/session recovery after app-process death
- window focus controls
- edge snapping plus Cascade/Grid/Columns layouts
- live framework-session notification state
- compatibility telemetry and diagnostic export
- workspace UI animations and state persistence

Still hardware-validation dependent before stable V2:

- OEM-specific validation on Samsung/Huawei/AOSP
- final tuning of multi-touch/IME behavior on real devices
- audio remains Android/package scoped; MultiTask does not fake per-window audio isolation for arbitrary third-party apps

## Version

Current development builds use `2.0.0.0-dev2`. Stable V2 will use the final V2 version according to the project versioning scheme.
