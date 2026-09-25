package com.noapp.container.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.noapp.container.R
import com.noapp.container.icon.AndroidIcon
import com.noapp.container.icon.BoltIcon
import com.noapp.container.icon.LinkIcon
import com.noapp.container.icon.SlotIcon
import com.noapp.container.icon.displayName
import com.noapp.container.icon.enabledLauncherComponent
import com.noapp.container.model.AppConfig
import com.noapp.container.model.AppMode
import com.noapp.container.model.ShortcutSlot
import com.noapp.container.model.SlotType

private const val MAX_FILL_SELECTION = 20

/** Never taller than this, however long the descriptions get: the example below needs room too. */
private val CARDS_MAX_HEIGHT = 340.dp


private fun SlotType.icon(): ImageVector = when (this) {
    SlotType.APP -> AndroidIcon
    SlotType.URL -> LinkIcon
    SlotType.INTENT -> BoltIcon
}

private fun AppMode.labelRes(): Int = when (this) {
    AppMode.LIST -> R.string.config_mode_list
    AppMode.DIRECT -> R.string.config_mode_direct
    AppMode.MIX -> R.string.config_mode_mix
}

/**
 * The picker's own title, distinct from [labelRes] (which is what the button in the toolbar shows):
 * every mode says on its title line what it does, in a few words, so the paragraph underneath only
 * adds detail and does not have to be read first.
 */
private fun AppMode.choiceTitleRes(): Int = when (this) {
    AppMode.LIST -> R.string.config_mode_list_choice
    AppMode.DIRECT -> R.string.config_mode_direct_choice
    AppMode.MIX -> R.string.config_mode_mix_choice
}

private fun AppMode.descriptionRes(): Int = when (this) {
    AppMode.LIST -> R.string.config_mode_list_desc
    AppMode.DIRECT -> R.string.config_mode_direct_desc
    AppMode.MIX -> R.string.config_mode_mix_desc
}

