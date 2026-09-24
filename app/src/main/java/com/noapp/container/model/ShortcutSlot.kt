package com.noapp.container.model

enum class SlotType { APP, URL, INTENT }

/**
 * One entry in a variable-length, user-ordered list (see [AppConfig]). [id] is
 * always its current list position, reassigned on every add/remove/reorder —
 * not a stable identity. In AppMode.DIRECT, id 0 is special ("Main": fires on
 * a plain tap); in AppMode.LIST every entry is just a row in the shown list.
 * [param] holds the type-specific payload: package name (APP), URL (URL),
 * or an intent URI string parsed via Intent.parseUri (INTENT).
 * [customIcon], if set, overrides the icon entirely (an emoji or short text,
 * drawn on a [color] badge) — otherwise APP slots show the target app's own
 * launcher icon and everything else falls back to a monogram of [label].
 */
data class ShortcutSlot(
    val id: Int,
    val type: SlotType? = null,
    val label: String = "",
    val color: String = DEFAULT_COLOR,
    val param: String = "",
    val customIcon: String = ""
) {
    val isConfigured: Boolean get() = type != null && param.isNotBlank()

    /**
     * What this slot opens, as a string that survives reordering and reloading: type plus param.
     * Used by AppConfig.tileSlot to mark which row the Quick Settings tile runs (see
     * NotAppTileService) — a row's own id cannot do that job, because ids are rebuilt from positions
     * on every load, so a reordered list would move the mark to whatever landed in that slot.
     *
     * Null for an empty row, which has nothing to point at.
     */
    val targetKey: String? get() = if (isConfigured) "${type?.name}:$param" else null

    companion object {
        const val DEFAULT_COLOR = "#4A6FA5"
        // Muted, "designed" tones instead of raw web RGB — requested order:
        // blue, red, yellow, white, orange, gray, lilac.
        val PALETTE = listOf("#4A6FA5", "#C0574C", "#D9A441", "#F1EFE9", "#C97A3D", "#8B8D91", "#9A7FB8")
        fun emptySlots(): List<ShortcutSlot> = (0..4).map { ShortcutSlot(id = it) }
    }
}
