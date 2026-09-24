package com.noapp.container.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noapp.container.R
import com.noapp.container.icon.SlotIcon
import com.noapp.container.icon.monogramBitmap
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import kotlin.math.roundToInt

/**
 * The example at the foot of the mode picker: how the mode that is selected right now behaves,
 * drawn from the user's real slots. The cards above stay text only, so this reads as their picture.
 *
 * It fills whatever height the picker leaves it, and its rows are the real ones rather than a
 * diagram of them: the same [ListItem] the sheet builds, with the slot's own icon and label, under
 * the same drag handle, in a real [LazyColumn] — so a long config scrolls here exactly as it does in
 * the sheet. Swiping the handle down collapses it to that handle alone and tapping the handle brings
 * it back, which is what the real sheet does with a downward swipe.
 *
 * It shows the mode that is on right now simply because it can only show that one — the dialog
 * closes on the tap that selects a mode, so there is no way to preview another without choosing it.
 * With nothing configured the rows are numbered placeholders, which is what an empty config looks
 * like. Nothing is ever launched to draw any of it.
 */
@Composable
fun ModeDemo(
    mode: AppMode,
    slots: List<ShortcutSlot>,
    onShowShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = if (slots.isEmpty()) List(PLACEHOLDER_ROWS) { ShortcutSlot(id = it) } else slots
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        // A hairline as well as the colour step: with dynamic colour the theme's own surface can
        // land very close to the dialog's own background, and the example must still read as a
        // panel rather than as loose rows.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        when (mode) {
            AppMode.LIST -> DemoSheet(items, startNumber = 1, modifier = Modifier.fillMaxSize())

            AppMode.DIRECT -> Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                HeroIcon(items[0], 1, onShowShortcuts)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.mode_demo_direct_opens, labelOf(items[0], 1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DemoHint()
            }

            // The same hero icon, with the real list of the rest starting at its midline: the mode's
            // own "one item, then everything else" shape. A column rather than fixed offsets, so it
            // stays right at any panel height.
            AppMode.MIX -> Column(Modifier.fillMaxSize()) {
                DemoHint(Modifier.padding(top = 10.dp))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    HeroIcon(items[0], 1, onShowShortcuts, Modifier.align(Alignment.TopCenter))
                    DemoSheet(
                        items = items.drop(1),
                        startNumber = 2,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = HERO_ICON / 2, start = 24.dp, end = 24.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/**
 * The sheet's own chrome: same handle, same rows, and a downward swipe on the handle collapses it
 * to that handle alone — the sheet's own gesture, with the list left scrollable underneath it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoSheet(items: List<ShortcutSlot>, startNumber: Int, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { COLLAPSE_THRESHOLD.toPx() }
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var collapsed by remember { mutableStateOf(false) }
    val settledPx by animateFloatAsState(
        targetValue = if (collapsed) {
            (sheetHeightPx - with(density) { HANDLE_STRIP.toPx() }).coerceAtLeast(0f)
        } else {
            0f
        },
        animationSpec = tween(COLLAPSE_ANIM_MS),
        label = "demoSheet"
    )

    Surface(
        modifier = modifier
            .onSizeChanged { sheetHeightPx = it.height }
            .offset { IntOffset(0, (settledPx + dragPx).roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> dragPx += delta },
                onDragStopped = {
                    val moved = dragPx
                    dragPx = 0f
                    collapsed = if (collapsed) {
                        moved < -collapseThresholdPx
                    } else {
                        moved > collapseThresholdPx
                    }
                }
            ),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(top = DEMO_PADDING)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HANDLE_STRIP)
                    .clickable(enabled = collapsed) { collapsed = false },
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle()
            }
            if (!collapsed) {
                LazyColumn(Modifier.weight(1f)) {
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
}

/**
 * Our own drawing of the launcher's long-press menu. The real one belongs to the launcher and cannot
 * be embedded in an app, so this is the same four shortcuts in the same shape — app name, then the
 * shortcuts — over a dimmed screen, and it exists only in this picker.
 */
@Composable
fun ShortcutMenuOverlay(appName: String, slots: List<ShortcutSlot>, onDismiss: () -> Unit) {
    val items = if (slots.isEmpty()) List(SHORTCUT_MENU_ROWS) { ShortcutSlot(id = it) } else slots
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            // Taps on the card itself are swallowed, so only an outside tap dismisses it — the same
            // as the menu it imitates.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balances the info icon, so the name stays centred as it is in that menu.
                    Spacer(Modifier.width(24.dp))
                    Text(
                        appName,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                items.take(SHORTCUT_MENU_ROWS).forEachIndexed { index, slot ->
                    ListItem(
                        headlineContent = {
                            Text(labelOf(slot, index + 1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = { DemoIcon(slot, index + 1, 40.dp) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroIcon(
    slot: ShortcutSlot,
    number: Int,
    onShowShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.pointerInput(slot) {
            // Tap as well as hold: in here the example has to be discoverable, and neither gesture
            // can launch anything.
            detectTapGestures(onLongPress = { onShowShortcuts() }, onTap = { onShowShortcuts() })
        }
    ) {
        DemoIcon(slot, number, HERO_ICON)
    }
}

@Composable
private fun DemoHint(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.mode_demo_long_press),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
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

private val DEMO_PADDING = 8.dp
private val HERO_ICON = 104.dp
private val HANDLE_STRIP = 40.dp
private val COLLAPSE_THRESHOLD = 40.dp
private const val COLLAPSE_ANIM_MS = 220
private const val PLACEHOLDER_ROWS = 4
private const val SHORTCUT_MENU_ROWS = 4
private const val SCRIM_ALPHA = 0.32f
