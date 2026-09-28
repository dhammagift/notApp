package com.noapp.container.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.noapp.container.model.AppConfig
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType

/**
 * The example, configured for the snapshot tests: real composables with a fixed, representative
 * config, so the rendered pictures are comparable between runs.
 */
@Composable
internal fun DemoPreview(
    mode: AppMode,
    slots: List<ShortcutSlot> = snapshotSlots,
    showRecentApps: Boolean = false,
    showPeekBubble: Boolean = true,
    useAllSlots: Boolean = false
) {
    ModeDemo(
        mode = mode,
        slots = slots,
        showRecentApps = showRecentApps,
        showPeekBubble = showPeekBubble,
        useAllSlotsInDirectMode = useAllSlots,
        peekBubbleSize = 1f,
        peekBubbleAlpha = 1f,
        peekBubbleDockPeek = 0.625f,
        peekBubbleReturns = true,
        onOpenSetting = {},
        // Same decision ConfigScreen makes: a landscape screen keeps the sheet phone-wide.
        narrowSheet = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp },
        modifier = Modifier.fillMaxSize()
    )
}

/** The whole item list, with the demo button and the add button, as it looks on opening. */
@Composable
internal fun ConfigScreenPreview(openPickerOnStart: Boolean = false, mode: AppMode = AppMode.LIST) {
    ConfigScreen(
        mode = mode,
        slots = snapshotSlots,
        showPeekBubble = true,
        showRecentApps = false,
        useAllSlotsInDirectMode = false,
        peekBubbleSize = 1f,
        peekBubbleAlpha = 1f,
        peekBubbleDockPeek = 0.625f,
        peekBubbleReturns = true,
        openPickerOnStart = openPickerOnStart,
        onPickerOpened = {},
        hint = null,
        onHintShown = {},
        onEditSlot = {},
        onAddSlot = {},
        onOpenSettings = {},
        onOpenSettingsFromDemo = {},
        onModeChanged = {},
        onSlotsChanged = {},
        tileSlot = AppConfig.TILE_NONE,
        onTileSlotChanged = {}
    )
}

/** Settings, optionally pointing at one row the way the demo does on its way here. */
@Composable
internal fun SettingsScreenPreview(spotlight: SettingsSpot? = null) {
    SettingsScreen(
        config = AppConfig(mode = AppMode.LIST, slots = snapshotSlots),
        spotlight = spotlight,
        onSpotlightShown = {},
        hint = null,
        onHintShown = {},
        onImportConfig = {},
        onUseAllSlotsInDirectModeChanged = {},
        onIconVariantChanged = {},
        onShowPeekBubbleChanged = {},
        onPeekBubbleReturnsChanged = {},
        onPeekBubbleSizeChanged = {},
        onPeekBubbleAlphaChanged = {},
        onPeekBubbleDockPeekChanged = {},
        onShowRecentAppsChanged = {},
        onThemeChanged = {},
        onBack = {}
    )
}

internal val snapshotSlots = listOf(
    ShortcutSlot(id = 0, type = SlotType.APP, label = "Calculator", param = "com.android.calculator2"),
    ShortcutSlot(id = 1, type = SlotType.APP, label = "Acode", param = "com.fox2code.mmm"),
    ShortcutSlot(id = 2, type = SlotType.APP, label = "Agoda", param = "com.agoda.mobile"),
    ShortcutSlot(id = 3, type = SlotType.APP, label = "AR Doodle", param = "com.samsung.android.ardoodle"),
    ShortcutSlot(id = 4, type = SlotType.APP, label = "Aurora Store", param = "com.aurora.store")
)
