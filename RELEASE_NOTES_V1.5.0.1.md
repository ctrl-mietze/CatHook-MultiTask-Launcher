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

## Important update note

For this release only, users upgrading from **v0.1.0** need to uninstall the old app before installing **v1.5.0.1**. The previous release was signed with a temporary build key, while v1.5.0.1 introduces MultiTask's new permanent release-signing key.

This is a one-time migration. Starting with **v1.5.0.1**, future MultiTask releases signed with the same key can be installed normally over the existing app, so settings and app data can remain in place during updates.

## Versioning

- First digit: major generation
- Second digit: larger update within the generation
- Third digit: smaller feature update
- Fourth digit: bug-fix update

The planned v2 window-framework API is not part of this release.
