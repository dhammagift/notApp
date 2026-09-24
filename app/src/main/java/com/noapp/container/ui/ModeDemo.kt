package com.noapp.container.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noapp.container.R
import com.noapp.container.icon.AppIcon
import com.noapp.container.icon.SlotIcon
import com.noapp.container.icon.monogramBitmap
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.recents.RecentApp
import com.noapp.container.recents.RecentApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The example at the foot of the mode picker: how the mode that is selected right now behaves,
 * drawn from the user's real slots, and meant to be played with rather than only looked at.
 *
 * Everything in it is real: the sheet is the sheet's own rows ([ListItem] with the slot's icon and
 * label) under the same drag handle in a real [LazyColumn], with the recent-apps strip when that
 * setting is on; swiping the handle down collapses it to that handle and — when the floating button
 * is on — leaves the same round button the app would, which can be dragged onto ✕ or tapped to bring
 * the list back. Holding the icon opens the shortcut menu the launcher would show
 * ([ShortcutMenuOverlay]); tapping it shows what pressing it does instead: the item opens.
 *
 * The panel stands in for the screen: a dimmed brand backdrop with the app's own mark on it, labelled
 * as a demo. Nothing is ever launched and no setting is changed from here.
 */
@Composable
fun ModeDemo(
    mode: AppMode,
    slots: List<ShortcutSlot>,
    showRecentApps: Boolean,
    showPeekBubble: Boolean,
    useAllSlotsInDirectMode: Boolean,
    onShowShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = if (slots.isEmpty()) List(PLACEHOLDER_ROWS) { ShortcutSlot(id = it) } else slots
    val context = LocalContext.current
    var recentApps by remember { mutableStateOf<List<RecentApp>>(emptyList()) }
    // The sheet's own rule: the strip appears when the setting is on AND usage access was granted.
    // With either missing there is nothing to draw, and that row keeps only the gear.
    LaunchedEffect(showRecentApps) {
        recentApps = if (showRecentApps) {
            withContext(Dispatchers.IO) { RecentApps.query(context) }
        } else {
            emptyList()
        }
    }
    // Owned here rather than inside the sheet: the floating button has to be able to open the list
    // again, and that button only exists while the list is collapsed.
    var collapsed by remember { mutableStateOf(false) }
    var bubbleRemoved by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        // Behind everything: a stand-in for the home screen the sheet opens over. It is the app's own
        // mark — the same vector the launcher icon is built from — scattered across the brand
        // gradient, rather than the user's real wallpaper: reading that is restricted on recent
        // Android versions, and this only has to read as "there is a screen back there".
        val mark = painterResource(R.drawable.ic_not_app_mark)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .drawBehind { brandWallpaper(mark) }
        ) {
            val panelWidth = maxWidth
            val panelHeight = maxHeight
            DemoLabel()
            // Keyed on the mode: switching modes must show the new example expanded, not the state
            // the previous one was left in (a sheet collapsed to its handle, say).
            key(mode) {
                Box(Modifier.fillMaxSize()) {
                    when (mode) {
                        AppMode.LIST -> DemoSheet(
                            items = items,
                            startNumber = 1,
                            recentApps = recentApps,
                            collapsed = collapsed,
                            onCollapsedChange = { collapsed = it },
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        )

                        AppMode.DIRECT -> Column(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            DemoHint()
                            Spacer(Modifier.height(8.dp))
                            HeroIcon(items[0], 1, onShowShortcuts)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.mode_demo_direct_opens, labelOf(items[0], 1)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }

                        // The icon the mode would launch first, then the list of the rest under it.
                        // The list is drawn after the icon — the other way round the icon covered the
                        // rows, and that reads backwards: an app on top of the list is a screen with
                        // no list on it.
                        AppMode.MIX -> Column(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    DemoHint()
                                    Spacer(Modifier.height(6.dp))
                                    HeroIcon(items[0], 1, onShowShortcuts)
                                }
                            }
                            DemoSheet(
                                items = items.drop(1),
                                startNumber = 2,
                                recentApps = recentApps,
                                collapsed = collapsed,
                                onCollapsedChange = { collapsed = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MIX_SHEET_INSET)
                                    .heightIn(max = MIX_SHEET_MAX_HEIGHT)
                            )
                        }
                    }
                    // The floating button only exists in LIST and MIX (that is what the setting says)
                    // and only while the list is down, exactly as in the app.
                    if (showPeekBubble && mode != AppMode.DIRECT && collapsed && !bubbleRemoved) {
                        DemoPeekBubble(
                            panelWidth = panelWidth,
                            panelHeight = panelHeight,
                            onOpen = { collapsed = false },
                            onRemove = { bubbleRemoved = true }
                        )
                    }
                }
            }
        }
    }
}

