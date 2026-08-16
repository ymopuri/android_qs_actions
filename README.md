# Quick Settings Actions

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
2. In Quick Settings Actions, open the Shizuku control card → **Set up**, copy all
   three across, and save.
3. Tap **Add tile**, name the tile, and confirm the system prompt.

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
pm grant com.ymopuri.qsactions android.permission.WRITE_SECURE_SETTINGS
```

This lives in the app's own **Settings** (gear, top right) rather than on the
Shizuku page, because the permission is app-wide — future actions will want it too.
There's a button that runs the grant through Shizuku, and a copy button if you'd
rather run it from a terminal or a PC. The app works fine without it — you just
don't get the lockdown, and its switch stays disabled.

**Try plain stop against your bank first.** If stopping Shizuku alone is enough, you
don't need any of this.

## Adding another action

1. Implement `QsAction` (`action/QsAction.kt`).
2. Add it to `ActionRegistry`.
3. Add a `BaseActionTileService` subclass and one `<service>` entry in the manifest.

Step 3 is unavoidable: `TileService`s are manifest components and can't be created at
runtime. Tiles ship `android:enabled="false"` and are switched on once their action
reports `isConfigured()`, so the Quick Settings editor only lists tiles that work.

## Install

Grab the APK from the [latest release](https://github.com/ymopuri/android_qs_actions/releases/latest).

It's a **universal** APK — every ABI in one file, so there's nothing to choose
between. (The app has no native code of its own; the only `.so` in there is a
~10 KB Compose helper, which is why per-architecture builds aren't worth the
extra files.)

Releases are R8-minified and release-signed. If you have an older debug build
installed, uninstall it first — the signing key differs and Android will refuse
the upgrade.

## Build

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

`assembleRelease` works locally too, but produces an **unsigned** APK unless the
signing environment variables below are set.

## Cutting a release

Push a `v*` tag and CI does the rest — no setup, no secrets:

```
git tag v0.2.0 && git push origin v0.2.0
```

Keep `versionName` in `app/build.gradle.kts` in step with the tag; the release is
named from the tag, not from the build. CI signs with the checked-in key, verifies
the result with `apksigner` before publishing, and fails rather than shipping an
unsigned APK.

The signing key lives in `signing/` and is intentionally not a secret — see
[signing/README.md](signing/README.md) for what that trade costs and how to move
to a private key without changing the build.

## Credits

The Shizuku integration is built against [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
(Apache-2.0) and [thedjchi's fork](https://github.com/thedjchi/Shizuku). No code from
either is vendored here.
