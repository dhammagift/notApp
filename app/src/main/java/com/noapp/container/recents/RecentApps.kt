package com.noapp.container.recents

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

data class RecentApp(val packageName: String, val label: String)

const val MAX_RECENT_APPS = 5
private const val LOOKBACK_MS = 24 * 60 * 60 * 1000L // plenty to fill a handful of slots

/**
 * A true "what's currently running" task list (like the OS Recents/Overview screen) isn't
 * readable by any third-party app on modern Android — that API is restricted to the system's
 * own launcher/SystemUI. UsageStatsManager's foreground-event history, gated on the user
 * separately granting Usage Access in system Settings, is the closest legitimate proxy: apps
 * ordered by when they were last brought to the foreground.
 */
object RecentApps {
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // unsafeCheckOpNoThrow only exists from API 29; below that it is a NoSuchMethodError on
        // Android 7-9 (minSdk is 24), and checkOpNoThrow is the same call under its old name.
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Most-recently-foregrounded launchable apps other than this one, most recent first. */
    fun query(context: Context, limit: Int = MAX_RECENT_APPS): List<RecentApp> {
        if (!hasUsageAccess(context)) return emptyList()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(end - LOOKBACK_MS, end)

        val timeline = mutableListOf<String>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                timeline.add(event.packageName)
            }
        }

        val pm = context.packageManager
        val ownPackage = context.packageName
        val seen = LinkedHashSet<String>()
        for (pkg in timeline.asReversed()) {
            if (seen.size >= limit) break
            if (pkg == ownPackage || pkg in seen) continue
            if (pm.getLaunchIntentForPackage(pkg) == null) continue // not a launchable app
            seen.add(pkg)
        }

        return seen.map { pkg ->
            val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                .getOrDefault(pkg)
            RecentApp(pkg, label)
        }
    }
}
