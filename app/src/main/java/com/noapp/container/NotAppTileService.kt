package com.noapp.container

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.noapp.container.model.AppMode
import com.noapp.container.data.ConfigStore
import com.noapp.container.model.AppConfig
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.shortcuts.ActionDispatcher
import com.noapp.container.shortcuts.withLaunchToken

/**
 * Not App in the Quick Settings shade, in one of two shapes:
 *
 *  - no slot assigned (the default): the same single tap the launcher icon gives, reachable from
 *    inside another app, from the lock screen or from the shade — which a home screen icon is not.
 *  - a slot assigned (the rocket marker on a row): that slot, with the row's own label on the tile.
 *    In Mix it behaves like Mix does everywhere — the slot runs and the list comes up over it, with
 *    that item left out, which is why the tile hands the job to MainActivity; in List and Direct it
 *    runs the slot and nothing else, because there the list is either the app's own screen or not
 *    part of a plain launch at all.
 *
 * The icon is always the app's rocket, never the target app's icon: half the icons in a launcher
 * are a white square at 24dp, and a tile that keeps one face and changes only its label is easier
 * to find in the shade than one whose picture moves (owner, 2026-09-24).
 *
 * The tile never invents a target of its own: it asks ActionDispatcher for the slot's intent, the
 * same call the list makes, so a tap in the shade and a tap in the list cannot come to mean two
 * different things.
 *
 * The tile is invisible until the reader drags it into the shade — Android's rule for every app.
 */
class NotAppTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val slot = assignedSlot()
        // The assignment lives in the app's config, which the manifest's static label and icon
        // cannot know, so both are refreshed every time the shade is pulled down.
        tile.label = slot?.label?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val slot = assignedSlot()
        val mode = ConfigStore.load(this).mode

        // Mix launches a slot AND shows the list over it, and a tile may only start one activity (a
        // PendingIntent, on API 34+), so in Mix the tile starts MainActivity, which does both — the
        // same thing a plain tap on the launcher icon does for slot 0.
        // Everywhere else the tile runs the slot itself and nothing else: List is the app's own
        // screen and Direct has no list, so there is nothing to show over the launched app.
        // A slot that no longer resolves (its app was uninstalled, the assignment was edited away)
        // falls back to the launcher-icon behaviour rather than doing nothing at all.
        val intent = when {
            slot == null -> launcherIntent()
            mode == AppMode.MIX -> Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_TILE_TARGET, slot.targetKey)
                .withLaunchToken(this)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> ActionDispatcher.intentFor(this, slot) ?: launcherIntent()
        }
        collapse(intent)
    }

    /** The slot the settings point this tile at, or null for "act like the launcher icon". */
    private fun assignedSlot(): ShortcutSlot? {
        val config = ConfigStore.load(this)
        if (config.tileSlot == AppConfig.TILE_NONE) return null
        return config.slots.firstOrNull { it.targetKey == config.tileSlot }
    }

    private fun launcherIntent() = Intent(this, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory(Intent.CATEGORY_LAUNCHER)
        // MainActivity is singleTask: CLEAR_TOP brings the existing task forward instead of stacking
        // a second copy of the list on top of the first.
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    private fun collapse(intent: Intent) {
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
