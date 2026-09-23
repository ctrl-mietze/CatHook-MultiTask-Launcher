# Start as my task

**Start as my task** is the CatCore V2 window path.

## What it does

MultiTask creates a CatCore-owned VirtualDisplay/session and hosts the target activity inside the **CatCore Workspace**. The target remains its normal Android app/UID; the workspace does not grant it MultiTask's privileges.

The workspace supports CatCore window controls including move, resize, maximize, close, focus, snapping and workspace layouts.

## Start

1. Open **MultiTask**.
2. Tap the start-method card and choose **Start as my task**.
3. Open **App Starter**.
4. Select the desired task/window count.
5. Tap **START**.

The Framework and Stability Guard+ track CatCore window sessions. If the workspace host disappears while saved sessions remain, Guard+ uses a grace period and can request workspace recovery.

## Manage later

Open **Task Manager → Manage → Manage Activity** to return an app to the CatCore Workspace for deeper activity/window management.

## If an app does not render correctly

Some apps restrict secondary displays, secure surfaces, input routing or their own activity lifecycle. Try **Start as app's own task** for those apps rather than forcing the window path.
