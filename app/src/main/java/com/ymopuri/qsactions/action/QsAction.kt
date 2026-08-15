package com.ymopuri.qsactions.action

import android.content.ComponentName
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/** What a tile shows and what the card reflects. */
sealed interface ActionState {
    data object On : ActionState
    data object Off : ActionState

    /** A toggle is in flight. Tiles render this as unavailable so it can't be re-tapped. */
    data object Working : ActionState

    /** The action can't run — not set up, a dependency is missing, etc. */
    data class Unavailable(val reason: String) : ActionState
}

/**
 * One toggleable thing surfaced as a Quick Settings tile and a card on the landing page.
 *
 * Adding an action means: implement this, add it to [ActionRegistry], and declare one
 * `<service>` in the manifest pointing at a [com.ymopuri.qsactions.tile.BaseActionTileService]
 * subclass. Nothing else in the app needs to change.
 */
interface QsAction {

    /** Stable across releases — it's how a tile service finds its action. */
    val id: String

    val title: String

    val summary: String

    /** The manifest-declared tile for this action. */
    val tileComponent: ComponentName

    @get:DrawableRes
    val tileIconRes: Int

    /** Confirmation shown after a successful toggle. Overridden for better wording. */
    val onMessage: String get() = "$title on"

    val offMessage: String get() = "$title off"

    fun state(): Flow<ActionState>

    /**
     * Whether the action has everything it needs to run. Unconfigured actions keep
     * their tile component disabled and show a "Set up" card instead of a switch.
     */
    fun isConfigured(): Boolean

    /**
     * Flip the action. Should return as soon as the outcome is known; the tile
     * shows [ActionState.Working] until then.
     */
    suspend fun toggle(): Result<Unit>

    /** Whether tapping the card opens a settings screen. */
    val hasConfig: Boolean get() = false

    /** Rendered full-screen when the card is tapped. Only called if [hasConfig]. */
    @Composable
    fun ConfigContent(onNavigateUp: () -> Unit) {
    }
}