/** Says out loud what the panel is, so it is never mistaken for live UI. */
@Composable
private fun BoxScope.DemoLabel() {
    Text(
        stringResource(R.string.mode_demo_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 12.dp, top = 10.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/**
 * The sheet's own chrome: same handle, same rows, and a downward swipe on the handle collapses it to
 * that handle alone — the sheet's own gesture, with the list left scrollable underneath it.
 *
 * The list stays in the layout even while collapsed: dropping it would shrink the sheet, and whatever
 * sits above it (Mix's icon) would slide down with the change — not what a sheet sliding away looks
 * like. The whole sheet is offset down instead, so only its handle strip is left in the panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoSheet(
    items: List<ShortcutSlot>,
    startNumber: Int,
    recentApps: List<RecentApp>,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { COLLAPSE_THRESHOLD.toPx() }
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    var handleStripPx by remember { mutableIntStateOf(0) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    val settledPx by animateFloatAsState(
        targetValue = if (collapsed) {
            (sheetHeightPx - handleStripPx).toFloat().coerceAtLeast(0f)
        } else {
            0f
        },
        animationSpec = tween(COLLAPSE_ANIM_MS),
        label = "demoSheet"
    )

    Surface(
        modifier = modifier
            .heightIn(max = SHEET_MAX_HEIGHT)
            .onSizeChanged { sheetHeightPx = it.height }
            .offset { IntOffset(0, (settledPx + dragPx).roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                // Downwards only: the sheet is already fully open, so an upward drag must do nothing.
                state = rememberDraggableState { delta -> dragPx = (dragPx + delta).coerceAtLeast(0f) },
                onDragStopped = {
                    val moved = dragPx
                    dragPx = 0f
                    onCollapsedChange(
                        if (collapsed) moved < -collapseThresholdPx else moved > collapseThresholdPx
                    )
                }
            ),
        shape = RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(top = DEMO_PADDING)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    // No fixed height here on purpose: Material3's own handle carries ~22dp of padding
                    // above and below the pill, and squeezing it into a fixed strip collapsed the pill
                    // to nothing — the handle was missing from the demo for exactly that reason.
                    .onSizeChanged { handleStripPx = it.height }
                    .clickable(enabled = collapsed) { onCollapsedChange(false) },
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle()
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recentApps.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        items(recentApps, key = { it.packageName }) { app ->
                            AppIcon(packageName = app.packageName, size = 32.dp)
                        }
                    }
                    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                // The sheet's own Configure gear, drawn but inert: this is a picture of the sheet, and
                // the screen around it is already where configuration happens.
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (recentApps.isNotEmpty()) HorizontalDivider()
            // fill = false is what keeps the sheet content-sized: the list takes its own height when
            // it is short, and the cap plus a scroll when it is not.
            LazyColumn(Modifier.weight(1f, fill = false)) {
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

/**
 * The floating button the list collapses into, at the size, colour and corner the real one has. It can
 * be dragged — onto ✕ to get rid of it, exactly like the app's drag-to-remove — or tapped to bring the
 * list back.
 */
@Composable
private fun BoxScope.DemoPeekBubble(
    panelWidth: Dp,
    panelHeight: Dp,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val bubbleRadius = PEEK_BUBBLE / 2
    // Where this bubble's centre is right now, and where the ✕ sits (bottom centre, as in the app).
    val centerX = panelWidth - PEEK_MARGIN - bubbleRadius + with(density) { offset.x.toDp() }
    val centerY = panelHeight - PEEK_MARGIN * 3 - bubbleRadius + with(density) { offset.y.toDp() }
    val overTrash = abs((centerX - panelWidth / 2).value) < TRASH_SNAP_DP &&
        centerY > panelHeight - TRASH_SNAP_DP * 2

    if (dragging) {
        // The same red target as the app's, shown only while a drag is actually happening.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(TRASH_SIZE)
                .background(if (overTrash) TRASH_ACTIVE else TRASH_IDLE, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_close_bubble),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .padding(end = PEEK_MARGIN, bottom = PEEK_MARGIN * 3)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(PEEK_BUBBLE)
            .background(PEEK_COLOR, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onOpen() } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        if (overTrash) onRemove() else offset = Offset.Zero
                    },
                    onDragCancel = {
                        dragging = false
                        offset = Offset.Zero
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        offset += drag
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_list_bubble),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Our own drawing of the launcher's long-press menu. The real one belongs to the launcher and cannot
 * be embedded in an app, so this is the same entries in the same order — app name, then the menu —
 * over a dimmed screen, and it exists only in this picker.
 *
 * The entries are the ones [com.noapp.container.shortcuts.ShortcutSync] would actually publish for
 * the mode that is on, because a menu showing four slots where the real one shows a single
 * "Configure" is worse than no example at all:
 *  - DIRECT keeps its first entry for Configure while "Use all shortcut slots" is off (that toggle
 *    needs the overlay permission, and without it the long-press menu is the only way back into
 *    Settings); the rest are the configured slots other than the one a plain tap already launches.
 *  - LIST publishes every configured slot, its own list having a Configure row inside it.
 *  - MIX skips slot 0 for the same reason DIRECT does.
 * With nothing configured the rows are numbered placeholders, and DIRECT previews its Configure
 * entry, since that is what appears as soon as the first slot is set.
 */
@Composable
fun ShortcutMenuOverlay(
    appName: String,
    mode: AppMode,
    slots: List<ShortcutSlot>,
    useAllSlotsInDirectMode: Boolean,
    onDismiss: () -> Unit
) {
    val configured = slots.filter { it.isConfigured }
    val empty = configured.isEmpty()
    val realRows = when {
        empty -> List(SHORTCUT_MENU_ROWS) { ShortcutSlot(id = it) }
        mode == AppMode.LIST -> configured
        // DIRECT and MIX both leave slot 0 out: a plain tap already launches it.
        else -> configured.filter { it.id != 0 }
    }
    val mainConfigured = empty || slots.getOrNull(0)?.isConfigured == true
    val showConfigure = mode == AppMode.DIRECT && !useAllSlotsInDirectMode && mainConfigured
    val entries: List<ShortcutSlot?> =
        (if (showConfigure) listOf<ShortcutSlot?>(null) else emptyList()) +
            realRows.take(SHORTCUT_MENU_ROWS).map { it as ShortcutSlot? }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            // Taps on the card itself are swallowed, so only an outside tap dismisses it — the same
            // as the menu it imitates. Its width is what its icons and labels need and no wider: the
            // real menu is a phone-sized card, and stretched across a tablet it looked nothing like it.
            modifier = Modifier
                .widthIn(max = MENU_MAX_WIDTH)
                .fillMaxWidth()
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
                entries.take(SHORTCUT_MENU_ROWS).forEach { slot ->
                    ListItem(
                        headlineContent = {
                            Text(
                                if (slot == null) {
                                    stringResource(R.string.shortcut_configure_label)
                                } else {
                                    labelOf(slot, slot.id + 1)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            DemoIcon(slot, if (slot == null) 0 else slot.id + 1, 40.dp)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

/**
 * The item that would be launched: holding it opens the shortcut menu, and tapping it shows what a tap
 * actually does — the item opens. Nothing is launched either way; the pop and the "Opened" pill are
 * the whole effect.
 */
@Composable
private fun HeroIcon(
    slot: ShortcutSlot,
    number: Int,
    onShowShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(opened) {
        if (opened) {
            delay(OPENED_MS)
            opened = false
        }
    }
    val pop by animateFloatAsState(
        targetValue = if (opened) 1.12f else 1f,
        animationSpec = tween(OPENED_MS.toInt() / 3),
        label = "heroPop"
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                }
                .pointerInput(slot) {
                    detectTapGestures(
                        onTap = { opened = true },
                        onLongPress = { onShowShortcuts() }
                    )
                }
        ) {
            DemoIcon(slot, number, HERO_ICON)
        }
        if (opened) {
            Text(
                "👍 " + stringResource(R.string.mode_demo_opened, labelOf(slot, number)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

/** Light on the brand backdrop, on a chip: the plain label was unreadable over it. */
@Composable
private fun DemoHint(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.mode_demo_long_press),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/**
 * A configured slot shows its real icon; an empty one shows its number, as the list itself does.
 * A null slot is the Configure entry, drawn with the same monogram
 * [com.noapp.container.shortcuts.ShortcutSync] paints on it.
 */
@Composable
private fun DemoIcon(slot: ShortcutSlot?, number: Int, size: Dp, modifier: Modifier = Modifier) {
    if (slot != null && slot.isConfigured) {
        SlotIcon(slot, size = size, modifier = modifier)
        return
    }
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(slot, number, sizePx) {
        val monogram = if (slot == null) {
            monogramBitmap(CONFIGURE_GLYPH, CONFIGURE_COLOR, sizePx)
        } else {
            monogramBitmap(
                text = number.toString(),
                colorHex = PLACEHOLDER_COLORS[(number - 1) % PLACEHOLDER_COLORS.size],
                sizePx = sizePx
            )
        }
        monogram.asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
}

@Composable
private fun labelOf(slot: ShortcutSlot, number: Int): String =
    slot.label.ifBlank { stringResource(R.string.common_item_n, number) }

/**
 * The brand backdrop: the app's own mark tiled diagonally at low alpha over the gradient its icon
 * uses, dimmed so the sheet stays the brightest thing in the panel. Deliberately not the user's real
 * wallpaper — reading that is restricted on recent Android versions — but it does have to look like a
 * screen, which a flat surface colour never did.
 */
private fun DrawScope.brandWallpaper(mark: Painter) {
    drawRect(
        Brush.linearGradient(
            colors = BRAND_GRADIENT,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
    )
    drawRect(Color.Black.copy(alpha = WALLPAPER_SCRIM))
    val tile = size.minDimension * 0.42f
    val step = tile * 1.5f
    rotate(degrees = -18f) {
        var y = -step
        while (y < size.height + step) {
            var x = -step
            while (x < size.width + step) {
                translate(left = x, top = y) {
                    with(mark) {
                        draw(size = Size(tile, tile), alpha = MARK_ALPHA)
                    }
                }
                x += step
            }
            y += step
        }
    }
}

private val DEMO_PADDING = 8.dp
private val HERO_ICON = 104.dp
private val COLLAPSE_THRESHOLD = 40.dp
private val SHEET_CORNER = 22.dp
private val MIX_SHEET_INSET = 24.dp

/** Mix shows the list under the icon, and never taller than this, so the icon stays visible. */
private val MIX_SHEET_MAX_HEIGHT = 220.dp

/** The most the sheet grows to before its list scrolls instead. */
private val SHEET_MAX_HEIGHT = 460.dp

/** Straight from QuickPickPeekOverlayService: the same button size, margin, colour and ✕ target. */
private val PEEK_BUBBLE = 48.dp
private val PEEK_MARGIN = 20.dp
private val TRASH_SIZE = 64.dp
private val TRASH_SNAP_DP = 72.dp
private val PEEK_COLOR = Color(0xCC3C4043)
private val TRASH_IDLE = Color(0xE6D32F2F)
private val TRASH_ACTIVE = Color(0xFFEF5350)

/** Sampled from the launcher icon: the same blue-to-violet its own tile is painted with. */
private val BRAND_GRADIENT = listOf(
    Color(0xFF062275),
    Color(0xFF0B6DCE),
    Color(0xFF7B5CE0),
    Color(0xFFA175F0)
)
private const val MARK_ALPHA = 0.16f
private const val WALLPAPER_SCRIM = 0.18f
private const val OPENED_MS = 1400L

/** The same glyph and colour ShortcutSync paints on the reserved Configure shortcut. */
private const val CONFIGURE_GLYPH = "\u2699"
private const val CONFIGURE_COLOR = "#3C4043"

/**
 * Numbered placeholders skip the palette's blues and lilac on purpose: the first of them is drawn
 * over a blue-to-violet backdrop, and the palette's first colour disappeared into it.
 */
private val PLACEHOLDER_COLORS = listOf(
    "#C0574C",
    "#D9A441",
    "#C97A3D",
    "#8B8D91"
)

private const val COLLAPSE_ANIM_MS = 220
private const val PLACEHOLDER_ROWS = 5
private const val SHORTCUT_MENU_ROWS = 4
private const val SCRIM_ALPHA = 0.32f

/** The real menu is a phone-sized card, not a full-width sheet — capped so a tablet gets that too. */
private val MENU_MAX_WIDTH = 300.dp
