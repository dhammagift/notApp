package com.noapp.container.data

import android.content.Context

/**
 * The last slot target THIS app launched, and when — the one piece of state that lets the MIX list
 * hide an item that is, as far as we can tell, already open behind it.
 *
 * WHY NOT ASK ANDROID: knowing the real foreground app needs Usage Access, which the app only asks
 * for when the user turns on "recent apps in the list"; a list that changed shape depending on an
 * unrelated permission would be worse than a list that is occasionally one item too short. So the
 * proxy is our own launch: MIX's plain tap launches slot 0 and immediately opens the sheet (exact,
 * no guessing), and the Quick Settings tile launches its assigned slot (the user is then in that
 * app, so its item is worth hiding when the list comes up over it).
 *
 * The window exists because nothing tells us the user has since switched away: inside it we assume
 * the launched app is still the one on screen, after it we show everything again. Deliberately
 * short — a stale hidden item is more annoying than a redundant visible one.
 *
 * Runtime state, not configuration: it is never exported, never imported, and losing it only means
 * the list shows one extra row.
 */
object LastLaunch {

    private const val PREFS_NAME = "noapp_runtime"
    private const val KEY_TARGET = "last_launch_target"
    private const val KEY_AT = "last_launch_at"

    /** How long a launch is assumed to still be what the user is looking at. */
    const val FRESH_MS = 3 * 60 * 1000L

    fun remember(context: Context, targetKey: String?) {
        if (targetKey.isNullOrBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TARGET, targetKey)
            .putLong(KEY_AT, System.currentTimeMillis())
            .apply()
    }

    /** The target launched recently enough to be assumed still on screen, or null. */
    fun freshTarget(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val target = prefs.getString(KEY_TARGET, null) ?: return null
        val at = prefs.getLong(KEY_AT, 0L)
        return if (isFresh(at, System.currentTimeMillis())) target else null
    }

    /**
     * Split out from [freshTarget] so the window rule itself is testable without a device: the
     * SharedPreferences half needs Android, this half does not.
     */
    fun isFresh(launchedAt: Long, now: Long): Boolean =
        launchedAt > 0L && now - launchedAt in 0..FRESH_MS
}
