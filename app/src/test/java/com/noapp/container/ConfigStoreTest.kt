package com.noapp.container

import com.noapp.container.data.ConfigStore
import com.noapp.container.model.AppConfig
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigStoreTest {

    @Test
    fun `round trip preserves configured and empty slots, and mode`() {
        val config = AppConfig(
            mode = AppMode.DIRECT,
            slots = listOf(
                ShortcutSlot(id = 0, type = SlotType.APP, label = "Camera", color = "#1A73E8", param = "com.android.camera2"),
                ShortcutSlot(id = 1, type = SlotType.URL, label = "Wiki", color = "#188038", param = "https://wikipedia.org/wiki/{{word}}"),
                ShortcutSlot(id = 2, type = SlotType.INTENT, label = "Dial", color = "#D93025", param = "intent://call#Intent;end"),
                ShortcutSlot(id = 3, type = SlotType.URL, label = "Rocket", color = "#F9AB00", param = "https://example.com", customIcon = "🚀"),
                ShortcutSlot(id = 4) // unconfigured
            )
        )

        val restored = ConfigStore.fromJson(ConfigStore.toJson(config))

        assertEquals(config, restored)
    }

    @Test
    fun `slot list is not capped at 5`() {
        val config = AppConfig(
            mode = AppMode.LIST,
            slots = (0 until 12).map { i -> ShortcutSlot(id = i, type = SlotType.URL, label = "App $i", param = "https://example.com/$i") }
        )

        val restored = ConfigStore.fromJson(ConfigStore.toJson(config))

        assertEquals(12, restored.slots.size)
        assertEquals(config, restored)
    }

    @Test
    fun `malformed json falls back to default config`() {
        val restored = ConfigStore.fromJson("not json")
        assertEquals(AppConfig(), restored)
    }

    @Test
    fun `the shade tile's assigned slot survives a round trip`() {
        // The assignment is what a row's rocket marker shows and what NotAppTileService reads back,
        // and it names a slot by its target rather than its position — so it has to survive both the
        // JSON and the reordering the list invites.
        val config = AppConfig(
            slots = listOf(
                ShortcutSlot(id = 0, type = SlotType.URL, label = "Wiki", param = "https://wikipedia.org"),
                ShortcutSlot(id = 1, type = SlotType.APP, label = "Camera", param = "com.android.camera2")
            ),
            tileSlot = "APP:com.android.camera2"
        )

        val restored = ConfigStore.fromJson(ConfigStore.toJson(config))

        assertEquals("APP:com.android.camera2", restored.tileSlot)
        assertEquals(restored.tileSlot, restored.slots[1].targetKey)
        assertEquals(config, restored)
    }

    @Test
    fun `an unconfigured slot has no target to point the tile at`() {
        assertEquals(null, ShortcutSlot(id = 0).targetKey)
    }
}
