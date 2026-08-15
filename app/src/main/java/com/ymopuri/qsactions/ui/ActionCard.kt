package com.ymopuri.qsactions.ui

import android.app.StatusBarManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ymopuri.qsactions.action.ActionState
import com.ymopuri.qsactions.action.QsAction
import com.ymopuri.qsactions.tile.TileEnabler
import com.ymopuri.qsactions.tile.TileLabelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ActionCard(
    action: QsAction,
    onConfigure: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labels = remember { TileLabelStore.get(context) }
    val labelRevision by labels.revision.collectAsStateWithLifecycle()

    val stateFlow = remember(action) { action.state() }
    val state by stateFlow.collectAsStateWithLifecycle(
        initialValue = ActionState.Unavailable("…")
    )

    var renaming by remember { mutableStateOf(false) }

    // The card doubles as the hero status card: it carries the action's state in
    // its own colour rather than repeating it in a separate summary card.
    val containerColor = when (state) {
        ActionState.On -> MaterialTheme.colorScheme.primaryContainer
        is ActionState.Unavailable -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when (state) {
        ActionState.On -> MaterialTheme.colorScheme.onPrimaryContainer
        is ActionState.Unavailable -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    // On a tinted card the badge needs to invert, or it disappears into the fill.
    val badgeContainer = when (state) {
        ActionState.On, is ActionState.Unavailable -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    AppCard(containerColor = containerColor, contentColor = contentColor) {
        CardHeader(
            iconRes = action.tileIconRes,
            title = action.title,
            status = statusLabel(state),
            iconContainerColor = badgeContainer,
            iconContentColor = contentColor,
            trailing = {
                Switch(
                    checked = state is ActionState.On,
                    enabled = state is ActionState.On || state is ActionState.Off,
                    onCheckedChange = {
                        val wasOn = state is ActionState.On
                        scope.launch(Dispatchers.Default) {
                            action.toggle()
                                .onSuccess {
                                    showToast(
                                        context,
                                        if (wasOn) action.offMessage else action.onMessage,
                                    )
                                }
                                .onFailure { error ->
                                    showToast(
                                        context,
                                        error.message ?: "Toggle failed",
                                        long = true,
                                    )
                                }
                        }
                    },
                )
            },
        )

        Text(
            text = action.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is ActionState.Off) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                contentColor
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (action.hasConfig) {
                TextButton(onClick = onConfigure) {
                    Text(if (action.isConfigured()) "Settings" else "Set up")
                }
            }
            TextButton(
                enabled = action.isConfigured(),
                onClick = { renaming = true },
            ) {
                Text("Add tile")
            }
        }
    }

    if (renaming) {
        // Named before it's added, so the system's "Add tile?" prompt shows the
        // name the user just chose.
        var draft by remember(labelRevision) { mutableStateOf(labels.labelFor(action)) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Tile name") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Shown in Quick Settings") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    labels.setLabel(action, draft)
                    renaming = false
                    TileEnabler.setTileEnabled(context, action.tileComponent, true)
                    TileEnabler.requestAddTile(context, action) { result ->
                        showToast(context, addTileMessage(result))
                    }
                }) { Text("Add tile") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false }) { Text("Cancel") }
            },
        )
    }
}

private fun statusLabel(state: ActionState): String = when (state) {
    ActionState.On -> "On"
    ActionState.Off -> "Off"
    ActionState.Working -> "Working…"
    is ActionState.Unavailable -> state.reason
}

private fun addTileMessage(result: Int): String = when (result) {
    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Tile added"
    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Already in Quick Settings"
    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> "Not added"
    else -> "Couldn't add the tile (code $result)"
}
