package com.noapp.container.shortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.noapp.container.R
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType

/** Executes one slot's target. [sharedText] is non-null only when triggered via the Sharing API. */
object ActionDispatcher {
    fun execute(context: Context, slot: ShortcutSlot, sharedText: String? = null) {
        if (!slot.isConfigured) return
        // A configured item that cannot be built (its app was uninstalled, a broken intent URI) says
        // so, instead of the sheet closing on nothing.
        runCatching {
            context.startActivity(buildIntent(context, slot, sharedText).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(context, context.getString(R.string.toast_launch_failed, slot.label, it.message), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * The intent [execute] would start, or null when the slot has nothing to run.
     *
     * Split out for the Quick Settings tile (NotAppTileService): a tile may not call startActivity
     * itself on API 34+, it has to hand a PendingIntent to startActivityAndCollapse — but which
     * intent a slot means must stay one implementation, or the tile and a list tap could drift.
     */
    fun intentFor(context: Context, slot: ShortcutSlot, sharedText: String? = null): Intent? {
        if (!slot.isConfigured) return null
        return runCatching { buildIntent(context, slot, sharedText) }.getOrNull()
    }

    private fun buildIntent(context: Context, slot: ShortcutSlot, sharedText: String?): Intent =
        when (slot.type) {
            SlotType.APP -> appIntent(context, slot.param, sharedText)
            SlotType.URL -> Intent(Intent.ACTION_VIEW, Uri.parse(resolveTemplate(slot.param, sharedText)))
            SlotType.INTENT -> sanitized(context, Intent.parseUri(resolveTemplate(slot.param, sharedText), Intent.URI_INTENT_SCHEME))
            null -> error("No type")
        }

    /**
     * An intent URI may come from someone else's config file, and started by Not App it would carry
     * Not App's own rights: so none of Not App's own screens (exported or not) and no selector, which
     * could swap in another target. URI grant flags parseUri already drops itself (without
     * URI_ALLOW_UNSAFE), so the FileProvider can't be handed out through launchFlags.
     */
    private fun sanitized(context: Context, intent: Intent): Intent {
        require(intent.component?.packageName != context.packageName && intent.`package` != context.packageName) {
            "Not App's own screens can't be an item"
        }
        intent.selector = null
        return intent
    }

    /**
     * {{word}} in a URL/Intent param is replaced with the shared text (URL-encoded). A plain tap has
     * no text, so the placeholder goes away: a preset like wa.me/{{word}} then opens the app's own
     * page instead of a search for the literal "{{word}}".
     */
    private fun resolveTemplate(param: String, sharedText: String?): String =
        param.replace("{{word}}", if (sharedText != null) Uri.encode(sharedText) else "")

    /**
     * Forward shared text natively via ACTION_SEND if the target app can receive it
     * (e.g. a translator or notes app); otherwise fall back to a plain launch.
     */
    private fun appIntent(context: Context, packageName: String, sharedText: String?): Intent {
        if (sharedText != null) {
            val sendIntent = Intent(Intent.ACTION_SEND)
                .setPackage(packageName)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, sharedText)
            if (sendIntent.resolveActivity(context.packageManager) != null) return sendIntent
        }
        return context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalStateException("App not installed")
    }
}
