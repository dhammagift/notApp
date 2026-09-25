package com.noapp.container.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * [to], arriving by turning out of [from]: [from] spins away and fades while [to] spins in from
 * the other half-turn. Played once, when this first appears, and only if [from] is given.
 * Used where one screen's top-bar action sits exactly where the next screen's did (Configure's
 * gear and Settings' share), so the change of screen reads as the same button changing its job.
 */
@Composable
fun MorphIcon(to: ImageVector, contentDescription: String?, from: ImageVector? = null) {
    val progress = remember { Animatable(if (from == null) 1f else 0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(MORPH_MS, easing = FastOutSlowInEasing)) }
    val p = progress.value
    Box(contentAlignment = Alignment.Center) {
        if (from != null && p < 1f) {
            Icon(
                from,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    rotationZ = p * 180f
                    alpha = 1f - p
                    scaleX = 1f - p * 0.4f
                    scaleY = 1f - p * 0.4f
                }
            )
        }
        Icon(
            to,
            contentDescription = contentDescription,
            modifier = Modifier.graphicsLayer {
                rotationZ = (p - 1f) * 180f
                alpha = p
                scaleX = 0.6f + p * 0.4f
                scaleY = 0.6f + p * 0.4f
            }
        )
    }
}

/** A cog that turns into place as it appears, like a gear catching. */
@Composable
fun Modifier.spinInOnAppear(fromDegrees: Float = -150f, delayMillis: Long = 0, start: Boolean = true): Modifier {
    val turn = remember { Animatable(fromDegrees) }
    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect
        delay(delayMillis)
        turn.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessVeryLow))
    }
    return graphicsLayer { rotationZ = turn.value }
}

/** Grows out of a smaller, see-through self as it appears, with a little overshoot. */
@Composable
fun Modifier.popInOnAppear(): Modifier {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    return graphicsLayer {
        val g = grow.value
        alpha = g.coerceIn(0f, 1f)
        scaleX = 0.6f + 0.4f * g
        scaleY = 0.6f + 0.4f * g
    }
}

/**
 * Fades in and rises a few dp into place, [order] steps after the first one, so a list opens as a
 * quick cascade rather than all at once. The delay stops growing after a handful of rows: further
 * down, a row scrolled into view must not be kept waiting.
 */
@Composable
fun Modifier.riseInOnAppear(order: Int, baseDelayMillis: Int = 0, start: Boolean = true): Modifier {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect
        shown.animateTo(1f, tween(RISE_MS, delayMillis = baseDelayMillis + order.coerceAtMost(RISE_MAX_STEPS) * RISE_STEP_MS, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = shown.value
        translationY = (1f - shown.value) * RISE_DP * density
    }
}

private const val RISE_MS = 340
private const val RISE_STEP_MS = 55
private const val RISE_MAX_STEPS = 6
private const val RISE_DP = 20f
/** Shrinks a little under the finger and springs back on release. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.92f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Pops once whenever [key] changes (not on first composition): a marker being switched on or off. */
@Composable
fun Modifier.popOnChange(key: Any?): Modifier {
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) { first = false; return@LaunchedEffect }
        pop.snapTo(1.5f)
        pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
    }
    return graphicsLayer { scaleX = pop.value; scaleY = pop.value }
}

private const val MORPH_MS = 420
