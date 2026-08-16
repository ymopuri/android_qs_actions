# Signing key

`release.jks` signs the APKs published to Releases. It is checked in on purpose,
with the password `qsactions` for the store, the key, and the alias.

This is a deliberate trade, not an oversight.

## What it buys

Anyone — including CI, and including you on a fresh clone — can build a release
APK that is byte-for-byte installable and upgrades cleanly over the published
one. Nothing to configure, no secrets, no setup step between cloning and shipping.

## What it costs

The key is public, so anyone can build an APK that Android accepts as an
**upgrade** to this app. An upgrade inherits already-granted permissions, so if
you have granted `WRITE_SECURE_SETTINGS`, a malicious build signed with this key
would inherit it.

That still requires you to sideload their file. For a personal app installed from
its own releases page, that's the same trust decision you make every time you tap
install. For anything with real users, it would not be.

## Moving to a private key

Nothing in the build needs to change. Generate a keystore, add four repository
secrets, and the environment variables take precedence over the checked-in one:

```
keytool -genkeypair -v -keystore release.jks -alias qsactions \
  -keyalg RSA -keysize 4096 -validity 10000
```

| Secret | Value |
|---|---|
| `KEYSTORE_FILE` | path the workflow decodes the keystore to |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

**Switching keys breaks upgrades.** Anyone on a release signed with the old key
has to uninstall before installing a new one, because Android refuses an upgrade
whose signature changed. Worth doing early if at all.
