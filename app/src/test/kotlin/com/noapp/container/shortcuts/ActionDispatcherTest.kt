package com.noapp.container.shortcuts

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** An intent item can come from someone else's config file: it gets none of Not App's own rights. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ActionDispatcherTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun intentItem(uri: String) = ShortcutSlot(id = 0, type = SlotType.INTENT, label = "x", param = uri)

    @Test
    fun `the selector is dropped and grant flags stay off`() {
        val intent = ActionDispatcher.intentFor(
            context,
            intentItem("intent:#Intent;action=android.intent.action.VIEW;launchFlags=0x43;SEL;package=com.example;end")
        )!!
        assertNull(intent.selector)
        assertEquals(0, intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
    }

    @Test
    fun `Not App's own screens are not an item`() {
        assertNull(ActionDispatcher.intentFor(context, intentItem("intent:#Intent;component=${context.packageName}/com.noapp.container.QuickPickActivity;end")))
    }

    @Test
    fun `a plain tap drops the share placeholder`() {
        val slot = ShortcutSlot(id = 0, type = SlotType.URL, label = "WhatsApp", param = "https://wa.me/{{word}}")
        assertEquals("https://wa.me/", ActionDispatcher.intentFor(context, slot)!!.dataString)
    }
}
