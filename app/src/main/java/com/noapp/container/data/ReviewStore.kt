package com.noapp.container.data

import android.content.Context
import com.noapp.container.BuildConfig

/**
 * When Not App may ask for a Play rating.
 *
 * One threshold and one flag is the whole machine: from [ASK_FROM_DAYS] days installed the card
 * is due, and it stays due — every launch, every visit to Settings — until the user answers it
 * with "rate" or "never ask". "Later" is not an answer: it only hides the card on the screen it
 * was shown on, and is not remembered. Days installed rather than a launch counter, because
 * staying installed that long already says more than counting launches would.
 *
 * [BuildConfig.ALWAYS_ASK_FOR_REVIEW] (a test build, see app/build.gradle.kts) skips even the day
 * count, so the card is there on the very next launch.
 */
object ReviewStore {
    const val ASK_FROM_DAYS = 60L

    private const val PREFS_NAME = "no_app_review"
    private const val KEY_DONE = "done"
    private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun answered(context: Context): Boolean = prefs(context).getBoolean(KEY_DONE, false)

    /**
     * The two facts the policy is built on, behind a seam so the tests can move the calendar instead
     * of waiting two months: the app itself always reads the real clock and the real install time.
     */
    internal var clock: () -> Long = { System.currentTimeMillis() }
    internal var installedAt: (Context) -> Long = { context ->
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(0L)
    }

    private fun daysSinceInstall(context: Context): Long =
        ((clock() - installedAt(context)) / MS_PER_DAY).coerceAtLeast(0L)

    /** Whether the card may be shown right now; the caller decides where it lands. */
    fun cardDue(context: Context): Boolean = !answered(context) &&
        (BuildConfig.ALWAYS_ASK_FOR_REVIEW || daysSinceInstall(context) >= ASK_FROM_DAYS)

    /** "Never ask", or a tap on "rate" — either way the card is answered and never returns. */
    fun stopAsking(context: Context) {
        if (BuildConfig.ALWAYS_ASK_FOR_REVIEW) return
        prefs(context).edit().putBoolean(KEY_DONE, true).apply()
    }
}
