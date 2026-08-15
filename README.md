# QS Actions

A small Android app that puts toggleable actions in Quick Settings. The first — and
currently only — action starts and stops Shizuku.

## Why

Shizuku has to keep running to give other apps ADB-level privileges, but leaving it
up (along with Developer options and USB debugging) trips the tamper checks in many
banking and payment apps. Stopping it normally means opening the Shizuku app, and
starting it again means the whole wireless-debugging dance. This makes it one tap.

## Requirements

The Shizuku action needs [thedjchi's Shizuku fork](https://github.com/thedjchi/Shizuku),
not upstream Shizuku. The fork adds two exported broadcast receivers
(`ManualStartReceiver` / `ManualStopReceiver`) that accept an authenticated
`<package>.START` / `<package>.STOP` intent; upstream has no equivalent, and its
`Shizuku.exit()` is restricted to the manager app.

Android 13+ (`minSdk 33`).

## Setup

1. In Shizuku, tap the **Automation** card on the home screen. It lists an Action, a
   Package, and an auth token (Extras).
2. In QS Actions, open the Shizuku card → **Set up**, copy all three across, and save.
3. Tap **Add tile** to drop the tile into Quick Settings.

### Action and Package are not the same value

With the fork's **Stealth mode** on, the two diverge, and this looks like a typo but
isn't:

| Field | Example |
|-------|---------|
| Action | `moe.shizuku.privileged.api` (no suffix) |
| Package | `moe.shizuku.privileged.api.p1k65` (random suffix) |

Stealth mode's `changePackageName()` rewrites the manifest package, but the fork's
receivers gate on `BuildConfig.APPLICATION_ID` — a constant compiled into the DEX
that the rename doesn't touch. So the action keeps the original name. Copy both
exactly as the Automation card shows them.

Getting this wrong fails *silently*: the broadcast matches no receiver, so Shizuku
does nothing and doesn't even post an auth-failure notification. If a test send
produces no reaction at all, the Action or Package is wrong. If it produces an
"authentication invalid" notification, they're right and the token is wrong.

Enable the fork's **TCP mode** if you want restarts to work without Wi-Fi. Without
it, Shizuku waits for a Wi-Fi connection before starting.

### Optional: lockdown

Stopping Shizuku leaves Developer options and USB debugging switched on, and that is
usually what banking apps actually check. The **lockdown** switch also clears
`development_settings_enabled`, `adb_enabled`, and `adb_wifi_enabled` when Shizuku
stops, and restores Developer options before it starts.

Worth knowing: the fork's `AdbStartWorker` re-enables `adb_enabled` and
`adb_wifi_enabled` on its own, but nothing in the fork touches
`development_settings_enabled` — so this app owns that one.

This needs `WRITE_SECURE_SETTINGS`, granted once while Shizuku is running:

```
pm grant dev.mopuri.qsactions android.permission.WRITE_SECURE_SETTINGS
```

The config screen has a button that runs this through Shizuku, and a copy button if
you'd rather run it from a terminal or a PC. The app works fine without it — you just
don't get the lockdown.

**Try plain stop against your bank first.** If stopping Shizuku alone is enough, you
don't need any of this.

## Adding another action

1. Implement `QsAction` (`action/QsAction.kt`).
2. Add it to `ActionRegistry`.
3. Add a `BaseActionTileService` subclass and one `<service>` entry in the manifest.

Step 3 is unavoidable: `TileService`s are manifest components and can't be created at
runtime. Tiles ship `android:enabled="false"` and are switched on once their action
reports `isConfigured()`, so the Quick Settings editor only lists tiles that work.

## Build

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Credits

The Shizuku integration is built against [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
(Apache-2.0) and [thedjchi's fork](https://github.com/thedjchi/Shizuku). No code from
either is vendored here.
