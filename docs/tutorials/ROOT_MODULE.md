# Optional Root Module

The **CatCore MultiTask Optional Root Module** is not required for the normal V2 LSPosed task bridge.

It is a systemless, mountless helper for explicitly enabled root-backed controls. It does not replace files in `/system`, `/vendor`, `/product` or `/system_ext`.

## Enable

1. Install/activate the CatCore module in a compatible root-module manager.
2. Reboot if your manager requires it.
3. Open **MultiTask → Settings**.
4. Confirm **Optional Root Module** is detected.
5. Only then can **Root Helper** be enabled.
6. **Root Task Start** remains a separate opt-in switch.

The order is intentionally: **Module → Root Helper → Root Task Start**.

## Root task fallback

When explicitly enabled, the module can use root-side ActivityManager commands as a fallback if the normal LSPosed own-task bridge cannot reach the requested task target. MultiTask re-checks the resulting task count.

Starting an app from this broker does **not** turn the target app into UID 0 and does not automatically grant that app root permissions.

## Other module functions

The helper can provide temporary session priority, process telemetry, memory-trim requests and package stop controls. Session priority state is restorable.

## Rollback

Disable/uninstall the module and reboot. The module uninstall path restores stored session priority state and removes its CatCore state directory.
