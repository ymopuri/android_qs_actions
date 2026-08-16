# Minification is newly enabled for release builds. The Shizuku integration is the
# part most likely to break under R8, because it crosses a process boundary and is
# reached reflectively rather than by a normal call graph — so it is kept wholesale
# rather than trimmed. The saving from shrinking these few classes is negligible
# next to the cost of the core feature failing silently in a signed build.

# Shizuku's server spawns a separate process that loads this class by name from our
# APK. Nothing in the app references it directly.
-keep class com.ymopuri.qsactions.shizuku.ShellService { *; }

# The AIDL interface and its generated Stub/Proxy are the wire contract between this
# app and that spawned process.
-keep interface com.ymopuri.qsactions.shizuku.IShellService { *; }
-keep class com.ymopuri.qsactions.shizuku.IShellService$* { *; }

# The Shizuku API talks to the server over binder and resolves parts of it by name.
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.**
-dontwarn moe.shizuku.**
