package com.noapp.container

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/**
 * Not App in the Quick Settings shade: the same single tap the launcher icon gives, reachable from
 * inside another app, from the lock screen or from the shade — which a home screen icon is not.
 *
 * It deliberately carries no logic of its own. The tap builds the MAIN/LAUNCHER intent that every
 * one of the activity-alias entries in the manifest targets, so the mode (LIST, DIRECT, MIX), the
 * primary slot and the current icon variant are all decided by the app exactly as they are for an
 * icon tap. A second entry point with its own idea of "the primary action" would be one more thing
 * to keep in step with AppMode.
 *
 * The tile stays invisible until the reader drags it into the shade — Android's rule for every app,
 * not something an app can skip.
 */
class NotAppTileService : TileService() {

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            // MainActivity is singleTask: CLEAR_TOP brings the existing task forward instead of
            // stacking a second copy of the list on top of the first.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34 deprecated the Intent overload, and a tile must hand over a PendingIntent
            // there; FLAG_IMMUTABLE is required from API 31 on.
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
