package com.ymopuri.qsactions.shizuku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ymopuri.qsactions.R
import com.ymopuri.qsactions.system.SecureSettings
import com.ymopuri.qsactions.ui.AppCard
import com.ymopuri.qsactions.ui.AppCardTokens
import com.ymopuri.qsactions.ui.SectionTitle
import com.ymopuri.qsactions.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShizukuConfigScreen(
    action: ShizukuToggleAction,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember(action) { action.prefs() }
    val revision by prefs.revision.collectAsStateWithLifecycle()

    var actionPrefix by rememberSaveable { mutableStateOf(prefs.actionPrefix) }
    var packageName by rememberSaveable { mutableStateOf(prefs.shizukuPackage) }
    var authToken by rememberSaveable { mutableStateOf(prefs.authToken) }

    val lockdown = remember(revision) { prefs.lockdown }
    val canWriteSettings = remember(revision) { SecureSettings.canWrite(context) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(action.title) },
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
            SectionTitle("Connection")

            AppCard {
                Text(
                    "Open Shizuku, tap the Automation card on its home screen, and copy the " +
                        "three values it lists into the matching fields below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "On a Stealth-mode install these genuinely differ: the action keeps the " +
                        "original package name while the package field gets the random " +
                        "suffix. That's not a typo — copy both exactly as shown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = actionPrefix,
                    onValueChange = { actionPrefix = it },
                    label = { Text("Action") },
                    supportingText = { Text("Without the trailing .START / .STOP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package") },
                    supportingText = { Text("Has a random suffix under Stealth mode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("Auth token (Extras)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        action.saveConfig(actionPrefix, packageName, authToken)
                        showToast(context, "Saved")
                    },
                    enabled = actionPrefix.isNotBlank() &&
                        packageName.isNotBlank() &&
                        authToken.isNotBlank(),
                ) {
                    Text("Save")
                }
            }

            SectionTitle("Test")

            AppCard {
                Text(
                    "Sends the broadcast directly. Shizuku should react, or post an " +
                        "\"authentication invalid\" notification if the token is wrong. No " +
                        "reaction at all means the Action or Package is wrong — the " +
                        "broadcast reached no receiver.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = prefs.isConfigured,
                        onClick = {
                            ShizukuControl.send(
                                context = context,
                                actionPrefix = prefs.actionPrefix,
                                targetPackage = prefs.shizukuPackage,
                                action = ShizukuControl.START,
                                authToken = prefs.authToken,
                            )
                            showToast(context, "Sent ${prefs.actionPrefix}.START")
                        },
                    ) { Text("Send START") }

                    OutlinedButton(
                        enabled = prefs.isConfigured,
                        onClick = {
                            ShizukuControl.send(
                                context = context,
                                actionPrefix = prefs.actionPrefix,
                                targetPackage = prefs.shizukuPackage,
                                action = ShizukuControl.STOP,
                                authToken = prefs.authToken,
                            )
                            showToast(context, "Sent ${prefs.actionPrefix}.STOP")
                        },
                    ) { Text("Send STOP") }
                }
            }

            SectionTitle("Lockdown")

            AppCard {
                Text(
                    "Also turn off Developer options and USB debugging when Shizuku stops, " +
                        "and turn Developer options back on before it starts. Most banking " +
                        "apps check these rather than Shizuku itself.",
                    style = MaterialTheme.typography.bodyMedium,
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
                    // The grant itself lives in the app's Settings screen, since the
                    // permission is app-wide rather than a Shizuku concern.
                    Text(
                        "Needs the write secure settings permission — grant it in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