/**
 * Full-screen instead of a narrow anchored dropdown: each mode's description is a
 * couple of sentences (it has to explain what happens to the OS long-press
 * shortcuts too), which read as an unreadably narrow column in a popup menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModePickerDialog(
    currentMode: AppMode,
    showPeekBubble: Boolean,
    showRecentApps: Boolean,
    useAllSlotsInDirectMode: Boolean,
    peekBubbleSize: Float,
    peekBubbleAlpha: Float,
    peekBubbleDockPeek: Float,
    peekBubbleReturns: Boolean,
    onOpenSetting: (SettingsSpot?) -> Unit,
    slots: List<ShortcutSlot>,
    onModeSelected: (AppMode) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pendingMode by remember { mutableStateOf<AppMode?>(null) }
    var showGearExplainer by remember { mutableStateOf(false) }
    var shortcutsShown by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val wideLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Granted or not, the mode switch itself already went through below — this permission
        // is advisory (both the peek bubble and the Direct-mode gear already degrade gracefully
        // without it).
    }

    fun selectMode(candidate: AppMode) {
        if (candidate != currentMode &&
            (candidate == AppMode.LIST || candidate == AppMode.MIX) &&
            showPeekBubble &&
            !AndroidSettings.canDrawOverlays(context)
        ) {
            pendingMode = candidate
        }
        // Direct's gear (GearOverlayService) is always wanted now, not just when a Settings
        // toggle is on — so ask for its permission right here too, the same way List/Mix does
        // above for the peek bubble, instead of leaving it silently missing.
        if (candidate != currentMode && candidate == AppMode.DIRECT && !AndroidSettings.canDrawOverlays(context)) {
            showGearExplainer = true
        }
        onModeSelected(candidate)
    }

    if (pendingMode != null) {
        PeekOverlayPermissionDialog(
            onContinue = {
                pendingMode = null
                overlaySettingsLauncher.launch(
                    Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            },
            onDismiss = { pendingMode = null }
        )
    }

    if (showGearExplainer) {
        GearOverlayPermissionDialog(
            onContinue = {
                showGearExplainer = false
                overlaySettingsLauncher.launch(
                    Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            },
            onDismiss = { showGearExplainer = false }
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.config_mode_dialog_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                            }
                        }
                    )
                    Column(
                        Modifier.heightIn(max = CARDS_MAX_HEIGHT)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppMode.entries.forEach { candidate ->
                            val selected = candidate == currentMode
                            Surface(
                                onClick = { selectMode(candidate) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(candidate.choiceTitleRes()), style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.padding(top = 4.dp))
                                        // Direct's description carries two facts, and the second one —
                                        // that Settings moved into the long-press menu — is the one nobody
                                        // reads past. Bold and in the theme's alert colour, so choosing the
                                        // mode is itself where that is learned.
                                        val description = if (candidate == AppMode.DIRECT) {
                                            buildAnnotatedString {
                                                append(stringResource(R.string.config_mode_direct_desc))
                                                append(" ")
                                                // Only the word "Settings" is coloured; the phrase it belongs
                                                // to is bold, and the parenthetical tail is ordinary text —
                                                // the emphasis marks the fact, not the whole paragraph.
                                                withStyle(
                                                    SpanStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                ) {
                                                    append(stringResource(R.string.config_mode_direct_desc_settings))
                                                }
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(" ")
                                                    append(
                                                        stringResource(
                                                            R.string.config_mode_direct_desc_settings_suffix
                                                        )
                                                    )
                                                }
                                                append(" ")
                                                append(stringResource(R.string.config_mode_direct_desc_tail))
                                            }
                                        } else {
                                            AnnotatedString(stringResource(candidate.descriptionRes()))
                                        }
                                        Text(description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Outside the scrolling part and weighted, so it takes all the height the cards
                    // leave: the picture of the mode that is on right now, never covering them.
                    ModeDemo(
                        mode = currentMode,
                        slots = slots,
                        showRecentApps = showRecentApps,
                        showPeekBubble = showPeekBubble,
                        useAllSlotsInDirectMode = useAllSlotsInDirectMode,
                        peekBubbleSize = peekBubbleSize,
                        peekBubbleAlpha = peekBubbleAlpha,
                        peekBubbleDockPeek = peekBubbleDockPeek,
                        peekBubbleReturns = peekBubbleReturns,
                        // Inside the dialog: closing it is onDismiss(), and the spot is forwarded to
                        // the Config screen that opened the dialog.
                        onOpenSetting = { spot ->
                            onDismiss()
                            onOpenSetting(spot)
                        },
                        narrowSheet = wideLandscape,
                        onShowShortcuts = { shortcutsShown = true },
                        modifier = Modifier
                            .weight(1f)
                            // The panel is never narrowed: it stands for the screen, and a screen is
                            // the whole width in either orientation. Only the sheet inside it gets a
                            // phone's width when the screen is a landscape one.
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                if (shortcutsShown) {
                    ShortcutMenuOverlay(
                        appName = stringResource(R.string.app_name),
                        mode = currentMode,
                        slots = slots,
                        useAllSlotsInDirectMode = useAllSlotsInDirectMode,
                        onDismiss = { shortcutsShown = false }
                    )
                }
            }
        }
    }
}

/** The enabled launcher icon, so the row reads as "this is about the icon on your home screen". */
@Composable
private fun LauncherIconSmall() {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { 32.dp.roundToPx() }
    val bitmap = remember(sizePx) {
        runCatching {
            context.packageManager.getActivityIcon(enabledLauncherComponent(context)).toBitmap(sizePx, sizePx)
        }.getOrNull()?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    mode: AppMode,
    slots: List<ShortcutSlot>,
    showPeekBubble: Boolean,
    showRecentApps: Boolean,
    useAllSlotsInDirectMode: Boolean,
    peekBubbleSize: Float,
    peekBubbleAlpha: Float,
    peekBubbleDockPeek: Float,
    peekBubbleReturns: Boolean,
    hint: UiHint?,
    onHintShown: (UiHint) -> Unit,
    onEditSlot: (Int) -> Unit,
    onAddSlot: (SlotType) -> Unit,
    onOpenSettings: (SettingsSpot?) -> Unit,
    onModeChanged: (AppMode) -> Unit,
    onSlotsChanged: (List<ShortcutSlot>) -> Unit,
    // AppConfig.tileSlot: which row's rocket marker is lit, and the only way to move it.
    tileSlot: String,
    onTileSlotChanged: (String) -> Unit
) {
    var showFillDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    // Hoisted out of the toolbar: the list's own footer opens the same picker, and that is the only
    // way to see the demo without going through a mode you do not want to change.
    var modeDialogVisible by remember { mutableStateOf(false) }
    val dragState = rememberSlotDragState(slots)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.common_undo)

    // Reuses this screen's own Scaffold-hosted SnackbarHost (already correctly positioned above
    // the FAB and system bars, same as the Undo snackbar below) rather than a separate host.
    // Consumed in `finally`, i.e. once the snackbar is done OR this screen leaves composition
    // mid-show — never before: clearing the hint changes this effect's key, which cancels the
    // very coroutine showing it. See UiHint.
    LaunchedEffect(hint?.id) {
        val pending = hint ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(pending.text)
        } finally {
            onHintShown(pending)
        }
    }

    fun removeWithUndo(previous: List<ShortcutSlot>, updated: List<ShortcutSlot>, message: String) {
        // previous is a live SnapshotStateList reference (see slots: List<ShortcutSlot> above) —
        // snapshot it to a real immutable copy before mutating, or Undo would just reapply
        // the already-mutated list onto itself.
        val previousSnapshot = previous.toList()
        onSlotsChanged(updated)
        scope.launch {
            val result = snackbarHostState.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) {
                onSlotsChanged(previousSnapshot)
            }
        }
    }

    Scaffold(
        // Material3's default Snackbar deliberately inverts the theme (light-on-dark even in a
        // dark theme) for contrast — jarring against this app's otherwise all-dark surfaces, so
        // this keeps it in the same palette as everything else instead.
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    AssistChip(
                        onClick = { modeDialogVisible = true },
                        label = { Text(stringResource(mode.labelRes())) }
                    )
                    if (modeDialogVisible) {
                        ModePickerDialog(
                            currentMode = mode,
                            showPeekBubble = showPeekBubble,
                            showRecentApps = showRecentApps,
                            useAllSlotsInDirectMode = useAllSlotsInDirectMode,
                            peekBubbleSize = peekBubbleSize,
                            peekBubbleAlpha = peekBubbleAlpha,
                            peekBubbleDockPeek = peekBubbleDockPeek,
                            peekBubbleReturns = peekBubbleReturns,
                            onOpenSetting = { spot ->
                                // Close the picker first: the user is going somewhere else, and the
                                // dialog would otherwise sit on top of the screen they asked for.
                                modeDialogVisible = false
                                onOpenSettings(spot)
                            },
                            slots = slots,
                            // The dialog deliberately stays open on a choice: the example at the
                            // bottom is the whole point of it, and it can only show a mode once that
                            // mode IS the current one. Closing is the ✕ or Back, nothing else.
                            onModeSelected = onModeChanged,
                            onDismiss = { modeDialogVisible = false }
                        )
                    }
                    TextButton(onClick = { showFillDialog = true }) { Text(stringResource(R.string.config_fill)) }
                    IconButton(onClick = { onOpenSettings(null) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.config_settings_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            val fabRotation by animateFloatAsState(
                if (fabExpanded) 225f else 0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "fabRotation"
            )
            val fabContainerColor by animateColorAsState(
                if (fabExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                label = "fabContainer"
            )
            val fabContentColor by animateColorAsState(
                if (fabExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                label = "fabContent"
            )
            Column(horizontalAlignment = Alignment.End) {
                // The whole menu grows out of the FAB's corner as one shape, instead of
                // each row popping in independently.
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn(tween(220)) +
                        expandVertically(
                            expandFrom = Alignment.Bottom,
                            animationSpec = tween(320, easing = FastOutSlowInEasing)
                        ) +
                        scaleIn(
                            transformOrigin = TransformOrigin(1f, 1f),
                            initialScale = 0.4f,
                            animationSpec = tween(320, easing = FastOutSlowInEasing)
                        ),
                    exit = fadeOut(tween(150)) +
                        shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(200)) +
                        scaleOut(transformOrigin = TransformOrigin(1f, 1f), targetScale = 0.4f, animationSpec = tween(200))
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        // Closest-to-thumb (bottom, right above the FAB) is the most-used type first.
                        SlotType.entries.reversed().forEach { t ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Text(
                                    t.displayName(),
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        shadow = Shadow(MaterialTheme.colorScheme.surface, blurRadius = 8f)
                                    ),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                SmallFloatingActionButton(
                                    onClick = { fabExpanded = false; onAddSlot(t) },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(t.icon(), contentDescription = t.displayName())
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = fabContainerColor,
                    contentColor = fabContentColor
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.config_add_item_desc),
                        modifier = Modifier.graphicsLayer { rotationZ = fabRotation }
                    )
                }
            }
        }
    ) { padding ->
        val notConfiguredLabel = stringResource(R.string.config_not_configured)
        val deleteDesc = stringResource(R.string.config_delete_desc)
        val reorderDesc = stringResource(R.string.config_reorder_desc)
        val mainPositionLabel = stringResource(R.string.config_position_main)
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                // Room for the two round things at the bottom: the demo button on the left, the add
                // button on the right.
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                itemsIndexed(dragState.items, key = { _, d -> d.stableKey }) { index, draggable ->
                    val slot = draggable.slot
                    val isDragging = dragState.draggedIndex == index
                    val positionLabel = if (mode != AppMode.LIST && index == 0) mainPositionLabel else stringResource(R.string.common_item_n, index + 1)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                removeWithUndo(
                                    previous = slots,
                                    updated = slots.filterIndexed { i, _ -> i != index }.mapIndexed { i, s -> s.copy(id = i) },
                                    message = context.getString(R.string.config_removed_named, slot.label.ifBlank { positionLabel })
                                )
                            }
                            true
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = deleteDesc, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text(slot.label.ifBlank { positionLabel }) },
                            supportingContent = {
                                Text(
                                    "${slot.type?.displayName() ?: notConfiguredLabel} · $positionLabel",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = { SlotIcon(slot, size = 40.dp) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Two different facts about a row, so two different markers.
                                    // The star is "a plain tap opens this item": in DIRECT and MIX that is
                                    // slot 0, so tapping the star promotes the row to the top rather than
                                    // storing a second, competing notion of "main".
                                    // The rocket is "the Quick Settings tile launches this item" — it sits
                                    // on any row and never touches the order.
                                    // Same 24dp as the reorder handle beside them, so the row reads as one
                                    // line of controls; both markers are vectors from the icon set the app
                                    // already uses (the rocket is the tile's own drawable), never an emoji.
                                    val isTileTarget = slot.targetKey != null && slot.targetKey == tileSlot
                                    // Every inactive control in this row — star, rocket, ✕ and the reorder
                                    // handle — is the same grey at the same strength, so the row reads as one
                                    // set; only an ACTIVE star or rocket stands out, in hue and in brightness
                                    // (the filled star drawable against the outlined one does the rest).
                                    // 0.38 is Material's own disabled-content alpha: these are affordances
                                    // that must not read as buttons, especially next to a genuinely active
                                    // star or rocket (owner: "сейчас они выглядят как просто какие-то активные
                                    // кнопки… нужно ещё более прозрачные").
                                    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    Icon(
                                        painterResource(
                                            if (index == 0) R.drawable.ic_marker_star
                                            else R.drawable.ic_marker_star_outline
                                        ),
                                        contentDescription = stringResource(R.string.config_main_marker),
                                        tint = if (index == 0) MaterialTheme.colorScheme.primary else inactiveTint,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable {
                                            if (index != 0) {
                                                val promoted = slots.toMutableList()
                                                    .also { it.add(0, it.removeAt(index)) }
                                                onSlotsChanged(promoted.mapIndexed { i, s -> s.copy(id = i) })
    }
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.config_main_marker_hint)
                                            )
                                        }
                                    }
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(
                                    painterResource(R.drawable.ic_tile),
                                    contentDescription = stringResource(R.string.config_tile_marker),
                                    tint = if (isTileTarget) MaterialTheme.colorScheme.primary else inactiveTint,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(enabled = slot.isConfigured) {
                                            val key = slot.targetKey ?: return@clickable
                                            val wasAssigned = tileSlot == key
                                            onTileSlotChanged(if (wasAssigned) AppConfig.TILE_NONE else key)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(
                                                        if (wasAssigned) R.string.config_tile_marker_off
                                                        else R.string.config_tile_marker_on
                                                    )
                                                )
                                            }
                                        }
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.common_close),
                                    tint = inactiveTint,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                        if (slot.isConfigured) {
                                            removeWithUndo(
                                                previous = slots,
                                                updated = slots.toMutableList().also { it[index] = ShortcutSlot(id = index) },
                                                message = context.getString(R.string.config_cleared_named, slot.label.ifBlank { positionLabel })
                                            )
                                        } else {
                                            removeWithUndo(
                                                previous = slots,
                                                updated = slots.filterIndexed { i, _ -> i != index }.mapIndexed { i, s -> s.copy(id = i) },
                                                message = context.getString(R.string.config_removed_plain, positionLabel)
                                            )
                                        }
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = reorderDesc,
                                    tint = inactiveTint
                                )
                            }
                        },
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragState.dragOffsetY
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    shadowElevation = 12f
                                }
                            }
                            .onSizeChanged { dragState.onRowSized(it.height) }
                            .pointerInput(draggable.stableKey) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragState.onDragStart(index) },
                                    onDragEnd = { dragState.onDragEnd(onSlotsChanged) },
                                    onDragCancel = dragState::onDragCancel,
                                    onDrag = { change, drag -> change.consume(); dragState.onDrag(drag.y) }
                                )
                            }
                            .clickable(enabled = dragState.draggedIndex < 0) { onEditSlot(slot.id) }
                    )
                }
                HorizontalDivider()
            }
            // Pinned to the bottom-left rather than trailing the items: as a list row it moved every
            // time an item was added or removed, and it is not an item.
            FilledTonalButton(
                onClick = { modeDialogVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                LauncherIconSmall()
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.mode_demo_label))
            }
        }
        }
    }

    if (showFillDialog) {
        AppPickerDialog(
            multiSelect = true,
            maxSelection = MAX_FILL_SELECTION,
            onDismiss = { showFillDialog = false },
            onConfirm = { picks ->
                // Fills existing empty slots in order first, then appends any leftover picks.
                // Never overwrites an already-configured slot.
                val updated = slots.toMutableList()
                var pickIndex = 0
                for (i in updated.indices) {
                    if (pickIndex >= picks.size) break
                    if (!updated[i].isConfigured) {
                        val (pkg, appLabel) = picks[pickIndex++]
                        updated[i] = updated[i].copy(type = SlotType.APP, label = appLabel, param = pkg)
                    }
                }
                val remaining = picks.drop(pickIndex).mapIndexed { i, (pkg, appLabel) ->
                    ShortcutSlot(id = updated.size + i, type = SlotType.APP, label = appLabel, param = pkg)
                }
                onSlotsChanged(updated + remaining)
                showFillDialog = false
            }
        )
    }
}
