# LSPosed integration in CatCore MultiTask V2

Upstream framework: https://github.com/LSPosed/LSPosed

LSPosed is licensed under GPL-3.0 and provides the runtime that injects Xposed-compatible module code into Android processes.

CatCore MultiTask does **not** vendor or repackage the LSPosed Zygisk/Riru/ART runtime inside the APK. That runtime cannot be replaced by ordinary app code and must remain provided by an installed LSPosed-compatible framework.

## What is integrated natively

MultiTask itself is an Xposed/LSPosed module. Its hook entry is:

`com.catcore.ctrlmietze.multitask.xposed.MultiTaskHook`

For V2 **Start as app's own task**, MultiTask no longer starts with a root-shell ActivityManager cascade.

The primary path is:

1. MultiTask sends a signature-protected request from its normal app process.
2. The request reaches the MultiTask hook already loaded by LSPosed inside `system_server`.
3. The hook reads Android's current task state directly from `RootWindowContainer`.
4. It resolves the target launcher activity.
5. It starts only the number of missing tasks required to reach the configured target count.
6. It re-reads task state and reports the verified result to the app through `ResultReceiver`.

This keeps the LSPosed path native to MultiTask while preserving the external LSPosed runtime boundary.

## Safety model

- No `/system`, `/vendor`, `/product` or `/system_ext` files are modified.
- The task bridge exists only while the LSPosed hook is loaded in the running `system_server`.
- The bridge receiver requires MultiTask's signature permission.
- Normal V2 own-task starts do not run the legacy root ActivityManager launch cascade.
- Root launch fallbacks require explicit **Max Stability** mode.
- Every requested task count is bounded to 1–8 and verified against Android's real task graph.

## Upstream API compatibility

LSPosed documents compatibility with the original Xposed Framework API. MultiTask therefore keeps the Xposed API as a `compileOnly` dependency; the runtime implementation is supplied by LSPosed on the device.

No LSPosed source files are copied into the MultiTask APK by this integration.
