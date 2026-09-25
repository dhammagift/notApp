package com.noapp.container.ui

import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.core.graphics.drawable.toBitmap
import com.noapp.container.R
import com.noapp.container.icon.AppIcon
import com.noapp.container.icon.SlotIcon
import com.noapp.container.icon.enabledLauncherComponent
import com.noapp.container.icon.monogramBitmap
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.recents.RecentApp
import com.noapp.container.recents.RecentApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    peekBubbleSize: Float,
    peekBubbleAlpha: Float,
    peekBubbleDockPeek: Float,
    peekBubbleReturns: Boolean,
    onOpenSetting: (SettingsSpot) -> Unit,
    narrowSheet: Boolean,
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
    // The share sheet is the same list with a "send to" line and no floating button at all (allowPeek
    // is off for it), and it exists in every mode but DIRECT. Kept across a mode switch: comparing the
    // two is the whole point of the switch.
    var shareCase by remember { mutableStateOf(false) }
    val canOverlay = AndroidSettings.canDrawOverlays(context)

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
                .clipToBounds()
                .drawBehind { brandWallpaper(mark) }
        ) {
            val panelWidth = maxWidth
            val panelHeight = maxHeight
            DemoLabel()
            DemoCaseSwitch(
                    shareCase = shareCase,
                    onChange = { shareCase = it },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            if (shareCase) {
                // Without this the share variant is just an oddly-annotated list; with it, it says
                // where the screen comes from, which is the thing a user has never seen.
                Text(
                    stringResource(R.string.mode_demo_share_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 46.dp, start = 24.dp, end = 24.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            // Keyed on the mode: switching modes must show the new example expanded, not the state
            // the previous one was left in (a sheet collapsed to its handle, say).
            // "Bring the button back after ✕": on, the button returns the next time the list is
            // opened here (a mode switch, in the demo); off, it stays gone for the rest of this
            // picker. The app persists that across openings, which a demo must not do — it never
            // writes the user's settings.
            var bubbleGoneForSession by remember { mutableStateOf(false) }
            key(mode) {
                // Declared inside the key, so every mode starts from the same place: a freshly opened
                // list and its button back. Kept outside, the state survived a mode switch and the
                // button looked like the thing controlling how the next mode opened.
                var collapsed by remember { mutableStateOf(false) }
                var bubbleRemoved by remember { mutableStateOf(false) }
                val bubbleGone = bubbleRemoved || (!peekBubbleReturns && bubbleGoneForSession)
                Box(Modifier.fillMaxSize()) {
                    when (mode) {
                        AppMode.LIST -> DemoSheet(
                            items = items,
                            startNumber = 1,
                            recentApps = recentApps,
                            recentsOff = !showRecentApps,
                            onEnableRecents = { onOpenSetting(SettingsSpot.RECENT_APPS) },
                            sharedText = if (shareCase) stringResource(R.string.mode_demo_share_text) else null,
                            collapsed = collapsed,
                            onCollapsedChange = { collapsed = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .widthIn(max = if (narrowSheet) SHEET_MAX_WIDTH else Dp.Unspecified)
                                .fillMaxWidth()
                        )

                        AppMode.DIRECT -> {
                            // The gear the app flashes over whatever Direct launched — it needs no
                            // toggle, and with "Use all shortcut slots" on it is the only way back.
                            var gearVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(gearVisible) {
                                if (gearVisible) {
                                    delay(GEAR_DISPLAY_MS)
                                    gearVisible = false
                                }
                            }
                            Box(Modifier.fillMaxSize()) {
                                Column(
                                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    DemoHint()
                                    Spacer(Modifier.height(8.dp))
                                    HeroIcon(items[0], onShowShortcuts, onOpen = { gearVisible = true })
                                }
                                if (gearVisible) DemoGearChip(Modifier.align(Alignment.TopEnd))
                                if (!useAllSlotsInDirectMode) {
                                    // Off, the mode keeps one slot for Configure; on, that slot goes to
                                    // a real item and the gear above is the way back. Saying so here is
                                    // the difference between "this is what Direct is" and "this is what
                                    // it could be".
                                    DemoOffHint(
                                        text = stringResource(R.string.settings_use_all_slots),
                                        onClick = { onOpenSetting(SettingsSpot.USE_ALL_SLOTS) },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(PEEK_MARGIN)
                                    )
                                }
                            }
                        }

                        // The icon the mode would launch first, then the list of the rest under it.
                        // The list is drawn after the icon — the other way round the icon covered the
                        // rows, and that reads backwards: an app on top of the list is a screen with
                        // no list on it.
                        AppMode.MIX -> Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    DemoHint()
                                    Spacer(Modifier.height(6.dp))
                                    HeroIcon(items[0], onShowShortcuts)
                                }
                            }
                            DemoSheet(
                                items = items.drop(1),
                                startNumber = 2,
                                recentApps = recentApps,
                                recentsOff = !showRecentApps,
                                onEnableRecents = { onOpenSetting(SettingsSpot.RECENT_APPS) },
                                sharedText = if (shareCase) stringResource(R.string.mode_demo_share_text) else null,
                                collapsed = collapsed,
                                onCollapsedChange = { collapsed = it },
                                modifier = Modifier
                                    .widthIn(max = if (narrowSheet) SHEET_MAX_WIDTH else Dp.Unspecified)
                                    .fillMaxWidth()
                                    .padding(horizontal = MIX_SHEET_INSET)
                                    // Everything the panel has above the icon block: a fixed cap here
                                    // made Mix's list stop halfway while List's ran the full height.
                                    .heightIn(
                                        max = (panelHeight - MIX_ICON_SPACE)
                                            .coerceAtLeast(MIX_SHEET_MIN_HEIGHT)
                                    )
                            )
                        }
                    }
                    // The floating button only exists in LIST and MIX (that is what the setting says)
                    // and only while the list is down, exactly as in the app.
                    // The share sheet never peeks (allowPeek is off for it), and without the overlay
                    // permission the app falls back to a button in its own window — both cases are
                    // drawn here rather than described.
                    val showButton = showPeekBubble && mode != AppMode.DIRECT && collapsed && !bubbleGone && !shareCase
                    if (!showPeekBubble && mode != AppMode.DIRECT && !shareCase && collapsed) {
                        // The setting is off: the button is shown faded where it would sit, with the
                        // way to turn it on — seeing it is what tells the user the option exists.
                        DemoOffHint(
                            text = stringResource(R.string.settings_show_peek_bubble),
                            onClick = { onOpenSetting(SettingsSpot.FLOATING_BUTTON) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(PEEK_MARGIN)
                        )
                    }
                    if (showButton && !canOverlay) {
                        DemoPeekPill(onOpen = { collapsed = false })
                    } else if (showButton) {
                        DemoPeekBubble(
                            panelWidth = panelWidth,
                            panelHeight = panelHeight,
                            size = PEEK_BUBBLE * peekBubbleSize,
                            alpha = peekBubbleAlpha,
                            dockPeek = peekBubbleDockPeek,
                            onOpen = { collapsed = false },
                            onRemove = {
                                bubbleRemoved = true
                                bubbleGoneForSession = true
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * The two shapes the list has in the app: the shortcut list, and the share sheet — same sheet, plus a
 * "send to" line and no floating button, because sharing never peeks.
 */
@Composable
private fun BoxScope.DemoCaseSwitch(
    shareCase: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.padding(end = 12.dp, top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DemoCaseChip(stringResource(R.string.mode_demo_normal), selected = !shareCase) { onChange(false) }
        DemoCaseChip(stringResource(R.string.mode_demo_share), selected = shareCase) { onChange(true) }
    }
}

@Composable
private fun DemoCaseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** A feature that exists but is switched off: named, faded, and one tap from its own setting. */
@Composable
private fun DemoOffHint(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Box(
            Modifier
                .size(PEEK_BUBBLE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.mode_demo_enable_in_settings) + ": " + text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * What the app falls back to when "draw over other apps" was never granted: the same round button, but
 * inside its own window (QuickPickSheet's PeekPill), so it cannot be dragged or parked on an edge and
 * it vanishes with the app. Drawn here with the note that explains the difference.
 */
@Composable
private fun BoxScope.DemoPeekPill(onOpen: () -> Unit) {
    Column(
        Modifier.align(Alignment.BottomEnd).padding(PEEK_MARGIN),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            stringResource(R.string.mode_demo_pill_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .size(PEEK_BUBBLE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onOpen() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.quick_pick_reopen_desc),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
    recentsOff: Boolean,
    onEnableRecents: () -> Unit,
    sharedText: String?,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // The sheet's own numbers, so the demo reacts to a swipe exactly as the sheet does.
    val collapseThresholdPx = with(density) { SHEET_DISMISS_THRESHOLD.toPx() }
    val dismissVelocityPx = with(density) { SHEET_DISMISS_VELOCITY.toPx() }
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    var handleStripPx by remember { mutableIntStateOf(0) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val hiddenPx = (sheetHeightPx - handleStripPx).toFloat().coerceAtLeast(0f)
    // Same shape as the real sheet's offset: an Animatable the drag offsets live, so the release can
    // hand its velocity to the settling animation.
    val offsetY = remember { Animatable(0f) }
    LaunchedEffect(collapsed, hiddenPx) {
        // The floating button opens the list, and that has to move the sheet too.
        if (!dragging) {
            offsetY.animateTo(if (collapsed) hiddenPx else 0f, SHEET_SPRING)
        }
    }

    Surface(
        modifier = modifier
            .heightIn(max = SHEET_MAX_HEIGHT)
            .onSizeChanged { sheetHeightPx = it.height }
            .offset { IntOffset(0, (offsetY.value + dragPx).roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                // Downwards only: the sheet is already fully open, so an upward drag must do nothing.
                state = rememberDraggableState { delta ->
                    dragging = true
                    dragPx = (dragPx + delta).coerceAtLeast(0f)
                },
                onDragStopped = { velocity ->
                    dragging = false
                    val moved = dragPx
                    // No seam between dragging and animating: the base is snapped to where the sheet
                    // actually is, and the finger's velocity carries into the settle.
                    val base = (offsetY.value + moved).coerceIn(0f, hiddenPx)
                    dragPx = 0f
                    val flung = if (collapsed) velocity < -dismissVelocityPx else velocity > dismissVelocityPx
                    val shouldCollapse = if (collapsed) {
                        moved < -collapseThresholdPx || flung
                    } else {
                        moved > collapseThresholdPx || flung
                    }
                    scope.launch {
                        offsetY.snapTo(base)
                        onCollapsedChange(shouldCollapse)
                        offsetY.animateTo(if (shouldCollapse) hiddenPx else 0f, SHEET_SPRING, initialVelocity = velocity)
                    }
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
                } else if (recentsOff) {
                    // Off, not unavailable: the strip is drawn as it would be, greyed, next to the way
                    // to turn it on.
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                                        CircleShape
                                    )
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.mode_demo_enable_in_settings) + ": " +
                                stringResource(R.string.settings_show_recent_apps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onEnableRecents() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
            if (sharedText != null) {
                Text(
                    stringResource(R.string.quick_pick_send_to, sharedText),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
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
 * The floating button the list collapses into: the real one's size, colour, opacity and corner, from
 * the same settings that scale, fade and dock it in the app. It can be dragged anywhere in the panel,
 * tapped to bring the list back, or removed by dropping it on the ✕ (which the app's drag-to-remove
 * does too). Letting go near a side edge tucks it into that edge the way the app does: mostly off the
 * panel, scaled down and faded, with `peekBubbleDockPeek` deciding how much of it stays visible.
 */
@Composable
private fun BoxScope.DemoPeekBubble(
    panelWidth: Dp,
    panelHeight: Dp,
    size: Dp,
    alpha: Float,
    dockPeek: Float,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val sizePx = with(density) { size.toPx() }
    val panelWidthPx = with(density) { panelWidth.toPx() }
    val panelHeightPx = with(density) { panelHeight.toPx() }
    val marginPx = with(density) { PEEK_MARGIN.toPx() }
    val dockZonePx = with(density) { DOCK_ZONE.toPx() }
    val dockPeekPx = (sizePx * dockPeek).coerceAtLeast(1f)
    // Where the app parks it: bottom-end, a little clear of the very edge.
    val restX = panelWidthPx - sizePx - marginPx
    val restY = panelHeightPx - sizePx - marginPx * 3
    // Fully off to one side, with dockPeekPx of it still showing.
    fun dockedX(side: Int) = if (side < 0) dockPeekPx - sizePx else panelWidthPx - dockPeekPx

    val position = remember(panelWidthPx, panelHeightPx) {
        Animatable(Offset(restX, restY), Offset.VectorConverter)
    }
    var dockedSide by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var fling by remember { mutableStateOf(Offset.Zero) }
    var lastSampleAt by remember { mutableLongStateOf(0L) }
    val trashCenterX = panelWidthPx / 2
    val trashCenterY = panelHeightPx - with(density) { TRASH_BOTTOM_MARGIN.toPx() } -
        with(density) { TRASH_SIZE.toPx() } / 2
    val snapPx = with(density) { TRASH_SNAP.toPx() }

    // Evaluated from the position passed in, because a lambda handed to detectDragGestures keeps the
    // values it captured when it was created — reading a composed value there meant a drop always saw
    // "not over the ✕" and the button could never be thrown away.
    fun overTrashAt(position: Offset): Boolean {
        val cx = position.x + sizePx / 2
        val cy = position.y + sizePx / 2
        return abs(cx - trashCenterX) < snapPx && abs(cy - trashCenterY) < snapPx
    }
    fun clamped(position: Offset) = Offset(
        position.x.coerceIn(0f, panelWidthPx - sizePx),
        position.y.coerceIn(0f, panelHeightPx - sizePx)
    )
    val overTrash = overTrashAt(position.value)

    // Shown only while the button is being dragged, exactly like the app's ✕ target.
    if (dragging) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TRASH_BOTTOM_MARGIN)
                .size(TRASH_SIZE)
                .background(if (overTrash) TRASH_ACTIVE else TRASH_IDLE, CircleShape)
                .pointerInput(Unit) { detectTapGestures { onRemove() } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_close_bubble),
                contentDescription = stringResource(R.string.settings_peek_bubble_returns),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    val docked by animateFloatAsState(if (dockedSide != 0) 1f else 0f, tween(DOCK_ANIM_MS.toInt()), label = "docked")
    Box(
        Modifier
            // TopStart, because this position is absolute inside the panel: aligning to the bottom as
            // well pushed the button a whole panel-height off the bottom edge, which is why it was
            // invisible while its ✕ was still on screen.
            .align(Alignment.TopStart)
            .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
            .size(size)
            .graphicsLayer {
                this.alpha = alpha * (1f - docked * (1f - DOCK_ALPHA_FACTOR))
                val s = 1f - docked * (1f - DOCK_SCALE)
                scaleX = s
                scaleY = s
            }
            .background(PEEK_COLOR, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onOpen() } }
            .pointerInput(sizePx) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        // Pulling it out of the edge brings it back to full size, like the app.
                        dockedSide = 0
                    },
                    onDragEnd = {
                        dragging = false
                        if (overTrashAt(position.value)) {
                            onRemove()
                        } else {
                            scope.launch {
                                // The app's own momentum: a fling carries the button on, decelerating,
                                // and only where it comes to rest decides the docking — see
                                // QuickPickPeekOverlayService's settle().
                                if (fling.getDistance() > FLING_MIN_VELOCITY) {
                                    position.animateDecay(fling, exponentialDecay())
                                }
                                val settled = clamped(position.value)
                                position.snapTo(settled)
                                val side = when {
                                    settled.x <= dockZonePx -> -1
                                    settled.x >= panelWidthPx - sizePx - dockZonePx -> 1
                                    else -> 0
                                }
                                dockedSide = side
                                if (side != 0) {
                                    position.animateTo(
                                        Offset(dockedX(side), settled.y),
                                        tween(DOCK_ANIM_MS.toInt())
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = { dragging = false },
                    onDrag = { change, delta ->
                        change.consume()
                        // Pixels per second from the last two samples: enough to hand the fling the
                        // speed the finger actually left behind.
                        val dt = (change.uptimeMillis - lastSampleAt).coerceAtLeast(1L)
                        fling = delta * (1000f / dt)
                        lastSampleAt = change.uptimeMillis
                        scope.launch { position.snapTo(clamped(position.value + delta)) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_list_bubble),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.46f)
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
 * The launcher icon the user actually has — whichever variant is enabled in Settings, not a stand-in —
 * because that is the icon on their home screen and the one the long-press menu belongs to. Holding it
 * opens that menu; tapping it shows what pressing it does instead, as a burst of light from the icon
 * (nothing is launched, and nothing pops up to say which item it was — the line underneath already
 * names it). The item's own icon is there too, so "which app" is answered by the app itself.
 */
@Composable
private fun HeroIcon(
    target: ShortcutSlot,
    onShowShortcuts: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val burst = remember { Animatable(1f) }
    // The kick at the start of the burst, so the icon itself reacts to the tap.
    val pop = 1f + 0.12f * (1f - burst.value).coerceIn(0f, 1f)
    // While it plays, the line under the icon says what the tap did: a firework on its own left it
    // open to question what had just opened. The animation is finite, so this always resets itself —
    // and the whole thing is keyed by mode, so switching modes starts clean.
    val opened = burst.value < 1f

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(HERO_ICON * 2.8f), contentAlignment = Alignment.Center) {
            if (burst.value < 1f) {
                Canvas(Modifier.matchParentSize()) { firework(burst.value) }
            }
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = pop
                        scaleY = pop
                    }
                    .pointerInput(target) {
                        detectTapGestures(
                            onTap = {
                                onOpen()
                                scope.launch {
                                    burst.snapTo(0f)
                                    burst.animateTo(1f, tween(FIREWORK_MS, easing = LinearEasing))
                                }
                            },
                            onLongPress = { onShowShortcuts() }
                        )
                    }
            ) {
                LauncherIcon(HERO_ICON)
            }
        }
        Row(
            Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (opened) {
                Text(
                    "\u2713",
                    style = MaterialTheme.typography.titleMedium,
                    color = OPENED_CHECK
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.mode_demo_opened, labelOf(target, target.id + 1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            } else {
                DemoIcon(target, target.id + 1, 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.mode_demo_direct_opens, labelOf(target, target.id + 1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}

/**
 * The Configure gear Direct flashes over the app it just launched: a 32dp dark scrim with the same
 * 24dp glyph, 12dp in from the top end, gone again after the same 2.5s. Drawn, not tappable — in the
 * demo there is nothing underneath it to come back from.
 */
@Composable
private fun DemoGearChip(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(GEAR_MARGIN)
            .size(GEAR_SCRIM)
            .background(GEAR_SCRIM_COLOR, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_settings_gear),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(GEAR_ICON)
        )
    }
}

/** Sparks thrown outwards from the icon: the demo's version of "this is what a tap does". */
private fun DrawScope.firework(progress: Float) {
    val center = size.center
    val reach = size.minDimension / 2f
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val dot = 5.dp.toPx() * (1f - progress * 0.45f)
    val sparks = FIREWORK_COLORS.size * 5
    for (index in 0 until sparks) {
        val angle = (index * 2.0 * PI / sparks).toFloat()
        // Two shells: an inner one that lands early and an outer one that carries further, which is
        // what makes it read as a burst rather than dots on a circle.
        val shell = if (index % 2 == 0) 0.62f else 1f
        val distance = reach * shell * (0.25f + 0.75f * progress)
        drawCircle(
            color = FIREWORK_COLORS[index % FIREWORK_COLORS.size],
            radius = dot,
            center = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance),
            alpha = alpha
        )
    }
    // The thin ring left behind by the outer shell.
    drawCircle(
        color = FIREWORK_COLORS.first(),
        radius = reach * (0.3f + 0.7f * progress),
        center = center,
        style = Stroke(width = 2.dp.toPx()),
        alpha = alpha * 0.5f
    )
}

/** The enabled launcher icon, straight from PackageManager — the one on the home screen. */
@Composable
private fun LauncherIcon(size: Dp) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(sizePx) {
        runCatching {
            context.packageManager.getActivityIcon(enabledLauncherComponent(context)).toBitmap(sizePx, sizePx)
        }.getOrNull()?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
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
/** The sheet's own dismissal numbers (QuickPickSheet): same threshold, same fling speed. */
private val SHEET_DISMISS_THRESHOLD = 100.dp
private val SHEET_DISMISS_VELOCITY = 1000.dp
private val SHEET_CORNER = 22.dp
private val MIX_SHEET_INSET = 24.dp

/** Room Mix keeps for the icon block above its list, so the list can still run to the bottom. */
private val MIX_ICON_SPACE = 190.dp
private val MIX_SHEET_MIN_HEIGHT = 120.dp

/** The most the sheet grows to before its list scrolls instead. */
private val SHEET_MAX_HEIGHT = 460.dp

/** A sheet is a phone-shaped thing: on a landscape screen it keeps that width, centred. */
private val SHEET_MAX_WIDTH = 420.dp

/** Straight from QuickPickPeekOverlayService: the same button size, margin, colour and ✕ target. */
private val PEEK_BUBBLE = 48.dp
private val PEEK_MARGIN = 20.dp
private val TRASH_SIZE = 64.dp
private val TRASH_BOTTOM_MARGIN = 24.dp
private val TRASH_SNAP = 72.dp

/** Straight from QuickPickPeekOverlayService: the same edge zone, scale and fade when docked. */
private val DOCK_ZONE = 28.dp
private const val DOCK_ANIM_MS = 300L
private const val FLING_MIN_VELOCITY = 50f
private const val DOCK_SCALE = 0.9f
private const val DOCK_ALPHA_FACTOR = 0.6f
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
/** Bright sparks, because on this backdrop the brand's own blues would not read as a burst. */
private val FIREWORK_COLORS = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFFFD166),
    Color(0xFFEF476F),
    Color(0xFF06D6A0),
    Color(0xFFA175F0)
)
private const val FIREWORK_MS = 900

/** Straight from GearOverlayService: same scrim, glyph, inset and display time. */
private val GEAR_SCRIM = 32.dp
private val GEAR_ICON = 24.dp
private val GEAR_MARGIN = 12.dp
private val GEAR_SCRIM_COLOR = Color(0xAA000000)
private const val GEAR_DISPLAY_MS = 2500L
private val OPENED_CHECK = Color(0xFF06D6A0)

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

/** The same settle the real sheet uses: a spring, so the drag's velocity counts. */
private val SHEET_SPRING = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
private const val PLACEHOLDER_ROWS = 5
private const val SHORTCUT_MENU_ROWS = 4
private const val SCRIM_ALPHA = 0.32f

/** The real menu is a phone-sized card, not a full-width sheet — capped so a tablet gets that too. */
private val MENU_MAX_WIDTH = 300.dp
