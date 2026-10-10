package com.noapp.container.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * A system settings screen for one of our permissions, as close to this app's own switch as Android lets us get.
 *
 * "Usage access" with the package opens this app's own page. "Display over other apps" has ignored the package
 * since Android 11 and opens the list of all apps. The two extras are the keys the Settings app's own search uses
 * to scroll to an entry and flash it: they work on screens built of preferences (the "Do Not Disturb access" list),
 * but AOSP's lists of apps (ManageApplications, 11-15) do not read them, so there they only help where a
 * manufacturer's Settings does.
 */
internal fun appSettingsIntent(context: Context, action: String, withPackageUri: Boolean = true): Intent {
    val pkg = context.packageName
    val intent = if (withPackageUri) Intent(action, Uri.parse("package:$pkg")) else Intent(action)
    intent.putExtra(":settings:fragment_args_key", pkg)
    intent.putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", pkg) })
    return intent
}
