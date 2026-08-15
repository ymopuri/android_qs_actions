# Shizuku's user-service process loads this class by name from the APK, so it
# must survive shrinking even though nothing in the app references it directly.
-keep class dev.mopuri.qsactions.shizuku.ShellService { *; }
