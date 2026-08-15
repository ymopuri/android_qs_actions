package com.ymopuri.qsactions.ui

import android.app.StatusBarManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ymopuri.qsactions.action.ActionState
import com.ymopuri.qsactions.action.QsAction
import com.ymopuri.qsactions.tile.TileEnabler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ActionCard(
    action: QsAction,
    onConfigure: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateFlow = remember(action) { action.state() }
    val state by stateFlow.collectAsStateWithLifecycle(
        initialValue = ActionState.Unavailable("…")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(action.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = statusLabel(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = state is ActionState.On,
                    enabled = state is ActionState.On || state is ActionState.Off,
                    onCheckedChange = {
                        scope.launch(Dispatchers.Default) {
                            action.toggle().onFailure { error ->
                                onMessage(error.message ?: "Toggle failed")
                            }
                        }
                    },
                )
            }

            Text(
                text = action.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (action.hasConfig) {
                    TextButton(onClick = onConfigure) {
                        Text(if (action.isConfigured()) "Settings" else "Set up")
                    }
                }
                TextButton(
                    enabled = action.isConfigured(),
                    onClick = {
                        TileEnabler.setTileEnabled(context, action.tileComponent, true)
                        TileEnabler.requestAddTile(context, action) { result ->
                            onMessage(addTileMessage(result))
                        }
                    },
                ) {
                    Text("Add tile")
                }
            }
        }
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
    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Tile is already in Quick Settings"
    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> "Not added"
    else -> "Couldn't add the tile (code $result)"
}
