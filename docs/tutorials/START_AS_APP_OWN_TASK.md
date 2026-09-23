# Start as app's own task

This mode asks Android to create additional **real tasks belonging to the target app**.

## Normal V2 path

The default path uses MultiTask's LSPosed system bridge inside `system_server`:

1. MultiTask sends a signature-protected task request.
2. The hook reads Android's real task graph.
3. It resolves the target launcher activity.
4. It creates only the missing tasks required to reach the selected target.
5. It counts the task graph again and reports the verified result.

A target of ×3 therefore means **reach three real tasks**, not blindly run three launch commands.

## Start

1. Choose **Start as app's own task** on Home.
2. Open **App Starter**.
3. Choose ×1–×8.
4. Tap **START**.

## Compatibility

Android and the target app still control activity/task semantics. Apps using restrictive `launchMode`, `documentLaunchMode`, aliases or custom lifecycle logic may reuse an existing task.

Compatibility Mode and Full Scan can help MultiTask learn the package's activity structure without modifying the target APK.

The optional root task path is deliberately separate and must be explicitly enabled; see [Optional Root Module](ROOT_MODULE.md).
