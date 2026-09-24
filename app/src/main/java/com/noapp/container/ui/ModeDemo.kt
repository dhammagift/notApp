package com.noapp.container.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noapp.container.R
import com.noapp.container.icon.SlotIcon
import com.noapp.container.icon.monogramBitmap
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot

/**
 * One picture of how the mode that is selected right now behaves, shown under the picker's three
 * paragraphs — the cards themselves stay text only, so the example reads as a footnote to them.
 *
 * Only one mode is ever drawn, and that is the point: this dialog closes on the tap that selects a
 * mode, so there is no way to look at another mode's example without choosing it. Drawing it from
 * [slots] — the user's own config — is what makes it their case rather than a generic diagram; with
 * nothing configured the rows are numbered placeholders, which is exactly what an empty config
 * looks like. Nothing is launched to draw it.
 */
@Composable
fun ModeDemo(mode: AppMode, slots: List<ShortcutSlot>, modifier: Modifier = Modifier) {
    val items = if (slots.isEmpty()) List(DEMO_ROWS) { ShortcutSlot(id = it) } else slots
    Surface(
        modifier = modifier.fillMaxWidth().height(DEMO_HEIGHT),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        // A hairline as well as the colour step: with dynamic colour the theme's own surface can
        // land very close to the dialog's own background, and the example must still read as a
        // panel rather than as loose rows.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        when (mode) {
            AppMode.LIST -> Box(Modifier.padding(vertical = 8.dp)) {
                Column {
                    items.take(DEMO_ROWS).forEachIndexed { index, slot ->
                        DemoRow(slot, index + 1, iconSize = 28.dp, labelStyle = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Only the icon of what would open, plus the one line that says so.
            AppMode.DIRECT -> Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DemoIcon(items[0], 1, 64.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.mode_demo_direct_opens, labelOf(items[0], 1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The same icon, with the list of the rest starting halfway down over it: the mode's
            // own "one item, then everything else" shape.
            AppMode.MIX -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                DemoIcon(items[0], 1, 64.dp, modifier = Modifier.padding(top = 12.dp))
                Surface(
                    modifier = Modifier.padding(top = 44.dp, start = 28.dp, end = 28.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 3.dp
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        items.drop(1).take(MIX_SHEET_ROWS).forEachIndexed { index, slot ->
                            DemoRow(slot, index + 2, iconSize = 24.dp, labelStyle = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoRow(slot: ShortcutSlot, number: Int, iconSize: Dp, labelStyle: TextStyle) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DemoIcon(slot, number, iconSize)
        Spacer(Modifier.width(12.dp))
        Text(
            labelOf(slot, number),
            style = labelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A configured slot shows its real icon; an empty one shows its number, as the list itself does. */
@Composable
private fun DemoIcon(slot: ShortcutSlot, number: Int, size: Dp, modifier: Modifier = Modifier) {
    if (slot.isConfigured) {
        SlotIcon(slot, size = size, modifier = modifier)
        return
    }
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(number, sizePx) {
        monogramBitmap(
            text = number.toString(),
            colorHex = ShortcutSlot.PALETTE[(number - 1) % ShortcutSlot.PALETTE.size],
            sizePx = sizePx
        ).asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
}

@Composable
private fun labelOf(slot: ShortcutSlot, number: Int): String =
    slot.label.ifBlank { stringResource(R.string.common_item_n, number) }

private val DEMO_HEIGHT = 160.dp
private const val DEMO_ROWS = 4
private const val MIX_SHEET_ROWS = 3
