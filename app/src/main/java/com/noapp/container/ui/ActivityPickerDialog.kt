package com.noapp.container.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noapp.container.R

private data class ActivityChoice(val className: String, val label: String)

/**
 * Lists one installed app's exported activities so a specific screen (not just its main
 * launcher entry) can be turned into an explicit-intent shortcut, replacing hand-typed
 * intent:// strings. Non-exported activities are omitted — launching them throws anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityPickerDialog(packageName: String, onDismiss: () -> Unit, onPick: (className: String) -> Unit) {
    val context = LocalContext.current
    val activities = remember(packageName) {
        val pm = context.packageManager
        runCatching {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
                ?.filter { it.exported }
                ?.map { ActivityChoice(it.name, it.loadLabel(pm).toString()) }
                ?.sortedBy { it.label.lowercase() }
        }.getOrNull().orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.activity_picker_title)) },
        text = {
            if (activities.isEmpty()) {
                Text(stringResource(R.string.activity_picker_empty))
            } else {
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(activities, key = { it.className }) { activity ->
                        ListItem(
                            headlineContent = { Text(activity.label) },
                            supportingContent = { Text(activity.className, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable { onPick(activity.className) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}
