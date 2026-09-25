package com.noapp.container.ui

import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noapp.container.R
import com.noapp.container.icon.AppIcon
import com.noapp.container.icon.SlotIcon
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.recents.RecentApp
import com.noapp.container.recents.RecentApps
import com.noapp.container.shortcuts.ActionDispatcher
import com.noapp.container.shortcuts.QuickPickPeekOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Settling uses a spring rather than a fixed-duration tween so the drag's velocity can carry into the
 * animation — a tween ignores it, which is what made a flick feel like it was put down by someone else.
 */
private val SHEET_SPRING = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

/** A sheet is a phone-shaped thing: on a landscape screen it keeps that width, centred. */
private val SHEET_MAX_WIDTH = 420.dp
private const val DISMISS_DRAG_THRESHOLD_DP = 100
private const val DISMISS_FLING_VELOCITY_DP_PER_S = 1000

/**
 * A minimal bottom sheet of our own instead of Material3's ModalBottomSheet — that one hosts
 * its content in a Dialog with a scrim baked into the dialog window itself, which no
 * scrimColor value fully suppresses (it kept flashing even at Color.Transparent). This one
 * lives directly in the Activity's own content, so there is nothing to dim: an invisible
 * full-screen catcher handles tap-outside-to-dismiss, and a plain Animatable offset drives
 * the slide-up entrance, the drag-to-dismiss gesture, and the slide-down exit — all through
 * the same value, so there's no seam between "being dragged" and "animating closed".
 *
 * "Configure" sits in a small header row up top (least reachable spot) so the item list,
 * which ends at the very bottom of the sheet, keeps the most reachable position for real
 * items.
 *
 * [allowPeek] (LIST and MIX, not the share sheet): swiping the sheet away
 * collapses it into a small draggable button instead of finishing the host
 * Activity — tapping that button re-shows the list (with no side effect, unlike
 * a plain re-tap in MIX mode, which would also re-dispatch slot 0).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPickSheet(
    slots: List<ShortcutSlot>,
    sharedText: String?,
    allowPeek: Boolean = false,
    showRecentApps: Boolean = false,
    onConfigure: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Falls back to this in-Activity pill (below) only when the "draw over other
    // apps" permission isn't granted — see requestDismiss() below.
    var peeked by remember { mutableStateOf(false) }
    BackHandler(enabled = peeked) { onDismiss() }

    if (peeked) {
        PeekPill(onClick = { peeked = false })
        return
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Comfortably more than any real screen height — used as "fully off-screen below"
    // for both the entrance start point and the exit end point, so we never need this
    // composable's own measured height to animate it in or out.
    val offScreenPx = with(density) { 1200.dp.toPx() }
    val dismissThresholdPx = with(density) { DISMISS_DRAG_THRESHOLD_DP.dp.toPx() }
    val dismissVelocityPx = with(density) { DISMISS_FLING_VELOCITY_DP_PER_S.dp.toPx() }

    val offsetY = remember { Animatable(offScreenPx) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }

    // Rises from just under its own bottom edge, measured, not from 1200dp away: with the whole
    // distance eased over a fraction of a second, the visible part of the trip lasted a few
    // frames and the sheet looked like it had simply appeared. A spring with a little overshoot
    // makes it arrive, so it reads as something of ours sliding over the app underneath.
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    // Whether the rise has begun; the gear and the rows take their cue from it.
    var sheetRising by remember { mutableStateOf(false) }
    val view = LocalView.current
    LaunchedEffect(sheetHeightPx > 0) {
        if (sheetHeightPx == 0) return@LaunchedEffect
        // In Mix this activity opens on top of another app that is still launching, and its window is
        // not on screen until it has focus: a rise that starts before then plays unseen and the sheet
        // seems to simply be there. Wait for the window, then begin.
        var waited = 0L
        while (!view.hasWindowFocus() && waited < WINDOW_WAIT_MS) {
            delay(16)
            waited += 16
        }
        delay(50)
        offsetY.snapTo(sheetHeightPx.toFloat())
        sheetRising = true
        offsetY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
    }

    fun requestDismiss(velocity: Float = 0f) {
        if (dismissed) return
        dismissed = true
        scope.launch {
            // The release velocity is handed to the animation, so a flicked-away sheet leaves at the
            // speed it was flicked at instead of at whatever a fixed duration implies.
            offsetY.animateTo(offScreenPx, SHEET_SPRING, initialVelocity = velocity)
            when {
                !allowPeek -> onDismiss()
                // Preferred path: a real system overlay that keeps showing over
                // whatever the user switches to, not just this Activity's own window.
                Settings.canDrawOverlays(context) -> {
                    context.startService(Intent(context, QuickPickPeekOverlayService::class.java))
                    onDismiss()
                }
                // No overlay permission: the in-Activity pill is at least usable
                // while the user stays on top of whatever slot 0 launched.
                else -> peeked = true
            }
        }
    }
    BackHandler(onBack = ::requestDismiss)

    // Launching something from the list is also "leaving" it: the bubble comes back so the
    // list stays one tap away over whatever just opened, same as swiping the sheet away would.
    // (No exit animation — the launched app is already covering us.)
    fun leaveWithPeek() {
        if (allowPeek && Settings.canDrawOverlays(context)) {
            context.startService(Intent(context, QuickPickPeekOverlayService::class.java))
        }
        (context as? Activity)?.finish()
    }

    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp
    // Held sideways the screen is far wider than a sheet ever is, and a full-width list puts the
    // labels a hand's width away from their icons. The sheet keeps a phone's width there and stays
    // centred, which is also what the mode picker's example shows for these modes.
    val wideLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(Modifier.fillMaxSize()) {
        // No scrim drawn here on purpose — just an invisible full-screen tap target so
        // tapping outside the sheet still dismisses it.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = ::requestDismiss
                )
        )

        Surface(
            shape = BottomSheetDefaults.ExpandedShape,
            color = BottomSheetDefaults.ContainerColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = if (wideLandscape) SHEET_MAX_WIDTH else Dp.Unspecified)
                .fillMaxWidth()
                .onSizeChanged { if (sheetHeightPx == 0) sheetHeightPx = it.height }
                .offset { IntOffset(0, (offsetY.value + dragOffset).roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = { velocity ->
                        val settled = dragOffset
                        dragOffset = 0f
                        offsetY.snapTo(settled)
                        if (settled > dismissThresholdPx || velocity > dismissVelocityPx) {
                            requestDismiss(velocity)
                        } else {
                            offsetY.animateTo(0f, SHEET_SPRING, initialVelocity = velocity)
                        }
                    }
                )
        ) {
            // navigationBarsPadding goes on the inner content, not the Surface itself: applied
            // to the Surface it shrinks the Surface's own bounds up from the true screen edge,
            // leaving a gap below it (through the transparent tap-outside catcher) that the
            // background behind the sheet shows through. Padding the content instead keeps the
            // Surface's background flush with the bottom of the screen while still keeping the
            // actual controls clear of the nav bar / gesture area.
            // animateContentSize: recent apps arrive after the sheet is already up, and the sheet
            // should grow to take them rather than jump.
            Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp).animateContentSize()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    BottomSheetDefaults.DragHandle()
                }
                var recentApps by remember { mutableStateOf<List<RecentApp>>(emptyList()) }
                if (showRecentApps) {
                    LaunchedEffect(Unit) {
                        recentApps = withContext(Dispatchers.IO) { RecentApps.query(context) }
                    }
                }
                // Recent apps and Configure share one compact header row instead of a row each —
                // there's no real content to spread across two, and every row here costs sheet
                // height that pushes the actual (real) items further from the thumb.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = recentApps.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        enter = fadeIn(tween(RECENTS_FADE_MS))
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RecentAppsIcons(
                                apps = recentApps,
                                modifier = Modifier.weight(1f),
                                onLaunched = ::leaveWithPeek
                            )
                            VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }
                    }
                    if (recentApps.isEmpty()) Spacer(Modifier.weight(1f))
                    IconButton(onClick = onConfigure) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.quick_pick_configure_desc),
                            // Waits for the sheet to land, then turns a full circle into place.
                            modifier = Modifier.spinInOnAppear(fromDegrees = -360f, delayMillis = 320, start = sheetRising)
                        )
                    }
                }
                AnimatedVisibility(recentApps.isNotEmpty(), enter = fadeIn(tween(RECENTS_FADE_MS))) { HorizontalDivider() }
                if (sharedText != null) {
                    Text(
                        stringResource(R.string.quick_pick_send_to, sharedText),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                LazyColumn(Modifier.heightIn(max = maxListHeight)) {
                    itemsIndexed(slots, key = { _, it -> it.id }) { index, slot ->
                        ListItem(
                            headlineContent = { Text(slot.label.ifBlank { stringResource(R.string.common_item_n, slot.id + 1) }) },
                            leadingContent = { SlotIcon(slot, size = 32.dp) },
                            modifier = Modifier.riseInOnAppear(index, baseDelayMillis = 260, start = sheetRising).clickable {
                                ActionDispatcher.execute(context, slot, sharedText)
                                leaveWithPeek()
                            }
                        )
                    }
                }
                // Rating ask, and only when the list is the reason the sheet is open: in the
                // share sheet the user is mid-task and has no patience for it.
                if (sharedText == null) {
                    ReviewCardIfDue(Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                }
            }
        }
    }
}

/**
 * Compact, icon-only strip of recently-foregrounded apps (see recents/RecentApps.kt for why
 * this is usage history rather than a true running-tasks list) — deliberately not full
 * ListItem rows like the configured slots below, and deliberately sharing the Configure
 * header row (see its call site) rather than a row of its own, to stay minimal.
 */
@Composable
private fun RecentAppsIcons(apps: List<RecentApp>, modifier: Modifier = Modifier, onLaunched: () -> Unit) {
    val context = LocalContext.current
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        items(apps, key = { it.packageName }) { app ->
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(onClickLabel = app.label) {
                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        onLaunched()
                    }
                    .padding(4.dp)
            ) {
                AppIcon(packageName = app.packageName, size = 32.dp)
            }
        }
    }
}

/**
 * The in-Activity fallback for [allowPeek] when the overlay permission isn't
 * granted: a small round button instead of a full-width bar, so it doesn't
 * cover whatever's underneath it. Positioned bottom-end, same thumb-reach
 * corner the sheet's own controls favor. Unlike QuickPickPeekOverlayService's
 * real system overlay, this only lives as long as the Activity itself does.
 */
@Composable
private fun PeekPill(onClick: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .popInOnAppear()
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClickLabel = stringResource(R.string.quick_pick_reopen_desc), onClick = onClick),
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

private const val RECENTS_FADE_MS = 260

private const val WINDOW_WAIT_MS = 700L
