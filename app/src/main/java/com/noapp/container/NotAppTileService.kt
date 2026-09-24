package com.noapp.container

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.noapp.container.data.ConfigStore
import com.noapp.container.data.LastLaunch
import com.noapp.container.model.AppConfig
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import com.noapp.container.shortcuts.ActionDispatcher

/**
 * Not App in the Quick Settings shade, in one of two shapes:
 *
 *  - no slot assigned (Settings > Shade tile, the default): the same single tap the launcher icon
 *    gives, reachable from inside another app, from the lock screen or from the shade — which a
 *    home screen icon is not.
 *  - a slot assigned: that slot, exactly as tapping it in the list would run it. The tile takes its
 *    label and icon from the slot, so the shade shows "Telegram" with Telegram's icon rather than
 *    another anonymous Not App tile.
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
        tile.icon = iconFor(slot)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val slot = assignedSlot()
        // A slot that no longer resolves (its app was uninstalled, its settings were cleared) falls
        // back to the launcher-icon behaviour rather than doing nothing at all.
        val intent = slot?.let { ActionDispatcher.intentFor(this, it) }
        if (intent != null) {
            // Recorded so the list, if it is opened over the app this launches, can leave that item
            // out — it is the app the user is looking at. See LastLaunch.
            LastLaunch.remember(this, slot.targetKey)
        }
        collapse(intent ?: launcherIntent())
    }

    /** The slot the settings point this tile at, or null for "act like the launcher icon". */
    private fun assignedSlot(): ShortcutSlot? {
        val config = ConfigStore.load(this)
        if (config.tileSlot == AppConfig.TILE_NONE) return null
        return config.slots.firstOrNull { it.targetKey == config.tileSlot }
    }

    /**
     * The icon the shade draws: the target app's own icon for an app slot, the app's rocket for
     * everything else (a URL or an explicit intent has no icon of its own to show, and the rocket is
     * what this tile has always used when it means "Not App itself").
     *
     * A fixed 96px bitmap rather than the drawable's intrinsic size: launcher icons are adaptive
     * drawables with no usable intrinsic bounds, and Tile.setIcon wants a bitmap anyway.
     */
    private fun iconFor(slot: ShortcutSlot?): Icon {
        val packageName = slot?.takeIf { it.type == SlotType.APP }?.param
        if (!packageName.isNullOrBlank()) {
            // Any failure — the app is gone, its icon cannot be read — falls through to the app's
            // own glyph: a tile that threw here would take the shade down with it.
            val icon = runCatching {
                val drawable = packageManager.getApplicationIcon(packageName)
                val size = TILE_ICON_PX
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(Canvas(bitmap))
                Icon.createWithBitmap(bitmap)
            }.getOrNull()
            if (icon != null) return icon
        }
        return Icon.createWithResource(this, R.drawable.ic_tile)
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

    private companion object {
        const val TILE_ICON_PX = 96
    }
}
