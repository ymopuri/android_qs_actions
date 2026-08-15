package com.ymopuri.qsactions.tile

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import com.ymopuri.qsactions.action.ActionState
import com.ymopuri.qsactions.action.QsAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TileLabelStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = TileLabelStore.get(context)
    private val action = FakeAction()

    @Test
    fun `falls back to the action title when unset`() {
        store.setLabel(action, "")

        assertEquals("Shizuku control", store.labelFor(action))
    }

    @Test
    fun `returns the override once set`() {
        store.setLabel(action, "Shizuku")

        assertEquals("Shizuku", store.labelFor(action))
    }

    @Test
    fun `a blank override falls back rather than leaving the tile nameless`() {
        store.setLabel(action, "Shizuku")
        store.setLabel(action, "   ")

        assertEquals("Shizuku control", store.labelFor(action))
    }

    @Test
    fun `overrides are trimmed`() {
        store.setLabel(action, "  Shizuku  ")

        assertEquals("Shizuku", store.labelFor(action))
    }

    private class FakeAction : QsAction {
        override val id: String = "fake"
        override val title: String = "Shizuku control"
        override val summary: String = ""
        override val tileComponent: ComponentName = ComponentName("pkg", "cls")
        override val tileIconRes: Int = 0
        override fun state(): Flow<ActionState> = flowOf(ActionState.Off)
        override fun isConfigured(): Boolean = true
        override suspend fun toggle(): Result<Unit> = Result.success(Unit)
    }
}
