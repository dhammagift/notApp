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

    private fun daysSinceInstall(context: Context): Long = runCatching {
        val installedAt = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        (System.currentTimeMillis() - installedAt) / MS_PER_DAY
    }.getOrDefault(0L)

    /** Whether the card may be shown right now; the caller decides where it lands. */
    fun cardDue(context: Context): Boolean = !answered(context) &&
        (BuildConfig.ALWAYS_ASK_FOR_REVIEW || daysSinceInstall(context) >= ASK_FROM_DAYS)

    /** "Never ask", or a tap on "rate" — either way the card is answered and never returns. */
    fun stopAsking(context: Context) {
        if (BuildConfig.ALWAYS_ASK_FOR_REVIEW) return
        prefs(context).edit().putBoolean(KEY_DONE, true).apply()
    }
}
