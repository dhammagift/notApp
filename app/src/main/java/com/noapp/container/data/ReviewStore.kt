package com.noapp.container.data

import android.content.Context
import com.noapp.container.BuildConfig

/**
 * When Not App may quietly ask for a Play rating.
 *
 * Two asks at most, and both are anchored to the install date instead of to a launch counter:
 * simply staying installed that long is already the stronger sign that the app is in use, so
 * counting launches would only add bookkeeping without changing the answer.
 *
 * The first ask waits [FIRST_ASK_DAYS]; "later" buys exactly one more, at [LAST_ASK_DAYS];
 * after that — or after "never ask" / a tap on "rate" — nothing is ever shown again.
 *
 * [BuildConfig.ALWAYS_ASK_FOR_REVIEW] (a test build, see app/build.gradle.kts) overrides all of
 * it: due on every launch, and the buttons deliberately change nothing.
 */
object ReviewStore {
    const val FIRST_ASK_DAYS = 60L
    const val LAST_ASK_DAYS = 180L

    private const val PREFS_NAME = "no_app_review"
    private const val KEY_STATE = "state"
    private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

    /** WAITING: first ask still ahead. SNOOZED: shown once, the second ask is the only one left. */
    private enum class State { WAITING, SNOOZED, DONE }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun state(context: Context): State =
        runCatching { State.valueOf(prefs(context).getString(KEY_STATE, null) ?: State.WAITING.name) }
            .getOrDefault(State.WAITING)

    private fun daysSinceInstall(context: Context): Long = runCatching {
        val installedAt = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        (System.currentTimeMillis() - installedAt) / MS_PER_DAY
    }.getOrDefault(0L)

    /** Whether the card may be shown right now; the caller decides where it lands. */
    fun cardDue(context: Context): Boolean = BuildConfig.ALWAYS_ASK_FOR_REVIEW || when (state(context)) {
        State.WAITING -> daysSinceInstall(context) >= FIRST_ASK_DAYS
        State.SNOOZED -> daysSinceInstall(context) >= LAST_ASK_DAYS
        State.DONE -> false
    }

    /** "Later": the first ask returns once at [LAST_ASK_DAYS], the second one never returns. */
    fun snooze(context: Context) {
        if (BuildConfig.ALWAYS_ASK_FOR_REVIEW) return
        val next = if (state(context) == State.WAITING) State.SNOOZED else State.DONE
        prefs(context).edit().putString(KEY_STATE, next.name).apply()
    }

    /** "Never ask", or a tap on "rate" — either way we are done asking. */
    fun stopAsking(context: Context) {
        if (BuildConfig.ALWAYS_ASK_FOR_REVIEW) return
        prefs(context).edit().putString(KEY_STATE, State.DONE.name).apply()
    }
}
