package com.noapp.container.data

import android.content.Context

/**
 * Which one-off permission explanations have already been shown.
 *
 * The mode picker used to raise the "draw over other apps" explanation every single time Direct was
 * chosen without the permission — the same dialog, over and over, whether the user had just declined
 * it or was simply switching back and forth between modes. An explanation is worth showing once;
 * after that the toggle in Settings is where the permission lives, and it asks on its own when the
 * user turns it on.
 */
object PromptStore {
    private const val PREFS_NAME = "no_app_prompts"
    private const val KEY_GEAR_ASKED = "gear_overlay_explained"
    private const val KEY_PEEK_ASKED = "peek_overlay_explained"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun gearExplained(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GEAR_ASKED, false)

    fun markGearExplained(context: Context) {
        prefs(context).edit().putBoolean(KEY_GEAR_ASKED, true).apply()
    }

    fun peekExplained(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PEEK_ASKED, false)

    fun markPeekExplained(context: Context) {
        prefs(context).edit().putBoolean(KEY_PEEK_ASKED, true).apply()
    }
}
