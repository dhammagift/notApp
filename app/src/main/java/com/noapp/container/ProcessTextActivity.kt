package com.noapp.container

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Text-selection menu entry (PROCESS_TEXT, see AndroidManifest.xml): hands the selection to the
 * list sheet in Not App's own task and finishes. Nothing is returned: the selection is input for
 * an item, not text to replace.
 */
class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            startActivity(
                Intent(this, QuickPickActivity::class.java)
                    .putExtra(EXTRA_SHARED_TEXT, text)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        setResult(RESULT_CANCELED)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
