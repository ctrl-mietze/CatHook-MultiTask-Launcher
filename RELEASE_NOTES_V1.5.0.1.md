# MultiTask v1.5.0.1

Major update of the CatHook MultiTask launcher.

## Highlights

- Much more robust app launching with improved launcher-activity discovery and multiple fallback strategies.
- Remembers successful launch strategies per app for faster subsequent starts.
- Clear launch diagnostics when an app still cannot be opened, including a direct path to settings.
- New Task Manager for active user-app tasks and MultiTask instances.
- Open multiple app tasks with a configurable count up to 8.
- Close all MultiTask instances from the Task Manager.
- New Settings screen with compatibility and stability options.
- Optional child-task behavior and runtime tuning without permanently patching Android system files.
- Improved LSPosed integration and stability-oriented scope guidance.
- Refined UI and launcher name: **MultiTask**.
- Release builds now use a persistent signing key so future versions can be installed as normal updates.

## Signing migration

v1.5.0.1 starts a new permanent release-signing lineage. Because v0.1.0 was built with an ephemeral CI debug key, v0.1.0 may need to be uninstalled once before installing this release. Future versions signed with the same release key can be installed directly over v1.5.0.1.

## Versioning

- First digit: major generation
- Second digit: larger update within the generation
- Third digit: smaller feature update
- Fourth digit: bug-fix update

The planned v2 window-framework API is not part of this release.
