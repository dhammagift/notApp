package com.noapp.container.shortcuts

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * launchToken()/isOwnLaunch() is what stops another app from firing a user's slots through the
 * still-exported MainActivity (see MainActivity.dispatchIfShortcut) — the fix in d7bcb8c had no
 * test of its own, so a later rename or reordering could silently reopen that hole.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShortcutSyncTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("no_app_launch", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `token is stable across calls`() {
        assertEquals(ShortcutSync.launchToken(context), ShortcutSync.launchToken(context))
    }

    @Test
    fun `an intent stamped with withLaunchToken is its own launch`() {
        val intent = Intent().withLaunchToken(context)
        assertTrue(ShortcutSync.isOwnLaunch(context, intent))
    }

    @Test
    fun `a bare intent from another app is not its own launch`() {
        assertFalse(ShortcutSync.isOwnLaunch(context, Intent()))
    }

    @Test
    fun `a stale token from before a reinstall is not accepted`() {
        val intent = Intent().withLaunchToken(context)
        context.getSharedPreferences("no_app_launch", Context.MODE_PRIVATE).edit().clear().commit()
        assertFalse(ShortcutSync.isOwnLaunch(context, intent))
    }
}
