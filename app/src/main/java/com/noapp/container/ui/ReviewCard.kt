package com.noapp.container.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.noapp.container.R
import com.noapp.container.data.ReviewStore
import com.noapp.container.icon.enabledLauncherComponent

/** Opens the app's Play page, with the browser as the fallback for devices without the store. */
fun openStorePage(context: Context) {
    val pkg = context.packageName
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    }.onFailure {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
        )
    }
}

/**
 * The rating ask, and deliberately a card rather than a dialog: it can sit at the end of the
 * shortcut list, be ignored without being dismissed, and it carries Not App's own launcher icon
 * and name — which is what makes it safe to show in every mode, including DIRECT, where the app
 * opens someone else's app and a bare "rate us" would read as being about that one.
 */
@Composable
fun ReviewCard(onDismissed: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                appIconBitmap()?.let { icon ->
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        stringResource(R.string.review_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        stringResource(R.string.review_card_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            // Actions on their own full-width row: three buttons next to the icon column would
            // run out of room on a narrow screen and clip the last label.
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    ReviewStore.stopAsking(context)
                    onDismissed()
                }) { Text(stringResource(R.string.review_card_never)) }
                TextButton(onClick = {
                    ReviewStore.snooze(context)
                    onDismissed()
                }) { Text(stringResource(R.string.review_card_later)) }
                Button(onClick = {
                    ReviewStore.stopAsking(context)
                    openStorePage(context)
                    onDismissed()
                }) { Text(stringResource(R.string.review_card_rate)) }
            }
        }
    }
}

/**
 * [ReviewCard] only while [ReviewStore] says it is due. The due check happens once per
 * composition and is then owned by local state, so answering the card removes it right away
 * instead of waiting for the next screen to be built.
 */
@Composable
fun ReviewCardIfDue(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var due by remember { mutableStateOf(ReviewStore.cardDue(context)) }
    if (due) ReviewCard(onDismissed = { due = false }, modifier = modifier)
}

/** The icon the user actually sees on their home screen, whichever variant is enabled. */
@Composable
private fun appIconBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember {
        runCatching {
            context.packageManager.getActivityIcon(enabledLauncherComponent(context)).toBitmap(96, 96)
        }.getOrNull()?.asImageBitmap()
    }
}
