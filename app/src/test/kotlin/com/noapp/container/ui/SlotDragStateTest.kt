package com.noapp.container.ui

import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SlotDragStateTest {
    private fun app(id: Int, pkg: String) = ShortcutSlot(id = id, type = SlotType.APP, label = pkg, param = pkg)

    private val start = listOf(app(0, "a"), ShortcutSlot(1), app(2, "b"), ShortcutSlot(3), ShortcutSlot(4))

    @Test
    fun reorderKeepsEveryKey() {
        val state = SlotDragState(start)
        val before = state.items.map { it.stableKey }.toSet()
        // What a drop commits: same rows, new order, ids renumbered.
        val moved = listOf(start[2], start[0], start[1], start[3], start[4]).mapIndexed { i, s -> s.copy(id = i) }
        state.resync(moved)
        assertEquals(before, state.items.map { it.stableKey }.toSet())
    }

    @Test
    fun removingAnEmptyRowGivesEmptiesFreshKeys() {
        val state = SlotDragState(start)
        val oldEmptyKeys = state.items.filter { !it.slot.isConfigured }.map { it.stableKey }.toSet()
        state.resync(start.filterIndexed { i, _ -> i != 1 }.mapIndexed { i, s -> s.copy(id = i) })
        state.items.filter { !it.slot.isConfigured }.forEach { assertNotEquals(true, it.stableKey in oldEmptyKeys) }
    }
}
