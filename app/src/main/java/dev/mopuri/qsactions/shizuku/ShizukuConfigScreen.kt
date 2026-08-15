package dev.mopuri.qsactions.shizuku

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShizukuConfigScreen(
    action: ShizukuToggleAction,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(action) { action.prefs() }
    val snackbarHostState = remember { SnackbarHostState() }

    val revision by prefs.revision.collectAsStateWithLifecycle()

    var packageName by rememberSaveable { mutableStateOf(prefs.shizukuPackage) }
    var authToken by rememberSaveable { mutableStateOf(prefs.authToken) }

    val lockdown = remember(revision) { prefs.lockdown }
    val canWriteSettings = remember(revision) { DebugFlags.canWrite(context) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Shizuku") },
                navigationIcon = { TextButton(onClick = onNavigateUp) { Text("Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Open Shizuku, find the Automation card on its home screen, and copy the " +
                    "package name and auth token it lists here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                label = { Text("Shizuku package") },
                supportingText = {
                    Text("With Stealth mode on this has a random suffix, e.g. …api.p1k65")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = authToken,
                onValueChange = { authToken = it },
                label = { Text("Auth token") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    action.saveConfig(packageName, authToken)
                    notify("Saved")
                },
                enabled = packageName.isNotBlank() && authToken.isNotBlank(),
            ) {
                Text("Save")
            }

            HorizontalDivider()

            Text("Test", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sends the broadcast directly. A wrong token makes Shizuku post an " +
                    "\"authentication invalid\" notification — a quick way to check.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = prefs.isConfigured,
                    onClick = {
                        ShizukuControl.send(
                            context, prefs.shizukuPackage, ShizukuControl.START, prefs.authToken
                        )
                        notify("Sent START")
                    },
                ) { Text("Send START") }

                OutlinedButton(
                    enabled = prefs.isConfigured,
                    onClick = {
                        ShizukuControl.send(
                            context, prefs.shizukuPackage, ShizukuControl.STOP, prefs.authToken
                        )
                        notify("Sent STOP")
                    },
                ) { Text("Send STOP") }
            }

            HorizontalDivider()

            Text("Lockdown", style = MaterialTheme.typography.titleMedium)
            Text(
                "Also turn off Developer options and USB debugging when Shizuku stops, and " +
                    "turn Developer options back on before it starts. Most banking apps check " +
                    "these rather than Shizuku itself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Clear debugging flags on stop")
                Switch(
                    checked = lockdown,
                    enabled = canWriteSettings,
                    onCheckedChange = { prefs.setLockdown(it) },
                )
            }

            if (!canWriteSettings) {
                Text(
                    "Needs WRITE_SECURE_SETTINGS. Grant it once while Shizuku is running:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = DebugFlags.grantCommand(context.packageName),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.Default) {
                                ShizukuShell.grantWriteSecureSettings(context)
                                    .onSuccess {
                                        notify(
                                            if (DebugFlags.canWrite(context)) {
                                                "Granted — restart the app if the switch stays off"
                                            } else {
                                                "Ran, but the permission still isn't granted: $it"
                                            }
                                        )
                                    }
                                    .onFailure { notify(it.message ?: "Grant failed") }
                            }
                        },
                    ) { Text("Grant via Shizuku") }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "pm grant",
                                    DebugFlags.grantCommand(context.packageName),
                                )
                            )
                            notify("Command copied")
                        },
                    ) { Text("Copy command") }
                }
            }
        }
    }
}
