package com.noapp.container.data

import android.content.Context
import com.noapp.container.model.AppConfig
import com.noapp.container.model.AppMode
import com.noapp.container.model.AppTheme
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for the config: JSON encode/decode is shared verbatim
 * between SharedPreferences persistence and file export/import, so there is
 * exactly one serialization format to keep correct. The slot list is
 * variable-length — array order IS the id, no separate "id" field on the wire.
 */
object ConfigStore {
    const val PEEK_SIZE_MIN = 0.75f
    const val PEEK_SIZE_MAX = 2f
    const val PEEK_ALPHA_MIN = 0.2f
    const val PEEK_ALPHA_MAX = 1f
    const val PEEK_DOCK_MIN = 0.4f
    const val PEEK_DOCK_MAX = 0.85f

    private const val PREFS_NAME = "no_app_prefs"
    private const val KEY_CONFIG_JSON = "config_json"
    private val COLOR_HEX = Regex("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")

    fun toJson(config: AppConfig): String {
        val arr = JSONArray()
        config.slots.forEach { slot ->
            arr.put(
                JSONObject().apply {
                    put("type", slot.type?.name ?: JSONObject.NULL)
                    put("label", slot.label)
                    put("color", slot.color)
                    put("param", slot.param)
                    put("customIcon", slot.customIcon)
                }
            )
        }
        return JSONObject()
            .put("mode", config.mode.name)
            .put("slots", arr)
            .put("useAllSlotsInDirectMode", config.useAllSlotsInDirectMode)
            .put("iconVariant", config.iconVariant)
            .put("showPeekBubble", config.showPeekBubble)
            .put("peekBubbleReturns", config.peekBubbleReturns)
            .put("peekBubbleSize", config.peekBubbleSize.toDouble())
            .put("peekBubbleAlpha", config.peekBubbleAlpha.toDouble())
            .put("peekBubbleDockPeek", config.peekBubbleDockPeek.toDouble())
            .put("showRecentApps", config.showRecentApps)
            .put("theme", config.theme.name)
            .put("tileSlot", config.tileSlot)
            .toString()
    }

    /** Lenient: whatever is stored always loads, a broken value falls back to the defaults. */
    fun fromJson(json: String): AppConfig = runCatching { parse(json) }.getOrDefault(AppConfig())

    /**
     * Strict, for an imported file: throws unless [json] is a Not App config (an object with a
     * "slots" array), so a wrong or broken file is refused instead of replacing the list with an
     * empty default one.
     */
    fun parse(json: String): AppConfig {
        val root = JSONObject(json)
        val mode = runCatching { AppMode.valueOf(root.optString("mode", AppMode.LIST.name)) }
            .getOrDefault(AppMode.LIST)
        val arr = root.getJSONArray("slots")
        val slots = (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val type = if (obj.isNull("type")) null else {
                // "CUSTOM" was merged into INTENT (identical behavior) — keep old configs working.
                val typeName = obj.getString("type").let { if (it == "CUSTOM") "INTENT" else it }
                runCatching { SlotType.valueOf(typeName) }.getOrNull()
            }
            ShortcutSlot(
                id = i,
                type = type,
                label = obj.optString("label", ""),
                // Our own exports always hold #RRGGBB; anything else (a typo, "#12", JSON null,
                // which Android's optString reads as "null") would not draw.
                color = obj.optString("color", ShortcutSlot.DEFAULT_COLOR).takeIf { COLOR_HEX.matches(it) } ?: ShortcutSlot.DEFAULT_COLOR,
                param = obj.optString("param", ""),
                customIcon = obj.optString("customIcon", "")
            )
        }
        return AppConfig(
            mode = mode,
            slots = slots.ifEmpty { ShortcutSlot.emptySlots() },
            useAllSlotsInDirectMode = root.optBoolean("useAllSlotsInDirectMode", false),
            iconVariant = root.optString("iconVariant", "default"),
            showPeekBubble = root.optBoolean("showPeekBubble", false),
            peekBubbleReturns = root.optBoolean("peekBubbleReturns", false),
            peekBubbleSize = root.optDouble("peekBubbleSize", 1.0).toFloat().coerceIn(PEEK_SIZE_MIN, PEEK_SIZE_MAX),
            peekBubbleAlpha = root.optDouble("peekBubbleAlpha", 0.9).toFloat().coerceIn(PEEK_ALPHA_MIN, PEEK_ALPHA_MAX),
            // Anything at or below the old default was never a choice — it is what the previous
            // range clamped to — so it comes back as the new middle instead of a sliver.
            peekBubbleDockPeek = root.optDouble("peekBubbleDockPeek", AppConfig.DEFAULT_DOCK_PEEK.toDouble())
                .toFloat()
                .let { if (it <= 0.45f) AppConfig.DEFAULT_DOCK_PEEK else it }
                .coerceIn(PEEK_DOCK_MIN, PEEK_DOCK_MAX),
            showRecentApps = root.optBoolean("showRecentApps", false),
            theme = runCatching { AppTheme.valueOf(root.optString("theme", AppTheme.SYSTEM.name)) }
                .getOrDefault(AppTheme.SYSTEM),
            tileSlot = root.optString("tileSlot", AppConfig.TILE_NONE)
        )
    }

    fun load(context: Context): AppConfig {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CONFIG_JSON, null) ?: return AppConfig()
        return fromJson(json)
    }

    fun save(context: Context, config: AppConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONFIG_JSON, toJson(config))
            .apply()
    }
}
