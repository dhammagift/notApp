package com.noapp.container.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.noapp.container.data.ReviewStore
import com.noapp.container.model.AppMode
import com.noapp.container.model.AppTheme
import com.noapp.container.ui.theme.NoAppTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The rest of the app's surfaces, rendered the same way: the mode picker itself, the rating card in
 * both places it appears, the item list with its demo button, Settings including a spotlighted row,
 * the long-press menu, and the example in dark. Same command as the others:
 * `RENDER_SNAPSHOTS=1 ./gradlew :app:testDebugUnitTest --tests "*ScreenSnapshots*"`
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenSnapshots {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhereAsked() {
        assumeTrue(System.getenv("RENDER_SNAPSHOTS") == "1")
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("no_app_review", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("build/snapshots/$name.png")
    }

    /**
     * The picker: the three modes, their one-line answers to "which one is this", and the example
     * underneath. It lives in its own window, so it is captured through its own root — photographing
     * the screen behind it would show the item list and prove nothing.
     */
    @Test
    fun modePicker() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    ConfigScreenPreview(openPickerOnStart = true)
                }
            }
        }
        compose.onNode(isDialog()).captureRoboImage("build/snapshots/26-mode-picker.png")
    }

    /** The card as it appears at the foot of the list, which is where most people will meet it. */
    @Test
    fun ratingCardInList() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ReviewStore.installedAt = { System.currentTimeMillis() - 200L * 24 * 60 * 60 * 1000 }
        val due = ReviewStore.cardDue(context)
        check(due) { "the card should be due in this snapshot" }
        shoot("20-rating-card-in-list") {
            QuickPickSheet(
                slots = snapshotSlots,
                sharedText = null,
                allowPeek = true,
                showRecentApps = false,
                onConfigure = {},
                onDismiss = {}
            )
        }
    }

    /** And as the first thing in Settings, which is the only Not App surface Direct mode shows. */
    @Test
    fun ratingCardInSettings() = shoot("21-rating-card-in-settings") { SettingsScreenPreview() }

    @Test
    fun itemList() = shoot("22-item-list") { ConfigScreenPreview() }

    @Test
    fun shortcutMenu() = shoot("23-shortcut-menu") {
        ShortcutMenuOverlay(
            appName = "Not App",
            mode = AppMode.DIRECT,
            slots = snapshotSlots,
            useAllSlotsInDirectMode = false,
            onOpenSettings = {},
            onDismiss = {}
        )
    }

    @Test
    fun demoDark() = shoot("24-demo-dark") {
        NoAppTheme(AppTheme.DARK) { DemoPreview(AppMode.MIX) }
    }

    /**
     * The row the demo sends the user to, flashing. The clock is driven by hand: the blink is a
     * finite animation, and left to itself the render waits it out and photographs an untinted row —
     * which is exactly the "highlighted nothing at all" bug the tint exists to fix.
     */
    @Test
    fun settingsSpotlight() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    SettingsScreenPreview(spotlight = SettingsSpot.FLOATING_BUTTON)
                }
            }
        }
        // Past the 120 ms layout wait and the scroll, and inside the first "on" window.
        compose.mainClock.advanceTimeBy(700)
        compose.onRoot().captureRoboImage("build/snapshots/25-settings-spotlight.png")
    }
}

/**
 * The same screens where the space runs out or grows: a phone on its side, and a tablet. Both are
 * places where a fixed width or a fixed height stops working, and neither is reachable from the
 * emulator this machine cannot run.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-land-xhdpi")
class ScreenSnapshotsLandscape {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhereAsked() = assumeTrue(System.getenv("RENDER_SNAPSHOTS") == "1")

    @Test
    fun modePicker() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    ConfigScreenPreview(openPickerOnStart = true)
                }
            }
        }
        compose.onNode(isDialog()).captureRoboImage("build/snapshots/40-picker-landscape.png")
    }

    @Test
    fun itemList() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    ConfigScreenPreview()
                }
            }
        }
        compose.onRoot().captureRoboImage("build/snapshots/41-item-list-landscape.png")
    }
}

/** A tablet, in portrait: the sheet stops being phone-wide, and the cards get real room. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h1280dp-xhdpi")
class ScreenSnapshotsTablet {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhereAsked() = assumeTrue(System.getenv("RENDER_SNAPSHOTS") == "1")

    @Test
    fun modePicker() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    ConfigScreenPreview(openPickerOnStart = true)
                }
            }
        }
        compose.onNode(isDialog()).captureRoboImage("build/snapshots/42-picker-tablet.png")
    }

    @Test
    fun demoMix() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    DemoPreview(AppMode.MIX)
                }
            }
        }
        compose.onRoot().captureRoboImage("build/snapshots/43-demo-mix-tablet.png")
    }
}
