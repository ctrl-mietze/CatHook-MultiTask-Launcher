# CatCore MultiTask Root Helper

Optional KernelSU module for MultiTask V2.

The module is intentionally systemless and includes a `skip_mount` marker. It does not replace or overlay files in `/system`, `/vendor`, `/product` or `/system_ext`.

## Features

- root-backed task launch helper
- session-only process priority boost
- automatic restoration of nice / oom_score_adj values
- root process telemetry
- app memory trim helper
- task stop helper

The helper validates package-name arguments before executing package-scoped operations.

## Rollback

Disable or uninstall the module from KernelSU and reboot. `uninstall.sh` restores any stored session process values and removes `/data/adb/catcore_multitask`.

## Development build

GitHub Actions creates `CatCore-MultiTask-Root-Helper-v2.zip` from this directory. The ZIP is installable through KernelSU's module installer.
