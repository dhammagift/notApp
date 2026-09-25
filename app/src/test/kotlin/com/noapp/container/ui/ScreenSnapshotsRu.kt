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
 * Russian, because that is where the long strings live: the picker's descriptions, the demo's
 * captions and Settings' two-line rows are all longest here, and the layout is the same width it
 * would be in English. A clipped sentence in English is a clipped sentence everywhere.
 *
 * `RENDER_SNAPSHOTS=1 ./gradlew :app:testDebugUnitTest --tests "*ScreenSnapshotsRu*"`
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-rRU-w411dp-h891dp-xhdpi")
class ScreenSnapshotsRu {

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

    @Test
    fun modePicker() {
        compose.setContent {
            NoAppTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    ConfigScreenPreview(openPickerOnStart = true)
                }
            }
        }
        compose.onNode(isDialog()).captureRoboImage("build/snapshots/30-picker-ru.png")
    }

    @Test
    fun demoMix() = shoot("31-demo-mix-ru") { DemoPreview(AppMode.MIX) }

    @Test
    fun itemList() = shoot("32-item-list-ru") { ConfigScreenPreview() }

    /** The rating card too: three buttons, the longest pair of which only exists in Russian. */
    @Test
    fun settings() {
        ReviewStore.installedAt = { System.currentTimeMillis() - 200L * 24 * 60 * 60 * 1000 }
        shoot("33-settings-ru") { SettingsScreenPreview() }
    }

    @Test
    fun demoDark() = shoot("34-demo-mix-dark-ru") {
        NoAppTheme(AppTheme.DARK) { DemoPreview(AppMode.MIX) }
    }
}
