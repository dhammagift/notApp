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
 * A small, honest picture of what a mode does, shown inside the picker's own mode cards.
 *
 * It is a picture and not a live list on purpose: the picker's whole job is to say what the three
 * modes mean before one of them is chosen, so each card carries its own demo rather than one
 * shared preview area — a shared one could only ever show the mode that is already selected,
 * because in this dialog tapping a card both chooses and closes it.
 *
 * The rows are the user's real slots and their real icons, so a configured app shows its own icon.
 * With nothing configured yet the same rows are numbered placeholders, matching what an empty
 * config actually looks like.
 */
@Composable
fun ModeDemo(mode: AppMode, slots: List<ShortcutSlot>, modifier: Modifier = Modifier) {
    val items = if (slots.isEmpty()) List(DEMO_ROWS) { ShortcutSlot(id = it) } else slots
    Surface(
        modifier = modifier.fillMaxWidth().height(DEMO_HEIGHT),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        // A hairline as well as the colour step: with dynamic colour the theme's own surface can
        // land very close to the card it sits on, and the picture must still read as a panel.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        when (mode) {
            AppMode.LIST -> Box(Modifier.padding(vertical = 6.dp)) {
                Column {
                    items.take(DEMO_ROWS).forEachIndexed { index, slot ->
                        DemoRow(slot, index + 1, iconSize = 26.dp, labelStyle = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Nothing is opened here: the icon of what WOULD open is the whole point, plus the
            // one line that says so.
            AppMode.DIRECT -> Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DemoIcon(items[0], 1, 56.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.mode_demo_direct_opens, labelOf(items[0], 1)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The icon first, and the list of the rest starting halfway down over it — the same
            // "one item, then everything else" shape the mode itself has.
            AppMode.MIX -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                DemoIcon(items[0], 1, 56.dp, modifier = Modifier.padding(top = 8.dp))
                Surface(
                    modifier = Modifier.padding(top = 36.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 3.dp
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        items.drop(1).take(MIX_SHEET_ROWS).forEachIndexed { index, slot ->
                            DemoRow(slot, index + 2, iconSize = 22.dp, labelStyle = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoRow(
    slot: ShortcutSlot,
    number: Int,
    iconSize: Dp,
    labelStyle: TextStyle
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DemoIcon(slot, number, iconSize)
        Spacer(Modifier.width(10.dp))
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

private val DEMO_HEIGHT = 118.dp
private const val DEMO_ROWS = 3
private const val MIX_SHEET_ROWS = 2
