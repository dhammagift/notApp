package com.noapp.container.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
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
 * mode, so there is no way to look at another mode's example without choosing it.
 *
 * The list here is the real one, not a diagram of one: the same rows the sheet builds (a [ListItem]
 * with the slot's own icon and label), under the same drag handle, and it really scrolls when the
 * config holds more items than fit. That is also why it is capped by [LIST_HEIGHT] rather than
 * growing with the config: uncapped, a long list would push the three descriptions off the screen.
 * With nothing configured the rows are numbered placeholders, which is what an empty config looks
 * like. Nothing is ever launched to draw any of it.
 */
@Composable
fun ModeDemo(mode: AppMode, slots: List<ShortcutSlot>, modifier: Modifier = Modifier) {
    val items = if (slots.isEmpty()) List(PLACEHOLDER_ROWS) { ShortcutSlot(id = it) } else slots
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
            AppMode.LIST -> DemoSheet(
                items = items,
                startNumber = 1,
                listHeight = LIST_HEIGHT,
                modifier = Modifier.fillMaxSize()
            )

            AppMode.DIRECT -> Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DemoIcon(items[0], 1, HERO_ICON)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.mode_demo_direct_opens, labelOf(items[0], 1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The same hero icon, with the real list of the rest starting halfway down over it:
            // the mode's own "one item, then everything else" shape.
            AppMode.MIX -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                DemoIcon(items[0], 1, HERO_ICON, modifier = Modifier.padding(top = DEMO_PADDING))
                DemoSheet(
                    items = items.drop(1),
                    startNumber = 2,
                    listHeight = MIX_LIST_HEIGHT,
                    modifier = Modifier
                        .padding(top = DEMO_PADDING + HERO_ICON / 2, start = 24.dp, end = 24.dp)
                        .height(MIX_SHEET_HEIGHT)
                )
            }
        }
    }
}

/** The sheet's own chrome: same handle, same rows, capped so it cannot grow past its height. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoSheet(
    items: List<ShortcutSlot>,
    startNumber: Int,
    listHeight: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(top = DEMO_PADDING)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                BottomSheetDefaults.DragHandle()
            }
            LazyColumn(Modifier.height(listHeight)) {
                itemsIndexed(items, key = { index, _ -> index }) { index, slot ->
                    // Same row as the real sheet: the slot's icon and its label, nothing added.
                    ListItem(
                        headlineContent = {
                            Text(
                                labelOf(slot, startNumber + index),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = { DemoIcon(slot, startNumber + index, 32.dp) }
                    )
                }
            }
        }
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

/** Enough for the panel; the rows inside keep the sheet's real single-line height. */
private val DEMO_HEIGHT = 208.dp
private val DEMO_PADDING = 8.dp
private val HERO_ICON = 104.dp
private val LIST_HEIGHT = 168.dp
private val MIX_LIST_HEIGHT = 112.dp
private val MIX_SHEET_HEIGHT = 142.dp
private const val PLACEHOLDER_ROWS = 4
