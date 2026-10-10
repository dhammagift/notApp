package com.noapp.container.shortcuts

import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A home-screen icon has to keep its item through any reorder: see pinnedKeyOf. */
class PinnedKeyTest {
    private val wiki = ShortcutSlot(id = 0, type = SlotType.URL, label = "Wiki", param = "https://wikipedia.org")
    private val camera = ShortcutSlot(id = 1, type = SlotType.APP, label = "Camera", param = "com.android.camera2")

    @Test
    fun `a pin names its item outright`() {
        assertEquals(camera.targetKey, pinnedKeyOf("pin:${camera.targetKey}", null, listOf(wiki)))
    }

    @Test
    fun `an old position-numbered icon is its item at that position until refreshed`() {
        assertEquals(camera.targetKey, pinnedKeyOf("slot_1", null, listOf(wiki, camera)))
        assertNull(pinnedKeyOf("slot_5", null, listOf(wiki, camera)))
    }

    @Test
    fun `once refreshed it keeps its item after a reorder`() {
        val reordered = listOf(camera.copy(id = 0), wiki.copy(id = 1))
        assertEquals(camera.targetKey, pinnedKeyOf("slot_1", camera.targetKey, reordered))
    }
}
