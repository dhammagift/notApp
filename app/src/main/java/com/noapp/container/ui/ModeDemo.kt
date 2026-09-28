package com.noapp.container.ui

import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.TransformOrigin
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
    onOpenSetting: (SettingsSpot?) -> Unit,
    narrowSheet: Boolean,
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
            DemoTopStrip(
                shareCase = shareCase,
                onChange = { shareCase = it },
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
            )
            // Every hint lives in this one block, directly under the two controls: floating them next
            // to whatever they were about meant a different mess on every screen size.
            DemoHints(
                mode = mode,
                shareCase = shareCase,
                recentsOff = !showRecentApps,
                floatingOff = !showPeekBubble,
                allSlotsOff = !useAllSlotsInDirectMode,
                needsOverlay = showPeekBubble && !canOverlay,
                onOpenSetting = onOpenSetting,
                maxWidth = panelWidth * 0.56f,
                modifier = Modifier.align(Alignment.TopStart)
            )
            // Keyed on the mode: switching modes must show the new example expanded, not the state
            // the previous one was left in (a sheet collapsed to its handle, say).
            // "Bring the button back after ✕": on, the button returns the next time the list is
            // opened here (a mode switch, in the demo); off, it stays gone for the rest of this
            // picker. The app persists that across openings, which a demo must not do — it never
            // writes the user's settings.
            var bubbleGoneForSession by remember { mutableStateOf(false) }
            key(mode) {
                // The launcher's long-press menu, drawn inside this panel and anchored to the icon (see
                // ShortcutMenuOverlay). Inside the key, so a mode switch closes it.
                var menuShown by remember { mutableStateOf(false) }
                // Declared inside the key, so every mode starts from the same place: a freshly opened
                // list and its button back. Kept outside, the state survived a mode switch and the
                // button looked like the thing controlling how the next mode opened.
                var collapsed by remember { mutableStateOf(false) }
                var bubbleRemoved by remember { mutableStateOf(false) }
                // Bumped by a tap on the icon in List and Mix: the sheet dips and springs back, so the tap
                // reads as "that is what opens the list" instead of doing nothing visible.
                var listPulse by remember { mutableIntStateOf(0) }
                val bubbleGone = bubbleRemoved || (!peekBubbleReturns && bubbleGoneForSession)
                Box(Modifier.fillMaxSize()) {
                    // The icon that sits on the home screen, in all three modes, at the size it is
                    // there. Compact and out of the way in the corner: at hero size it collided with
                    // whatever the panel had to say.
                    DemoIconBlock(
                        mode = mode,
                        target = items[0],
                        onShowShortcuts = { menuShown = true },
                        onTap = {
                            // List and Mix both open the list on a tap: Mix launches the first item AND
                            // shows the list over it (a collapsed list comes back, an open one dips).
                            if (mode == AppMode.LIST || mode == AppMode.MIX) {
                                collapsed = false
                                listPulse++
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 62.dp, end = 12.dp)
                    )
                    when (mode) {
                        AppMode.LIST -> DemoSheet(
                            items = items,
                            startNumber = 1,
                            recentApps = recentApps,
                            recentsOff = !showRecentApps,
                            onOpenSettings = { onOpenSetting(null) },
                            sharedText = if (shareCase) stringResource(R.string.mode_demo_share_text) else null,
                            collapsed = collapsed,
                            onCollapsedChange = { collapsed = it },
                            pulse = listPulse,
                            heightCap = minOf(
                                panelHeight * SHEET_PANEL_FRACTION,
                                // ...but never over the icon and its caption: a shorter panel (the
                                // picker's own, with three cards above it) used to push the sheet up
                                // over the caption's last line, which read as the caption being cut.
                                (panelHeight - MIX_ICON_SPACE).coerceAtLeast(MIX_SHEET_MIN_HEIGHT)
                            ),
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
                                AnimatedVisibility(
                                    visible = gearVisible,
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    enter = fadeIn(tween(OverlayMotion.GEAR_IN_MS.toInt())) +
                                        scaleIn(tween(OverlayMotion.GEAR_IN_MS.toInt()), initialScale = 0.6f),
                                    exit = fadeOut(tween(OverlayMotion.GEAR_OUT_MS.toInt())) +
                                        scaleOut(tween(OverlayMotion.GEAR_OUT_MS.toInt()), targetScale = 0.6f)
                                ) {
                                    DemoGearChip(
                                        onClick = { onOpenSetting(null) },
                                        // The real overlay turns as it grows in.
                                        modifier = Modifier.spinInOnAppear(fromDegrees = -120f)
                                    )
                                }
                                if (!useAllSlotsInDirectMode) {
                                    // Off, the mode keeps one slot for Configure; on, that slot goes to
                                    // a real item and the gear above is the way back. Saying so here is
                                    // the difference between "this is what Direct is" and "this is what
                                    // it could be".
                                    DemoGhostBubble(
                                        onClick = { onOpenSetting(SettingsSpot.USE_ALL_SLOTS) },
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(PEEK_MARGIN)
                                    )
                                }
                            }
                        }

                        // Mix: the same list, of everything after the item a tap launches.
                        AppMode.MIX -> Box(Modifier.fillMaxSize()) {
                            DemoSheet(
                                items = items.drop(1),
                                startNumber = 2,
                                recentApps = recentApps,
                                recentsOff = !showRecentApps,
                                onOpenSettings = { onOpenSetting(null) },
                                sharedText = if (shareCase) stringResource(R.string.mode_demo_share_text) else null,
                                collapsed = collapsed,
                                onCollapsedChange = { collapsed = it },
                                pulse = listPulse,
                                heightCap = minOf(
                                    panelHeight * SHEET_PANEL_FRACTION,
                                    (panelHeight - MIX_ICON_SPACE).coerceAtLeast(MIX_SHEET_MIN_HEIGHT)
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .widthIn(max = if (narrowSheet) SHEET_MAX_WIDTH else Dp.Unspecified)
                                    .fillMaxWidth()
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
                        DemoGhostBubble(
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
                    if (menuShown) {
                        ShortcutMenuOverlay(
                            appName = stringResource(R.string.app_name),
                            mode = mode,
                            slots = slots,
                            useAllSlotsInDirectMode = useAllSlotsInDirectMode,
                            onOpenSettings = {
                                menuShown = false
                                onOpenSetting(null)
                            },
                            onDismiss = { menuShown = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/** One line of the demo's hint block: informational, or a way into the setting behind it. */
private data class DemoHintLine(val text: String, val spot: SettingsSpot? = null)

/**
 * All of the demo's hints, in one block under the controls. They used to float next to whatever they
 * were about — over the icon, under the button, beside the list — which meant a different mess on
 * every screen size, and on a short panel the last one fell off the bottom edge.
 */
@Composable
private fun BoxScope.DemoHints(
    mode: AppMode,
    shareCase: Boolean,
    recentsOff: Boolean,
    floatingOff: Boolean,
    allSlotsOff: Boolean,
    needsOverlay: Boolean,
    onOpenSetting: (SettingsSpot?) -> Unit,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val lines = buildList {
        if (shareCase) {
            add(DemoHintLine(stringResource(R.string.mode_demo_share_hint)))
        }
        if (recentsOff) {
            add(
                DemoHintLine(
                    stringResource(R.string.mode_demo_enable_in_settings) + ": " +
                        stringResource(R.string.mode_demo_short_recents),
                    SettingsSpot.RECENT_APPS
                )
            )
        }
        if (floatingOff && mode != AppMode.DIRECT) {
            add(
                DemoHintLine(
                    stringResource(R.string.mode_demo_enable_in_settings) + ": " +
                        stringResource(R.string.mode_demo_short_float),
                    SettingsSpot.FLOATING_BUTTON
                )
            )
        }
        if (allSlotsOff && mode == AppMode.DIRECT) {
            add(
                DemoHintLine(
                    stringResource(R.string.mode_demo_enable_in_settings) + ": " +
                        stringResource(R.string.mode_demo_short_all_slots),
                    SettingsSpot.USE_ALL_SLOTS
                )
            )
        }
        if (needsOverlay) {
            add(DemoHintLine(stringResource(R.string.mode_demo_pill_hint), SettingsSpot.FLOATING_BUTTON))
        }
    }
    Column(
        modifier
            .padding(start = 12.dp, top = 46.dp, end = 12.dp)
            // The icon block sits in the other corner of this same band, so the block never takes more
            // than a little over half the width.
            .widthIn(max = maxWidth),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { line ->
            Text(
                line.text,
                style = MaterialTheme.typography.labelSmall,
                color = if (line.spot != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (line.spot != null) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        } else {
                            Color.Black.copy(alpha = 0.45f)
                        }
                    )
                    .then(
                        if (line.spot != null) Modifier.clickable { onOpenSetting(line.spot) } else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * The two shapes the list has in the app: the shortcut list, and the share sheet — same sheet, plus a
 * "send to" line and no floating button, because sharing never peeks.
 */
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

/**
 * Where a switched-off floating button would sit, faded. Its name and its way into Settings live in
 * the hint block at the top of the panel: under the button there was no room for them, and on a short
 * panel they fell off the bottom edge.
 */
@Composable
private fun DemoGhostBubble(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(PEEK_BUBBLE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Menu,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
        Box(
            Modifier
                .popInOnAppear()
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

/**
 * The panel's own label and the two cases it can show, laid out as one row. As two separate corners
 * of the panel the label ran underneath the chips — on the picker's narrower panel a longer label
 * ("Демо — попробуйте меня") lost its last word to them, and the chips drew over it.
 */
@Composable
private fun DemoTopStrip(shareCase: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            stringResource(R.string.mode_demo_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            // Weight, not a fixed size: the chips are measured first and the label gets what is left,
            // so a long one wraps instead of sliding under them. SpaceBetween keeps the chips at the
            // end however wide the label turned out.
            modifier = Modifier
                .weight(1f, fill = false)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DemoCaseChip(stringResource(R.string.mode_demo_normal), selected = !shareCase) { onChange(false) }
            DemoCaseChip(stringResource(R.string.mode_demo_share), selected = shareCase) { onChange(true) }
        }
    }
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
    onOpenSettings: () -> Unit,
    sharedText: String?,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pulse: Int = 0,
    heightCap: Dp = SHEET_MAX_HEIGHT
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
    // Set while the drag's own animation runs, so the effect below does not start a second one on the
    // same offset — two springs on one value is what made Mix's sheet judder.
    var settlingFromDrag by remember { mutableStateOf(false) }
    val hiddenPx = (sheetHeightPx - handleStripPx).toFloat().coerceAtLeast(0f)
    // Same shape as the real sheet's offset: an Animatable the drag offsets live, so the release can
    // hand its velocity to the settling animation.
    val offsetY = remember { Animatable(0f) }
    // The sheet arrives the way the real one does (SheetMotion): it rises from just under its own
    // bottom edge on the same spring, and the gear and the rows start once it is on its way.
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(sheetHeightPx > 0) {
        if (sheetHeightPx == 0 || arrived) return@LaunchedEffect
        offsetY.snapTo(sheetHeightPx.toFloat())
        arrived = true
        offsetY.animateTo(if (collapsed) hiddenPx else 0f, SheetMotion.enterSpring)
    }
    LaunchedEffect(collapsed) {
        // The floating button opens the list, and that has to move the sheet too.
        if (!dragging && !settlingFromDrag) {
            offsetY.animateTo(if (collapsed) hiddenPx else 0f, SHEET_SPRING)
        }
    }
    LaunchedEffect(pulse) {
        if (pulse > 0 && !dragging && !settlingFromDrag) {
            val bump = with(density) { 26.dp.toPx() }
            offsetY.animateTo(offsetY.value + bump, tween(SHEET_BUMP_MS))
            offsetY.animateTo(if (collapsed) hiddenPx else 0f, SHEET_SPRING)
        }
    }

    Surface(
        modifier = modifier
            .heightIn(max = heightCap)
            .onSizeChanged { sheetHeightPx = it.height }
            // Invisible for the frame before it has been measured, so it never shows at rest first.
            .graphicsLayer { alpha = if (arrived) 1f else 0f }
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
                        settlingFromDrag = true
                        onCollapsedChange(shouldCollapse)
                        offsetY.animateTo(
                            if (shouldCollapse) hiddenPx else 0f,
                            SHEET_SPRING,
                            initialVelocity = velocity
                        )
                        settlingFromDrag = false
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
                        Spacer(Modifier.weight(1f))
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                // The sheet's own Configure gear, and it works: the same trip to Settings the real
                // one makes from here.
                Box(
                    Modifier.size(48.dp).clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.sheetGearMotion(arrived)
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
                        leadingContent = { DemoIcon(slot, startNumber + index, 32.dp) },
                        modifier = Modifier.sheetRowMotion(index, arrived)
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
    AnimatedVisibility(
        visible = dragging,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TRASH_BOTTOM_MARGIN),
        enter = fadeIn(tween(OverlayMotion.TRASH_IN_MS.toInt())) + scaleIn(tween(OverlayMotion.TRASH_IN_MS.toInt()), initialScale = 0.5f),
        exit = fadeOut(tween(OverlayMotion.TRASH_OUT_MS.toInt())) + scaleOut(tween(OverlayMotion.TRASH_OUT_MS.toInt()), targetScale = 0.5f)
    ) {
        Box(
            Modifier
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
            .popInOnAppear()
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
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
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

    // Grows out of the icon's corner, the way a launcher's menu opens from the icon it belongs to.
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }

    // Fills the demo panel only (the caller sizes it): dimming and dismissing stay inside the picture of
    // a home screen, which is what the menu is a picture of.
    BoxWithConstraints(
        modifier
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        // Under the icon when the panel has room for it, as launchers do; beside it (on the icon's left)
        // when the panel is too short, so the card is never cut off by the panel's edge.
        val cardHeight = MENU_HEADER_HEIGHT + MENU_ROW_HEIGHT * entries.size + MENU_PADDING * 2
        val below = maxHeight >= MENU_ICON_BOTTOM + cardHeight + MENU_EDGE
        Surface(
            // Taps on the card itself are swallowed, so only an outside tap dismisses it — the same as the
            // menu it imitates.
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (below) MENU_ICON_BOTTOM else MENU_EDGE,
                    end = if (below) MENU_EDGE + 12.dp else HOME_ICON + MENU_EDGE * 2 + 12.dp
                )
                .width(MENU_WIDTH)
                .graphicsLayer {
                    alpha = grow.value.coerceIn(0f, 1f)
                    val scale = 0.8f + 0.2f * grow.value
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(vertical = MENU_PADDING)) {
                Row(
                    Modifier.fillMaxWidth().height(MENU_HEADER_HEIGHT).padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balances the info icon, so the name stays centred as it is in that menu.
                    Spacer(Modifier.width(20.dp))
                    Text(
                        appName,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                entries.forEach { slot ->
                    Row(
                        // The Configure entry is a real way in, exactly as it is in the launcher's
                        // menu: tapping it goes to Settings.
                        Modifier
                            .fillMaxWidth()
                            .height(MENU_ROW_HEIGHT)
                            .then(if (slot == null) Modifier.clickable { onOpenSettings() } else Modifier)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DemoIcon(slot, if (slot == null) 0 else slot.id + 1, MENU_ICON)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            if (slot == null) {
                                stringResource(R.string.shortcut_configure_label)
                            } else {
                                labelOf(slot, slot.id + 1)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * The icon as it is on the home screen — the user's own, in the variant Settings has enabled — at the
 * size it is there, parked in the panel's corner. It behaves the way that icon does: a tap does what
 * the mode does with it (show the list in List, open the first item in Direct and Mix), and holding it
 * opens the shortcut menu the launcher would show.
 *
 * The line under it names what a tap gives you before you tap, and says it back once you have.
 */
@Composable
private fun DemoIconBlock(
    mode: AppMode,
    target: ShortcutSlot,
    onShowShortcuts: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val burst = remember { Animatable(1f) }
    val pop = 1f + 0.12f * (1f - burst.value).coerceIn(0f, 1f)
    val opened = burst.value < 1f

    // A fixed width on purpose: as a wrap-content column the icon's own place moved every time the
    // caption under it changed length ("Opens right away" vs "Opened"), which read as the icon
    // twitching around.
    Column(
        modifier.width(ICON_BLOCK_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(HOME_ICON), contentAlignment = Alignment.Center) {
            if (burst.value < 1f) {
                // requiredSize, not matchParentSize: a canvas in the layout flow used to make this
                // block taller than the room it has, and the caption was what got squeezed out.
                Canvas(Modifier.requiredSize(HOME_ICON * 2.4f)) { firework(burst.value) }
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
                                onTap()
                                scope.launch {
                                    burst.snapTo(0f)
                                    burst.animateTo(1f, tween(FIREWORK_MS, easing = LinearEasing))
                                }
                            },
                            onLongPress = { onShowShortcuts() }
                        )
                    }
            ) {
                LauncherIcon(HOME_ICON)
            }
        }
        Column(
            Modifier
                .padding(top = 8.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mode == AppMode.LIST) {
                Text(
                    stringResource(R.string.mode_demo_list_opens),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            } else if (opened) {
                Text("\u2713", style = MaterialTheme.typography.titleSmall, color = OPENED_CHECK)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.mode_demo_opened, labelOf(target, target.id + 1)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            } else {
                DemoIcon(target, target.id + 1, 18.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.mode_demo_direct_opens, labelOf(target, target.id + 1)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2
                )
            }
        }
            // What holding the icon does, right under the icon it belongs to.
            Text(
                stringResource(R.string.mode_demo_long_press),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * The Configure gear Direct flashes over the app it just launched: a 32dp dark scrim with the same
 * 24dp glyph, 12dp in from the top end, gone again after the same 2.5s. Tapping it goes to Settings,
 * as the real one does.
 */
@Composable
private fun DemoGearChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(GEAR_MARGIN)
            .clickable { onClick() }
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
@Composable
fun Modifier.brandWallpaperBackground(): Modifier {
    val mark = painterResource(R.drawable.ic_not_app_mark)
    return drawBehind { brandWallpaper(mark) }
}

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
/** The size a home-screen icon actually is. */
private val HOME_ICON = 56.dp

/** Fixed, so the icon inside it never shifts when its caption changes. */
private val ICON_BLOCK_WIDTH = 150.dp
/** The sheet's own dismissal numbers (QuickPickSheet): same threshold, same fling speed. */
private val SHEET_DISMISS_THRESHOLD = 100.dp
private val SHEET_DISMISS_VELOCITY = 1000.dp
private val SHEET_CORNER = 22.dp

/** Room Mix keeps for the icon block above its list, so the list can still run to the bottom. */
private val MIX_ICON_SPACE = 190.dp
private val MIX_SHEET_MIN_HEIGHT = 120.dp

/** The most the sheet grows to before its list scrolls instead. */
private val SHEET_MAX_HEIGHT = 460.dp

/** The real sheet keeps its list inside 60% of the screen; the example follows the same rule. */
private const val SHEET_PANEL_FRACTION = 0.6f

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

private const val SHEET_BUMP_MS = 130

/** The same settle the real sheet uses: a spring, so the drag's velocity counts. */
private val SHEET_SPRING = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
private const val PLACEHOLDER_ROWS = 5
private const val SHORTCUT_MENU_ROWS = 4
private const val SCRIM_ALPHA = 0.32f

/** The real menu is a phone-sized card, not a full-width sheet — capped so a tablet gets that too. */
// The menu is a phone-sized card at a fixed width, not stretched to its container. It hangs under the icon
// (the icon block sits 62dp from the panel's top and is HOME_ICON tall).
private val MENU_WIDTH = 224.dp
private val MENU_ICON_BOTTOM = 62.dp + HOME_ICON + 8.dp
private val MENU_EDGE = 8.dp
private val MENU_HEADER_HEIGHT = 40.dp
private val MENU_ROW_HEIGHT = 48.dp
private val MENU_PADDING = 6.dp
private val MENU_ICON = 32.dp
