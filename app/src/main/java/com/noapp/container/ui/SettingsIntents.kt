package com.noapp.container.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * A system settings screen for one of our permissions, scrolled to this app with its row highlighted.
 *
 * Since Android 11 "Display over other apps" always opens as the list of all apps (the package in the
 * URI is ignored), and "Usage access" was always a list: the person had to find the app in it. The two
 * extras are the keys the Settings app's own search uses to land on an entry; undocumented, so a settings
 * app that does not know them shows the plain list, as before.
 */
internal fun appSettingsIntent(context: Context, action: String, withPackageUri: Boolean = true): Intent {
    val pkg = context.packageName
    val intent = if (withPackageUri) Intent(action, Uri.parse("package:$pkg")) else Intent(action)
    intent.putExtra(":settings:fragment_args_key", pkg)
    intent.putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", pkg) })
    return intent
}
