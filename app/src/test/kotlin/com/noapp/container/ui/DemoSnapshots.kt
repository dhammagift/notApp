package com.noapp.container.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import com.noapp.container.ui.theme.NoAppTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pictures of the mode picker's example, rendered on the JVM by Roborazzi (Robolectric + Layoutlib).
 * No device and no emulator: the layouts themselves are what gets drawn, so overlaps, clipping and
 * squeezed-out captions show up here instead of on a phone.
 *
 * Local only, on purpose — CI has no business downloading Layoutlib for this:
 * `RENDER_SNAPSHOTS=1 ./gradlew :app:testDebugUnitTest --tests "*DemoSnapshots*"`
 * PNGs land in `app/build/snapshots/`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class DemoSnapshots {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhereAsked() {
        assumeTrue(System.getenv("RENDER_SNAPSHOTS") == "1")
    }

    private val items = listOf(
        ShortcutSlot(id = 0, type = SlotType.APP, label = "Calculator", param = "com.android.calculator2"),
        ShortcutSlot(id = 1, type = SlotType.APP, label = "Acode", param = "com.fox2code.mmm"),
        ShortcutSlot(id = 2, type = SlotType.APP, label = "Agoda", param = "com.agoda.mobile"),
        ShortcutSlot(id = 3, type = SlotType.APP, label = "AR Doodle", param = "com.samsung.android.ardoodle"),
        ShortcutSlot(id = 4, type = SlotType.APP, label = "Aurora Store", param = "com.aurora.store")
    )

    @Composable
    private fun Panel(
        mode: AppMode,
        slots: List<ShortcutSlot> = items,
        showRecentApps: Boolean = false,
        showPeekBubble: Boolean = true,
        useAllSlots: Boolean = false
    ) {
        NoAppTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
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
                    narrowSheet = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent { content() }
        compose.onRoot().captureRoboImage("build/snapshots/$name.png")
    }

    @Test fun list() = shoot("01-list") { Panel(AppMode.LIST) }

    @Test fun listRecentsOffers() = shoot("02-list-offers") { Panel(AppMode.LIST) }

    @Test fun listRecentsOn() = shoot("03-list-recents-on") { Panel(AppMode.LIST, showRecentApps = true) }

    @Test fun direct() = shoot("04-direct") { Panel(AppMode.DIRECT) }

    @Test fun directAllSlotsOn() = shoot("05-direct-all-slots") { Panel(AppMode.DIRECT, useAllSlots = true) }

    @Test fun mix() = shoot("06-mix") { Panel(AppMode.MIX) }

    @Test fun mixEmptyConfig() = shoot("07-mix-empty") { Panel(AppMode.MIX, slots = emptyList()) }
}

/**
 * The same example on a landscape screen, in the dark, and in English: the three cases that were
 * reported broken on a phone and cannot be judged from a portrait light-theme render.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-land-xhdpi")
class DemoSnapshotsWide {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhereAsked() = assumeTrue(System.getenv("RENDER_SNAPSHOTS") == "1")

    @Test
    fun listLandscape() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    DemoPreview(AppMode.LIST)
                }
            }
        }
        compose.onRoot().captureRoboImage("build/snapshots/10-list-landscape.png")
    }

    @Test
    fun mixLandscape() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    DemoPreview(AppMode.MIX)
                }
            }
        }
        compose.onRoot().captureRoboImage("build/snapshots/11-mix-landscape.png")
    }
}
