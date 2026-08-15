package com.ymopuri.qsactions.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ymopuri.qsactions.R
import com.ymopuri.qsactions.prefs.ThemeMode
import com.ymopuri.qsactions.prefs.ThemePrefs
import com.ymopuri.qsactions.shizuku.ShizukuShell
import com.ymopuri.qsactions.system.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePrefs = remember { ThemePrefs.get(context) }
    val themeMode by themePrefs.mode.collectAsStateWithLifecycle()

    // Permission state can only change out-of-band (via the grant below or adb),
    // so it's re-read on demand rather than observed.
    var permissionRevision by remember { mutableIntStateOf(0) }
    val canWriteSettings = remember(permissionRevision) { SecureSettings.canWrite(context) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(AppCardTokens.Spacing),
        ) {
            SectionTitle("Appearance")

            AppCard {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { themePrefs.setMode(mode) },
                            )
                            Text(
                                text = themeLabel(mode),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            SectionTitle("Permissions")

            AppCard {
                Text("Write secure settings", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (canWriteSettings) {
                        "Granted. Actions can change Developer options and USB debugging."
                    } else {
                        "Not granted. Actions that need to change Developer options or USB " +
                            "debugging — such as Shizuku control's lockdown — stay disabled."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!canWriteSettings) {
                    Text(
                        text = SecureSettings.grantCommand(context.packageName),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    // Shizuku is the only shell this app has, so the
                                    // one-tap grant borrows it even though the
                                    // permission itself is app-wide.
                                    ShizukuShell.grantWriteSecureSettings(context)
                                        .onSuccess {
                                            permissionRevision++
                                            showToast(
                                                context,
                                                if (SecureSettings.canWrite(context)) {
                                                    "Granted"
                                                } else {
                                                    "Ran, but not granted: $it"
                                                },
                                                long = true,
                                            )
                                        }
                                        .onFailure {
                                            showToast(
                                                context,
                                                it.message ?: "Grant failed",
                                                long = true,
                                            )
                                        }
                                }
                            },
                        ) { Text("Grant via Shizuku") }

                        OutlinedButton(
                            onClick = {
                                context.getSystemService(ClipboardManager::class.java)
                                    ?.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "pm grant",
                                            SecureSettings.grantCommand(context.packageName),
                                        )
                                    )
                                showToast(context, "Command copied")
                            },
                        ) { Text("Copy command") }
                    }
                }
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> "Follow system"
    ThemeMode.Light -> "Light"
    ThemeMode.Dark -> "Dark"
}
